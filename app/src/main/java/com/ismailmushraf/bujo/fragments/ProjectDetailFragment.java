package com.ismailmushraf.bujo.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListAdapter;
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

public class ProjectDetailFragment extends Fragment {

    private static final String ARG_PROJECT_ID = "project_id";
    private static final String ARG_PROJECT_NAME = "project_name";

    private int projectId;
    private String projectName;

    private ListView lvUncompleted, lvCompleted;
    private DatabaseManager dbManager;
    private EntryUIHelper uiHelper;

    public static ProjectDetailFragment newInstance(int projectId, String projectName) {
        ProjectDetailFragment fragment = new ProjectDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PROJECT_ID, projectId);
        args.putString(ARG_PROJECT_NAME, projectName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_project_detail, container, false);

        if (getArguments() != null) {
            projectId = getArguments().getInt(ARG_PROJECT_ID);
            projectName = getArguments().getString(ARG_PROJECT_NAME);
        }

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setToolbarTitle(projectName);
            ((MainActivity) getActivity()).setToolbarSubtitle("");
        }

        lvUncompleted = root.findViewById(R.id.lv_uncompleted);
        lvCompleted = root.findViewById(R.id.lv_completed);
        final EditText etNewEntry = root.findViewById(R.id.et_new_entry);
        View deleteProject = root.findViewById(R.id.btn_delete_project);

        dbManager = new DatabaseManager(getActivity());
        dbManager.open();

        uiHelper = new EntryUIHelper(getActivity(), dbManager, new EntryUIHelper.OnEntryUpdatedListener() {
            @Override
            public void onEntryUpdated() { loadEntries(); }
        });

        loadEntries();

        deleteProject.setOnClickListener(v -> showDeleteProjectConfirmation());
        setupInputListener(etNewEntry);

        root.findViewById(R.id.btn_emoji).setOnClickListener(v -> uiHelper.showEmojiPicker(etNewEntry));

        return root;
    }

    private void setupInputListener(final EditText etNewEntry) {
        etNewEntry.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE || actionId == 0) {
                addEntry(etNewEntry.getText().toString());
                etNewEntry.setText("");
                return true;
            }
            return false;
        });

        etNewEntry.setOnKeyListener((v, keyCode, event) -> {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                    (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER || keyCode == KeyEvent.KEYCODE_PLUS)) {
                addEntry(etNewEntry.getText().toString());
                etNewEntry.setText("");
                return true;
            }
            return false;
        });
    }

    private void loadEntries() {
        List<Entry> allEntries = dbManager.getEntriesForProject(projectId);
        List<Entry> uncompleted = new ArrayList<>();
        List<Entry> completed = new ArrayList<>();

        for (Entry e : allEntries) {
            if (e.isCompleted()) completed.add(e);
            else uncompleted.add(e);
        }

        // Uncompleted List Setup
        EntryAdapter uncompletedAdapter = new EntryAdapter(getActivity(), uncompleted, false);
        lvUncompleted.setAdapter(uncompletedAdapter);
        lvUncompleted.setOnItemClickListener((parent, view, position, id) -> {
            Entry entry = uncompleted.get(position);
            entry.setCompleted(true);
            dbManager.updateEntry(entry);
            loadEntries();
        });
        lvUncompleted.setOnItemLongClickListener((parent, view, position, id) -> {
            uiHelper.showContextDialog(uncompleted.get(position));
            return true;
        });

        // Completed List Setup
        EntryAdapter completedAdapter = new EntryAdapter(getActivity(), completed, false);
        lvCompleted.setAdapter(completedAdapter);
        lvCompleted.setOnItemClickListener((parent, view, position, id) -> {
            Entry entry = completed.get(position);
            entry.setCompleted(false);
            dbManager.updateEntry(entry);
            loadEntries();
        });

        // Force height calculation
        setListViewHeightBasedOnChildren(lvUncompleted);
        setListViewHeightBasedOnChildren(lvCompleted);
    }

    private void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) return;

        int totalHeight = 0;
        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.AT_MOST);

        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(desiredWidth, View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }

    private void addEntry(String content) {
        if (content != null && !content.trim().isEmpty()) {
            Entry newEntry = com.ismailmushraf.bujo.utils.EntryParser.parse(content);
            if (newEntry.getProjectTag() == null) newEntry.setProjectTag(projectName);
            newEntry.setProjectId(projectId);
            newEntry.setCompleted(false);
            if (!newEntry.getContent().trim().isEmpty()) {
                dbManager.insertEntry(newEntry);
                loadEntries();
            }
        }
    }

    private void showDeleteProjectConfirmation() {
        new AlertDialog.Builder(getActivity())
                .setTitle("Delete Project")
                .setMessage("How would you like to handle the entries in this project?")
                .setPositiveButton("Keep Entries", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Original behavior: Unassign only
                        if (dbManager.deleteProjectAndUnassignEntries(projectId)) {
                            ((MainActivity) getActivity()).refreshDrawer();
                            ((MainActivity) getActivity()).showDailyLog();
                        }
                    }
                })
                .setNeutralButton("Delete Everything", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // New behavior: Delete project + all its entries
                        dbManager.deleteProjectAndAllEntries(projectId);
                        ((MainActivity) getActivity()).refreshDrawer();
                        ((MainActivity) getActivity()).showDailyLog();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override public void onDestroy() { super.onDestroy(); dbManager.close(); }
}