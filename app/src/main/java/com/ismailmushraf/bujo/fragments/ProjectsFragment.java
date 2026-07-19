package com.ismailmushraf.bujo.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.ismailmushraf.bujo.R;
import com.ismailmushraf.bujo.MainActivity;
import com.ismailmushraf.bujo.db.DatabaseManager;
import com.ismailmushraf.bujo.models.Project;

import java.util.List;

public class ProjectsFragment extends Fragment {

    private ListView listView;
    private DatabaseManager dbManager;
    private List<Project> projectList;
    private ArrayAdapter<String> adapter;
    private String[] projectNames;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_projects, container, false);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setToolbarTitle("PROJECTS");
            ((MainActivity) getActivity()).setToolbarSubtitle("");
        }

        listView = (ListView) root.findViewById(R.id.lv_projects);
        Button btnAddProject = (Button) root.findViewById(R.id.btn_add_project);

        dbManager = new DatabaseManager(getActivity());
        dbManager.open();

        loadProjects();

        btnAddProject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddProjectDialog();
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
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
        projectNames = new String[projectList.size()];
        for (int i = 0; i < projectList.size(); i++) {
            projectNames[i] = projectList.get(i).getName();
        }

        adapter = new ArrayAdapter<String>(getActivity(), R.layout.item_drawer, projectNames);
        listView.setAdapter(adapter);
    }

    private void showAddProjectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("New Project");

        View viewInflated = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_add_project, (ViewGroup) getView(), false);
        final EditText input = (EditText) viewInflated.findViewById(R.id.et_project_name);
        builder.setView(viewInflated);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String projectName = input.getText().toString().trim();
                if (!projectName.isEmpty()) {
                    Project newProject = new Project();
                    newProject.setName(projectName);
                    dbManager.insertProject(newProject);
                    loadProjects();
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).refreshDrawer();
                    }
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dbManager != null) {
            dbManager.close();
        }
    }
}
