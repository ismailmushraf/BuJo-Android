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
import android.widget.ArrayAdapter;
import android.widget.EditText;
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
    private ProjectFolderAdapter adapter;
    private int selectedColor = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_projects, container, false);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setToolbarTitle("");
            ((MainActivity) getActivity()).setToolbarSubtitle("");
        }

        gridView = (GridView) root.findViewById(R.id.gv_projects);
        final EditText etNewProject = (EditText) root.findViewById(R.id.et_new_project);
        View btnColorPicker = root.findViewById(R.id.btn_color_picker);

        if (btnColorPicker != null) {
            btnColorPicker.setOnClickListener(v -> showColorPaletteDialog(btnColorPicker));
        }

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
                dbManager.deleteProjectAndAllEntries(selectedProject.getId());
                loadProjects();
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).refreshDrawer();
                }
            });

            dialog.show();
            return true;
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
            newProject.setColor(selectedColor);
            dbManager.insertProject(newProject);
            loadProjects();
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).refreshDrawer();
            }
            etNewProject.setText("");
        }
    }

    private void showColorPaletteDialog(final View colorPickerView) {
        int[] palette = {
            getResources().getColor(R.color.bb10_folder_blue),
            getResources().getColor(R.color.bb10_folder_dark),
            getResources().getColor(R.color.bb10_folder_yellow),
            getResources().getColor(R.color.bb10_folder_green),
            getResources().getColor(R.color.bb10_folder_purple),
            getResources().getColor(R.color.bb10_folder_red)
        };

        android.widget.LinearLayout layout = new android.widget.LinearLayout(getActivity());
        layout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        layout.setPadding(32, 32, 32, 32);
        layout.setGravity(android.view.Gravity.CENTER);

        final AlertDialog dialog = new AlertDialog.Builder(getActivity(), R.style.BujoDialog)
                .setTitle("Select Folder Color")
                .setView(layout)
                .create();

        for (final int color : palette) {
            View circle = new View(getActivity());
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(80, 80);
            params.setMargins(12, 12, 12, 12);
            circle.setLayoutParams(params);

            android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
            shape.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            shape.setColor(color);
            circle.setBackgroundDrawable(shape);

            circle.setOnClickListener(v -> {
                selectedColor = color;
                android.graphics.drawable.GradientDrawable pickerShape = new android.graphics.drawable.GradientDrawable();
                pickerShape.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                pickerShape.setColor(color);
                colorPickerView.setBackgroundDrawable(pickerShape);
                dialog.dismiss();
            });
            layout.addView(circle);
        }
        dialog.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dbManager != null) {
            dbManager.close();
        }
    }
}
