package com.ismailmushraf.bujo.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.ismailmushraf.bujo.MainActivity;
import com.ismailmushraf.bujo.R;
import com.ismailmushraf.bujo.adapters.EntryAdapter;
import com.ismailmushraf.bujo.db.DatabaseManager;
import com.ismailmushraf.bujo.models.Entry;
import com.ismailmushraf.bujo.models.Project;
import com.ismailmushraf.bujo.utils.EntryUIHelper;

import java.util.ArrayList;
import java.util.List;

public class InboxFragment extends Fragment {

    private EntryAdapter adapter;
    private List<Object> entries;
    private List<Object> allEntriesList = new ArrayList<>();
    private ListView listView;
    private DatabaseManager dbManager;
    private EntryUIHelper uiHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_daily_log, container, false);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setToolbarTitle("INBOX");
        }

        listView = (ListView) root.findViewById(R.id.lv_daily_bullets);
        final EditText etNewEntry = (EditText) root.findViewById(R.id.et_new_entry);

        dbManager = new DatabaseManager(getActivity());
        dbManager.open();

        uiHelper = new EntryUIHelper(getActivity(), dbManager, new EntryUIHelper.OnEntryUpdatedListener() {
            @Override
            public void onEntryUpdated() {
                loadEntries();
                updateCompletionRatio();
            }
        });

        loadEntries();
        updateCompletionRatio();

        listView.setOnItemClickListener(null);
        listView.setOnItemLongClickListener(null);

        etNewEntry.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEntries(s.toString());
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        etNewEntry.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                // actionId == 0 is sometimes returned by physical keyboards
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE || actionId == 0) {
                    processNewEntry(etNewEntry);
                    return true;
                }
                return false;
            }
        });

        etNewEntry.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN &&
                        (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER || keyCode == KeyEvent.KEYCODE_PLUS)) {
                    processNewEntry(etNewEntry);
                    return true;
                }
                return false;
            }
        });

        // Emoji and Event buttons removed from XML

        return root;
    }

    private void processNewEntry(EditText etNewEntry) {
        String content = etNewEntry.getText().toString();
        if (!content.trim().isEmpty()) {
            com.ismailmushraf.bujo.models.Entry newEntry = com.ismailmushraf.bujo.utils.EntryParser.parse(content);

            int projectId = 0;
            if (newEntry.getProjectTag() != null) {
                Project p = dbManager.getOrCreateProject(newEntry.getProjectTag());
                projectId = p.getId();

                // Refresh main activity drawer to show new project instantly
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).refreshDrawer();
                }
            }

            newEntry.setProjectId(projectId);
            newEntry.setCompleted(false);
            newEntry.setMigrated(false);

            // A project tag on its own creates the project but not an empty note.
            if (!newEntry.getContent().trim().isEmpty()) {
                long insertedId = dbManager.insertEntry(newEntry);
                
                if (insertedId != -1 && getActivity() instanceof MainActivity) {
                    int commitment = dbManager.calculateCommitmentReward(newEntry);
                    if (commitment > 0) {
                        ((MainActivity) getActivity()).animatePointsChange(commitment, etNewEntry);
                    }
                }

                loadEntries();
                updateCompletionRatio();
            }
            etNewEntry.setText("");
        }
    }

    private void filterEntries(String query) {
        if (adapter == null) return;
        List<Object> filtered = com.ismailmushraf.bujo.utils.SearchHelper.filter(allEntriesList, query, item -> {
            if (item instanceof Entry) {
                Entry e = (Entry) item;
                return e.getContent() != null ? e.getContent() : "";
            } else if (item instanceof String) {
                return (String) item;
            }
            return "";
        });
        adapter.clear();
        adapter.addAll(filtered);
        adapter.notifyDataSetChanged();
    }

    private void loadEntries() {
        allEntriesList = new ArrayList<>(dbManager.getInboxEntries());
        entries = new ArrayList<>(allEntriesList);
        adapter = new EntryAdapter(getActivity(), entries, true);
        adapter.setUIHelper(uiHelper);
        adapter.setOnEntryInteractionListener(new EntryAdapter.OnEntryInteractionListener() {
            @Override
            public void onEntryTextClick(Entry entry) {
                if (getFragmentManager() != null && entry != null) {
                    android.support.v4.app.FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right);
                    ft.replace(R.id.fragment_container, EditTaskFragment.newInstance(entry.getId()));
                    ft.addToBackStack(null);
                    ft.commit();
                }
            }

            @Override
            public void onEntryLongClick(Entry entry, View view) {
                uiHelper.showContextDialog(entry, view);
            }
        });
        listView.setAdapter(adapter);
    }

    private void updateCompletionRatio() {
        String completion;
        if (entries == null || entries.isEmpty()) {
            completion = "0/0";
        } else {
            int completedCount = 0;
            int totalTasks = 0;
            for (Object item : entries) {
                if (item instanceof Entry) {
                    Entry entry = (Entry) item;
                    if ("*".equals(entry.getSignifier())) {
                        totalTasks++;
                        if (entry.isCompleted()) {
                            completedCount++;
                        }
                    }
                }
            }
            completion = totalTasks == 0 ? entries.size() + " items" : completedCount + "/" + totalTasks;
        }
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setToolbarSubtitle(completion);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dbManager != null) {
            dbManager.close();
        }
    }
}
