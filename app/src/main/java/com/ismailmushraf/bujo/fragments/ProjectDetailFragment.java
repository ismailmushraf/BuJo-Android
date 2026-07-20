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
import java.util.Locale;

public class ProjectDetailFragment extends Fragment {

    private static final String ARG_PROJECT_ID = "project_id";
    private static final String ARG_PROJECT_NAME = "project_name";

    private int projectId;
    private String projectName;

    private ListView listView;
    private EntryAdapter adapter;
    private List<Entry> entries;
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            projectId = getArguments().getInt(ARG_PROJECT_ID);
            projectName = getArguments().getString(ARG_PROJECT_NAME);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_project_detail, container, false);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setToolbarTitle(projectName);
            ((MainActivity) getActivity()).setToolbarSubtitle("");
        }

        listView = (ListView) root.findViewById(R.id.lv_project_entries);
        final EditText etNewEntry = (EditText) root.findViewById(R.id.et_new_entry);
        View deleteProject = root.findViewById(R.id.btn_delete_project);

        dbManager = new DatabaseManager(getActivity());
        dbManager.open();

        uiHelper = new EntryUIHelper(getActivity(), dbManager, new EntryUIHelper.OnEntryUpdatedListener() {
            @Override
            public void onEntryUpdated() {
                loadEntries();
            }
        });

        loadEntries();

        deleteProject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteProjectConfirmation();
            }
        });

        etNewEntry.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE || actionId == 0) {
                    processNewEntry(etNewEntry);
                    return true;
                }
                return false;
            }
        });

        // Key Listener for physical keyboard (BlackBerry Passport)
        etNewEntry.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER || keyCode == KeyEvent.KEYCODE_PLUS)) {
                    processNewEntry(etNewEntry);
                    return true;
                }
                return false;
            }
        });

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

        // Add this logic right here:
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
        addEntry(etNewEntry.getText().toString());
        etNewEntry.setText("");
    }

    private void loadEntries() {
        entries = dbManager.getEntriesForProject(projectId);
        adapter = new EntryAdapter(getActivity(), entries, false);
        listView.setAdapter(adapter);
    }

    private void addEntry(String content) {
        if (content != null && !content.trim().isEmpty()) {
            com.ismailmushraf.bujo.models.Entry newEntry = com.ismailmushraf.bujo.utils.EntryParser.parse(content);

            int targetProjectId = projectId;
            if (newEntry.getProjectTag() != null) {
                Project p = dbManager.getOrCreateProject(newEntry.getProjectTag());
                targetProjectId = p.getId();

                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).refreshDrawer();
                }
            } else {
                // FIX: Automatically assign the current project's name as the tag!
                newEntry.setProjectTag(projectName);
            }

            newEntry.setProjectId(targetProjectId);
            newEntry.setCompleted(false);
            newEntry.setMigrated(false);

            // Let a tag-only line create a project without adding a blank entry.
            if (!newEntry.getContent().trim().isEmpty()) {
                dbManager.insertEntry(newEntry);
                loadEntries();
            }
        }
    }

    private void showDeleteProjectConfirmation() {
        new AlertDialog.Builder(getActivity())
                .setTitle("Delete project?")
                .setMessage("\"" + projectName + "\" will be removed. Its entries will be kept and returned to the Daily Log.")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (dbManager.deleteProjectAndUnassignEntries(projectId)
                                && getActivity() instanceof MainActivity) {
                            MainActivity activity = (MainActivity) getActivity();
                            activity.refreshDrawer();
                            activity.showDailyLog();
                        }
                    }
                })
                .show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dbManager.close();
    }
}