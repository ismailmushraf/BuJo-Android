package com.ismailmushraf.bujo.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.ismailmushraf.bujo.MainActivity;
import com.ismailmushraf.bujo.R;
import com.ismailmushraf.bujo.db.DatabaseManager;
import com.ismailmushraf.bujo.models.Project;

import java.util.List;

public class EditProjectFragment extends Fragment {

    private static final String ARG_PROJECT_ID = "project_id";
    private static final String ARG_PROJECT_NAME = "project_name";

    private int projectId;
    private String originalName = "";
    private int originalWeight = 1;
    private int currentWeight = 1;
    private int originalColor = 0;
    private int currentColor = 0;

    private DatabaseManager dbManager;
    private TextView btnSave;
    private TextView[] starViews = new TextView[5];
    private View btnColorPicker;

    public static EditProjectFragment newInstance(int projectId, String projectName) {
        EditProjectFragment fragment = new EditProjectFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PROJECT_ID, projectId);
        args.putString(ARG_PROJECT_NAME, projectName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_edit_project, container, false);

        if (getArguments() != null) {
            projectId = getArguments().getInt(ARG_PROJECT_ID);
            originalName = getArguments().getString(ARG_PROJECT_NAME);
        }

        dbManager = new DatabaseManager(getActivity());
        dbManager.open();

        List<Project> allProjects = dbManager.getAllProjects();
        for (Project p : allProjects) {
            if (p.getId() == projectId) {
                originalName = p.getName();
                originalWeight = p.getWeight() > 0 ? p.getWeight() : 1;
                originalColor = p.getColor();
                break;
            }
        }
        currentWeight = originalWeight;
        currentColor = originalColor;

        final EditText etTitle = (EditText) root.findViewById(R.id.et_project_title);
        btnSave = (TextView) root.findViewById(R.id.btn_save);
        btnColorPicker = root.findViewById(R.id.btn_edit_color_picker);

        etTitle.setText(originalName);

        starViews[0] = (TextView) root.findViewById(R.id.star_1);
        starViews[1] = (TextView) root.findViewById(R.id.star_2);
        starViews[2] = (TextView) root.findViewById(R.id.star_3);
        starViews[3] = (TextView) root.findViewById(R.id.star_4);
        starViews[4] = (TextView) root.findViewById(R.id.star_5);

        for (int i = 0; i < 5; i++) {
            final int starWeight = i + 1;
            if (starViews[i] != null) {
                starViews[i].setOnClickListener(v -> {
                    updateStars(starWeight);
                    checkSaveButtonState(etTitle);
                });
            }
        }
        updateStars(currentWeight);

        updateColorPickerPreview(currentColor);
        if (btnColorPicker != null) {
            btnColorPicker.setOnClickListener(v -> showColorPaletteDialog(etTitle));
        }

        etTitle.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkSaveButtonState(etTitle);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        root.findViewById(R.id.btn_cancel).setOnClickListener(v -> {
            if (getFragmentManager() != null) {
                getFragmentManager().popBackStack();
            }
        });

        btnSave.setOnClickListener(v -> {
            String newTitle = etTitle.getText().toString().trim();
            if (!newTitle.isEmpty()) {
                Project project = new Project(projectId, newTitle);
                project.setWeight(currentWeight);
                project.setColor(currentColor);
                dbManager.updateProject(project);

                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).refreshDrawer();
                }

                if (getFragmentManager() != null) {
                    getFragmentManager().popBackStack();
                }
            }
        });

        checkSaveButtonState(etTitle);
        return root;
    }

    private void updateStars(int weight) {
        currentWeight = weight;
        for (int i = 0; i < 5; i++) {
            if (starViews[i] != null) {
                starViews[i].setText(i < weight ? "★" : "☆");
            }
        }
    }

    private void updateColorPickerPreview(int color) {
        if (btnColorPicker == null) return;
        int displayColor = (color != 0) ? color : getResources().getColor(R.color.bb10_folder_blue);
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        shape.setColor(displayColor);
        btnColorPicker.setBackgroundDrawable(shape);
    }

    private void showColorPaletteDialog(final EditText etTitle) {
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
                currentColor = color;
                updateColorPickerPreview(currentColor);
                checkSaveButtonState(etTitle);
                dialog.dismiss();
            });
            layout.addView(circle);
        }
        dialog.show();
    }

    private void checkSaveButtonState(EditText etTitle) {
        String newName = etTitle.getText().toString().trim();
        boolean isValid = !newName.isEmpty();
        boolean hasChanged = !newName.equals(originalName) || currentWeight != originalWeight || currentColor != originalColor;
        boolean enable = isValid && hasChanged;

        btnSave.setEnabled(enable);
        btnSave.setTextColor(enable ? android.graphics.Color.parseColor("#00a8df") : android.graphics.Color.parseColor("#A0C8E6"));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dbManager != null) dbManager.close();
    }
}