package com.ismailmushraf.bujo.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
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
    private ProjectFolderAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_projects, container, false);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setToolbarTitle("");
            ((MainActivity) getActivity()).setToolbarSubtitle("");
        }

        gridView = (GridView) root.findViewById(R.id.gv_projects);
        final EditText etNewProject = (EditText) root.findViewById(R.id.et_new_project);

        dbManager = new DatabaseManager(getActivity());
        dbManager.open();

        loadProjects();

        etNewProject.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE || actionId == 0) {
                    processNewProject(etNewProject);
                    return true;
                }
                return false;
            }
        });

        etNewProject.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN &&
                        (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER || keyCode == KeyEvent.KEYCODE_PLUS)) {
                    processNewProject(etNewProject);
                    return true;
                }
                return false;
            }
        });

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Project selectedProject = projectList.get(position);
                ProjectDetailFragment fragment = ProjectDetailFragment.newInstance(selectedProject.getId(), selectedProject.getName());
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.fragment_container, fragment);
                ft.addToBackStack(null);
                ft.commit();
            }
        });

        return root;
    }

    private void loadProjects() {
        projectList = dbManager.getAllProjects();
        adapter = new ProjectFolderAdapter(getActivity(), projectList, dbManager);
        gridView.setAdapter(adapter);
    }

    private void processNewProject(EditText etNewProject) {
        String projectName = etNewProject.getText().toString().trim();
        if (!projectName.isEmpty()) {
            Project newProject = new Project();
            newProject.setName(projectName);
            dbManager.insertProject(newProject);
            loadProjects();
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).refreshDrawer();
            }
            etNewProject.setText("");
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
