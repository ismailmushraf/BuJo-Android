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

        TextView tvDetailTitle = root.findViewById(R.id.tv_project_detail_title);
        if (tvDetailTitle != null) {
            tvDetailTitle.setText(projectName != null && !projectName.isEmpty() ? projectName : "Project");
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
                if (getFragmentManager() != null && entry != null) {
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
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

    public void openRightSidebar() {
        if (getActivity() == null) return;

        final android.app.Dialog dialog = new android.app.Dialog(getActivity(), android.R.style.Theme_Translucent_NoTitleBar);
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_bb10_project_sidebar, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            android.view.WindowManager.LayoutParams lp = new android.view.WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = android.view.WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = android.view.WindowManager.LayoutParams.MATCH_PARENT;
            lp.gravity = android.view.Gravity.END;
            lp.windowAnimations = R.style.BB10SidebarAnimation;
            dialog.getWindow().setAttributes(lp);
        }

        view.findViewById(R.id.sidebar_dim_scrim).setOnClickListener(v -> dialog.dismiss());

        TextView tvProjectName = view.findViewById(R.id.sidebar_project_name);
        TextView tvCreatedDate = view.findViewById(R.id.sidebar_project_created_date);
        TextView tvStats = view.findViewById(R.id.sidebar_project_stats);

        tvProjectName.setText(projectName);

        Project project = dbManager.getOrCreateProject(projectName);
        List<Entry> allEntries = dbManager.getEntriesForProject(projectId);
        int totalTasks = allEntries.size();
        int completedTasks = 0;
        for (Entry e : allEntries) {
            if (e.isCompleted()) completedTasks++;
        }

        if (project != null && project.getCreatedAt() > 0) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.US);
            tvCreatedDate.setText("Created: " + sdf.format(new java.util.Date(project.getCreatedAt())));
        } else {
            tvCreatedDate.setText("Project Goal");
        }

        if (totalTasks > 0) {
            int pct = (int) ((completedTasks * 100.0f) / totalTasks);
            tvStats.setText("Completed: " + completedTasks + " of " + totalTasks + " (" + pct + "%)");
        } else {
            tvStats.setText("No tasks created yet");
        }

        class SidebarOption {
            String title;
            int iconResId;
            SidebarOption(String title, int iconResId) {
                this.title = title;
                this.iconResId = iconResId;
            }
        }

        List<SidebarOption> optionsList = new ArrayList<>();
        optionsList.add(new SidebarOption("History", R.drawable.ic_bb10_history));
        optionsList.add(new SidebarOption("Edit", android.R.drawable.ic_menu_edit));

        ListView lvOptions = view.findViewById(R.id.lv_sidebar_options);
        android.widget.ArrayAdapter<SidebarOption> adapter = new android.widget.ArrayAdapter<SidebarOption>(getActivity(), R.layout.item_sidebar_option, optionsList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_sidebar_option, parent, false);
                }
                SidebarOption option = getItem(position);
                TextView tv = convertView.findViewById(R.id.tv_option_title);
                android.widget.ImageView iv = convertView.findViewById(R.id.iv_option_icon);
                if (option != null) {
                    tv.setText(option.title);
                    iv.setImageResource(option.iconResId);
                }
                return convertView;
            }
        };
        lvOptions.setAdapter(adapter);

        lvOptions.setOnItemClickListener((parent, v, position, id) -> {
            dialog.dismiss();
            SidebarOption option = optionsList.get(position);
            if ("History".equals(option.title)) {
                if (getFragmentManager() != null) {
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right);
                    ft.replace(R.id.fragment_container, ProjectHistoryFragment.newInstance(projectId, projectName));
                    ft.addToBackStack(null);
                    ft.commit();
                }
            } else if ("Edit".equals(option.title)) {
                if (getFragmentManager() != null) {
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right);
                    ft.replace(R.id.fragment_container, EditProjectFragment.newInstance(projectId, projectName));
                    ft.addToBackStack(null);
                    ft.commit();
                }
            }
        });

        view.findViewById(R.id.sidebar_bottom_delete).setOnClickListener(v -> {
            dialog.dismiss();
            showDeleteProjectConfirmation();
        });

        dialog.show();
    }

    private void showDeleteProjectConfirmation() {
        if (getActivity() == null) return;
        com.ismailmushraf.bujo.utils.BB10DialogHelper.showConfirmDialog(
                getActivity(),
                "Delete Project",
                "Are you sure you want to delete '" + projectName + "' and all its tasks?",
                "Delete",
                () -> {
                    int pointsDeducted = dbManager.deleteProjectAndAllEntries(projectId);
                    if (getActivity() instanceof MainActivity) {
                        MainActivity main = (MainActivity) getActivity();
                        if (pointsDeducted > 0) {
                            main.animatePointsChange(-pointsDeducted, main.getWindow().getDecorView());
                        }
                        main.refreshDrawer();
                        main.showDailyLog();
                    }
                }
        );
    }

    public int getProjectId() {
        return projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    @Override public void onDestroy() { super.onDestroy(); dbManager.close(); }
}
