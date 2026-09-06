package com.ismailmushraf.bujo.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.ismailmushraf.bujo.MainActivity;
import com.ismailmushraf.bujo.R;
import com.ismailmushraf.bujo.adapters.EntryAdapter;
import com.ismailmushraf.bujo.db.DatabaseManager;
import com.ismailmushraf.bujo.models.Entry;
import com.ismailmushraf.bujo.utils.EntryUIHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProjectHistoryFragment extends Fragment {

    private static final String ARG_PROJECT_ID = "project_id";
    private static final String ARG_PROJECT_NAME = "project_name";

    private int projectId;
    private String projectName;
    private ListView listView;
    private DatabaseManager dbManager;
    private EntryUIHelper uiHelper;

    public static ProjectHistoryFragment newInstance(int projectId, String projectName) {
        ProjectHistoryFragment fragment = new ProjectHistoryFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PROJECT_ID, projectId);
        args.putString(ARG_PROJECT_NAME, projectName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_project_history, container, false);

        if (getArguments() != null) {
            projectId = getArguments().getInt(ARG_PROJECT_ID);
            projectName = getArguments().getString(ARG_PROJECT_NAME);
        }

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setToolbarTitle(projectName + " HISTORY");
            ((MainActivity) getActivity()).setToolbarSubtitle("");
        }

        listView = (ListView) root.findViewById(R.id.lv_project_history);

        dbManager = new DatabaseManager(getActivity());
        dbManager.open();

        uiHelper = new EntryUIHelper(getActivity(), dbManager, new EntryUIHelper.OnEntryUpdatedListener() {
            @Override
            public void onEntryUpdated() {
                loadEntries();
            }
        });

        loadEntries();

        return root;
    }

    private void loadEntries() {
        List<Entry> entries = dbManager.getCompletedEntriesForProject(projectId);
        List<Object> groupedItems = new ArrayList<>();
        
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US);
        String lastDate = "";
        
        for (Entry e : entries) {
            long timestamp = e.getCompletedAt() > 0 ? e.getCompletedAt() : e.getCreatedAt();
            String entryDate = sdf.format(new Date(timestamp));
            if (!entryDate.equals(lastDate)) {
                groupedItems.add(entryDate.toUpperCase(Locale.US));
                lastDate = entryDate;
            }
            groupedItems.add(e);
        }
        
        EntryAdapter adapter = new EntryAdapter(getActivity(), groupedItems, false);
        adapter.setUIHelper(uiHelper);
        adapter.setOnEntryInteractionListener(new EntryAdapter.OnEntryInteractionListener() {
            @Override
            public void onEntryTextClick(Entry entry) {
                if ("*".equals(entry.getSignifier())) {
                    uiHelper.showTaskDetailDialog(entry);
                }
            }

            @Override
            public void onEntryLongClick(Entry entry, View view) {
                uiHelper.showContextDialog(entry, view);
            }
        });
        listView.setAdapter(adapter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dbManager != null) dbManager.close();
    }
}
