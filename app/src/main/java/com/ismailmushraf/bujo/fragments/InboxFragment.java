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
        root.findViewById(R.id.btn_plan_tomorrow).setVisibility(View.GONE);

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

        // Add this right after initializing etNewEntry
        TextView btnEmoji = (TextView) root.findViewById(R.id.btn_emoji);
        btnEmoji.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uiHelper.showEmojiPicker(etNewEntry);
            }
        });

        View btnAddEvent = root.findViewById(R.id.btn_add_event);
        btnAddEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uiHelper.showAddEventDialog(0, null);
            }
        });

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

    private void loadEntries() {
        entries = new ArrayList<>(dbManager.getInboxEntries());
        adapter = new EntryAdapter(getActivity(), entries, true);
        adapter.setUIHelper(uiHelper);
        adapter.setOnEntryInteractionListener(new EntryAdapter.OnEntryInteractionListener() {
            @Override
            public void onEntryTextClick(Entry entry) {
                // Future: show detail modal?
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
