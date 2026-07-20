package com.ismailmushraf.bujo.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.ismailmushraf.bujo.MainActivity;
import com.ismailmushraf.bujo.R;
import com.ismailmushraf.bujo.adapters.EntryAdapter;
import com.ismailmushraf.bujo.db.DatabaseManager;
import com.ismailmushraf.bujo.models.Entry;
import com.ismailmushraf.bujo.utils.EntryUIHelper;

import java.util.List;
import java.util.Locale;

public class MigratedItemsFragment extends Fragment {

    private ListView listView;
    private DatabaseManager dbManager;
    private EntryAdapter adapter;
    private List<Entry> entries;
    private EntryUIHelper uiHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_migrated_items, container, false);

        listView = (ListView) root.findViewById(R.id.lv_migrated_items);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setToolbarTitle("Logbook".toUpperCase(Locale.US));
            ((MainActivity) getActivity()).setToolbarSubtitle("");
        }

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

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Entry entry = entries.get(position);
                entry.setCompleted(!entry.isCompleted());
                dbManager.updateEntry(entry);
                loadEntries();
            }
        });

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

        return root;
    }

    private void loadEntries() {
        entries = dbManager.getMigratedEntries();
        adapter = new EntryAdapter(getActivity(), entries);
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