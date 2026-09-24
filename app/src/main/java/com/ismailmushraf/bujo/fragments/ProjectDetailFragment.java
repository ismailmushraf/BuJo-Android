package com.ismailmushraf.bujo.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
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

    private ListView lvUncompleted;
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
        setHasOptionsMenu(true);
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
        final EditText etNewEntry = root.findViewById(R.id.et_new_entry);

        dbManager = new DatabaseManager(getActivity());
        dbManager.open();

        uiHelper = new EntryUIHelper(getActivity(), dbManager, new EntryUIHelper.OnEntryUpdatedListener() {
            @Override
            public void onEntryUpdated() { loadEntries(); }
        });

        loadEntries();

        setupInputListener(etNewEntry);

        return root;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.project_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_project_history) {
            if (getFragmentManager() != null) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.fragment_container, ProjectHistoryFragment.newInstance(projectId, projectName));
                ft.addToBackStack(null);
                ft.commit();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupInputListener(final EditText etNewEntry) {
        etNewEntry.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE || actionId == 0) {
                addEntry(etNewEntry.getText().toString(), etNewEntry);
                etNewEntry.setText("");
                return true;
            }
            return false;
        });

        etNewEntry.setOnKeyListener((v, keyCode, event) -> {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                    (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER || keyCode == KeyEvent.KEYCODE_PLUS)) {
                addEntry(etNewEntry.getText().toString(), etNewEntry);
                etNewEntry.setText("");
                return true;
            }
            return false;
        });
    }

    private void loadEntries() {
        List<Entry> allEntries = dbManager.getEntriesForProject(projectId);
        List<Object> uncompleted = new ArrayList<>();

        for (Entry e : allEntries) {
            if (!e.isCompleted()) uncompleted.add(e);
        }

        // Uncompleted List Setup
        EntryAdapter uncompletedAdapter = new EntryAdapter(getActivity(), uncompleted, false);
        uncompletedAdapter.setUIHelper(uiHelper);
        uncompletedAdapter.setOnEntryInteractionListener(new EntryAdapter.OnEntryInteractionListener() {
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
        lvUncompleted.setAdapter(uncompletedAdapter);
        lvUncompleted.setOnItemClickListener(null);
        lvUncompleted.setOnItemLongClickListener(null);

        // Force height calculation
        setListViewHeightBasedOnChildren(lvUncompleted);
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

    private void addEntry(String content, View sourceView) {
        if (content != null && !content.trim().isEmpty()) {
            Entry newEntry = com.ismailmushraf.bujo.utils.EntryParser.parse(content);
            if (newEntry.getProjectTag() == null) newEntry.setProjectTag(projectName);
            newEntry.setProjectId(projectId);
            newEntry.setCompleted(false);
            if (!newEntry.getContent().trim().isEmpty()) {
                long insertedId = dbManager.insertEntry(newEntry);

                if (insertedId != -1 && getActivity() instanceof MainActivity) {
                    int commitment = dbManager.calculateCommitmentReward(newEntry);
                    if (commitment > 0) {
                        ((MainActivity) getActivity()).animatePointsChange(commitment, sourceView);
                    }
                }

                loadEntries();
            }
        }
    }

    private void showEditProjectDialog() {
        final Project project = dbManager.getOrCreateProject(projectName); // Get fresh object
        
        AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
        b.setTitle("Goal Settings");

        LinearLayout layout = new LinearLayout(getActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 30, 30, 30);

        final EditText etName = new EditText(getActivity());
        etName.setHint("Goal Name");
        etName.setText(project.getName());
        layout.addView(etName);

        final TextView label = new TextView(getActivity());
        label.setText("Priority Weight (1-5 stars)");
        label.setPadding(0, 20, 0, 0);
        layout.addView(label);

        final android.widget.RatingBar rb = new android.widget.RatingBar(getActivity(), null, android.R.attr.ratingBarStyle);
        rb.setNumStars(5);
        rb.setStepSize(1.0f);
        rb.setRating(project.getWeight());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rb.setLayoutParams(lp);
        layout.addView(rb);

        b.setView(layout);
        b.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newName = etName.getText().toString().trim();
                int newWeight = (int) rb.getRating();
                if (newWeight < 1) newWeight = 1;

                if (!newName.isEmpty()) {
                    project.setName(newName);
                    project.setWeight(newWeight);
                    dbManager.updateProject(project);
                    projectName = newName;
                    
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).setToolbarTitle(projectName);
                        ((MainActivity) getActivity()).refreshDrawer();
                    }
                    loadEntries();
                }
            }
        });
        b.setNegativeButton(android.R.string.cancel, null);
        b.show();
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
                        int appliedPoints = dbManager.deleteProjectAndAllEntries(projectId);
                        MainActivity main = (MainActivity) getActivity();
                        main.animatePointsChange(appliedPoints, main.getWindow().getDecorView());
                        main.refreshDrawer();
                        main.showDailyLog();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override public void onDestroy() { super.onDestroy(); dbManager.close(); }
}
