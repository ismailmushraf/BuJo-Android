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

import java.util.List;

public class InboxFragment extends Fragment {

    private EntryAdapter adapter;
    private List<Entry> entries;
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

        // Tap to complete/uncomplete
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position < entries.size()) {
                    Entry clickedEntry = entries.get(position);
                    clickedEntry.setCompleted(!clickedEntry.isCompleted());
                    dbManager.updateEntry(clickedEntry);
                    loadEntries();
                    updateCompletionRatio();
                }
            }
        });

        // Long press context menu for deadlines, migration and delete
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (position < entries.size()) {
                    uiHelper.showContextDialog(entries.get(position));
                    return true;
                }
                return false;
            }
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

        // Add this right after initializing etNewEntry
        TextView btnEmoji = (TextView) root.findViewById(R.id.btn_emoji);
        btnEmoji.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uiHelper.showEmojiPicker(etNewEntry);
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
                dbManager.insertEntry(newEntry);
                loadEntries();
                updateCompletionRatio();
            }
            etNewEntry.setText("");
        }
    }

    private void loadEntries() {
        entries = dbManager.getInboxEntries();
        adapter = new EntryAdapter(getActivity(), entries, true);
        listView.setAdapter(adapter);
    }

    private void updateCompletionRatio() {
        String completion;
        if (entries == null || entries.isEmpty()) {
            completion = "0/0";
        } else {
            int completedCount = 0;
            int totalTasks = 0;
            for (Entry entry : entries) {
                if ("*".equals(entry.getSignifier())) {
                    totalTasks++;
                    if (entry.isCompleted()) {
                        completedCount++;
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