package com.ismailmushraf.bujo.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;
import com.ismailmushraf.bujo.adapters.ProjectFolderAdapter;

import com.ismailmushraf.bujo.R;
import com.ismailmushraf.bujo.MainActivity;
import com.ismailmushraf.bujo.db.DatabaseManager;
import com.ismailmushraf.bujo.models.Project;

import java.util.List;

public class ProjectsFragment extends Fragment {

    private GridView gridView;
    private DatabaseManager dbManager;
    private List<Project> projectList;
    private List<Project> allProjectsList = new java.util.ArrayList<>();
    private ProjectFolderAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_projects, container, false);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setToolbarTitle("");
            ((MainActivity) getActivity()).setToolbarSubtitle("");
        }

        gridView = (GridView) root.findViewById(R.id.gv_projects);
        android.widget.EditText etSearch = (android.widget.EditText) root.findViewById(R.id.et_search_project);

        if (etSearch != null) {
            etSearch.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterProjects(s.toString());
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });

            etSearch.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE || actionId == 0) {
                    processQuickProjectCreation(etSearch);
                    return true;
                }
                return false;
            });

            etSearch.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == android.view.KeyEvent.ACTION_DOWN &&
                        (keyCode == android.view.KeyEvent.KEYCODE_ENTER || keyCode == android.view.KeyEvent.KEYCODE_NUMPAD_ENTER || keyCode == android.view.KeyEvent.KEYCODE_PLUS)) {
                    processQuickProjectCreation(etSearch);
                    return true;
                }
                return false;
            });
        }

        dbManager = new DatabaseManager(getActivity());
        dbManager.open();

        loadProjects();

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Project selectedProject = projectList.get(position);
                ProjectDetailFragment fragment = ProjectDetailFragment.newInstance(selectedProject.getId(), selectedProject.getName());
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right);
                ft.replace(R.id.fragment_container, fragment);
                ft.addToBackStack(null);
                ft.commit();
            }
        });

        gridView.setOnItemLongClickListener((parent, view, position, id) -> {
            Project selectedProject = projectList.get(position);

            final android.app.Dialog dialog = new android.app.Dialog(getActivity(), android.R.style.Theme_Translucent_NoTitleBar);
            View sidebarView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_bb10_context_sidebar, null);
            dialog.setContentView(sidebarView);

            if (dialog.getWindow() != null) {
                android.view.WindowManager.LayoutParams lp = new android.view.WindowManager.LayoutParams();
                lp.copyFrom(dialog.getWindow().getAttributes());
                lp.width = android.view.WindowManager.LayoutParams.MATCH_PARENT;
                lp.height = android.view.WindowManager.LayoutParams.MATCH_PARENT;
                lp.gravity = android.view.Gravity.END;
                lp.windowAnimations = R.style.BB10SidebarAnimation;
                dialog.getWindow().setAttributes(lp);
            }

            sidebarView.findViewById(R.id.sidebar_dim_scrim).setOnClickListener(v -> dialog.dismiss());

            TextView tvTitle = (TextView) sidebarView.findViewById(R.id.sidebar_task_title);
            tvTitle.setText(selectedProject.getName());

            ListView lvOptions = (ListView) sidebarView.findViewById(R.id.lv_sidebar_options);
            final List<String> optionsList = new java.util.ArrayList<>();
            optionsList.add("Edit");

            ArrayAdapter<String> adapterOptions = new ArrayAdapter<String>(getActivity(), R.layout.item_sidebar_option, R.id.tv_option_title, optionsList) {
                @Override
                public View getView(int pos, View convertView1, ViewGroup parentGroup) {
                    if (convertView1 == null) {
                        convertView1 = LayoutInflater.from(getContext()).inflate(R.layout.item_sidebar_option, parentGroup, false);
                    }
                    TextView tv = (TextView) convertView1.findViewById(R.id.tv_option_title);
                    android.widget.ImageView iv = (android.widget.ImageView) convertView1.findViewById(R.id.iv_option_icon);
                    tv.setText(optionsList.get(pos));
                    iv.setImageResource(R.drawable.ic_bb10_compose);
                    return convertView1;
                }
            };
            lvOptions.setAdapter(adapterOptions);

            lvOptions.setOnItemClickListener((parentAdapter, view1, pos, optionId) -> {
                dialog.dismiss();
                EditProjectFragment editFragment = EditProjectFragment.newInstance(selectedProject.getId(), selectedProject.getName());
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right);
                ft.replace(R.id.fragment_container, editFragment);
                ft.addToBackStack(null);
                ft.commit();
            });

            sidebarView.findViewById(R.id.sidebar_bottom_delete).setOnClickListener(deleteView -> {
                dialog.dismiss();
                com.ismailmushraf.bujo.utils.BB10DialogHelper.showConfirmDialog(getActivity(), "Delete Project", "Are you sure you want to delete '" + selectedProject.getName() + "' and all its tasks?", "Delete", () -> {
                    dbManager.deleteProjectAndAllEntries(selectedProject.getId());
                    loadProjects();
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).refreshDrawer();
                    }
                });
            });

            dialog.show();
            return true;
        });

        return root;
    }

    private void processQuickProjectCreation(android.widget.EditText etSearch) {
        String projectName = etSearch.getText().toString().trim();
        if (projectName.isEmpty()) return;

        // Check if project name already exists (case-insensitive)
        for (Project p : allProjectsList) {
            if (p != null && p.getName() != null && p.getName().equalsIgnoreCase(projectName)) {
                android.widget.Toast.makeText(getActivity(), "Project already exists", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Project newProject = new Project();
        newProject.setName(projectName);
        newProject.setWeight(1);
        int defaultColor = getResources().getColor(R.color.bb10_folder_blue);
        newProject.setColor(defaultColor);

        dbManager.insertProject(newProject);
        loadProjects();

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).refreshDrawer();
        }

        etSearch.setText("");
    }

    public void openCreateProjectScreen() {
        EditProjectFragment createFragment = EditProjectFragment.newInstanceForCreate();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right);
        ft.replace(R.id.fragment_container, createFragment);
        ft.addToBackStack(null);
        ft.commit();
    }

    private void filterProjects(String query) {
        if (adapter == null) return;
        List<Project> filtered = com.ismailmushraf.bujo.utils.SearchHelper.filter(allProjectsList, query, p -> p != null ? p.getName() : "");
        adapter.clear();
        adapter.addAll(filtered);
        adapter.notifyDataSetChanged();
    }

    private void loadProjects() {
        allProjectsList = dbManager.getAllProjects();
        projectList = new java.util.ArrayList<>(allProjectsList);
        adapter = new ProjectFolderAdapter(getActivity(), projectList, dbManager);
        gridView.setAdapter(adapter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dbManager != null) {
            dbManager.close();
        }
    }
}
