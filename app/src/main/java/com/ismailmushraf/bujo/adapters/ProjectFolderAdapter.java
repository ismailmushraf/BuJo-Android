package com.ismailmushraf.bujo.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.ismailmushraf.bujo.R;
import com.ismailmushraf.bujo.models.Project;
import com.ismailmushraf.bujo.db.DatabaseManager;

import java.util.List;

public class ProjectFolderAdapter extends ArrayAdapter<Project> {
    private LayoutInflater inflater;
    private DatabaseManager dbManager;

    public ProjectFolderAdapter(Context context, List<Project> projects, DatabaseManager db) {
        super(context, 0, projects);
        inflater = LayoutInflater.from(context);
        this.dbManager = db;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_bb10_folder, parent, false);
        }

        Project project = getItem(position);
        
        TextView tvTitle = convertView.findViewById(R.id.tv_folder_title);
        TextView tvCount = convertView.findViewById(R.id.tv_folder_count);
        View background = convertView.findViewById(R.id.folder_background);

        tvTitle.setText(project.getName());
        
        // Count tasks in project
        int count = dbManager.getTaskCountForProject(project.getId());
        tvCount.setText(count + (count == 1 ? " Task" : " Tasks"));
        
        // Cycle colors dynamically using a single bb10_folder.xml template
        int[] colors = {
            getContext().getResources().getColor(R.color.bb10_folder_dark),
            getContext().getResources().getColor(R.color.bb10_folder_blue),
            getContext().getResources().getColor(R.color.bb10_folder_yellow),
            getContext().getResources().getColor(R.color.bb10_folder_green),
            getContext().getResources().getColor(R.color.bb10_folder_purple),
            getContext().getResources().getColor(R.color.bb10_folder_red)
        };
        int selectedColor = (project != null && project.getColor() != 0) ? project.getColor() : colors[position % colors.length];

        android.graphics.drawable.LayerDrawable folderDrawable = 
                (android.graphics.drawable.LayerDrawable) getContext().getResources().getDrawable(R.drawable.bb10_folder).mutate();
        
        android.graphics.drawable.GradientDrawable body = 
                (android.graphics.drawable.GradientDrawable) folderDrawable.findDrawableByLayerId(R.id.folder_body);
        android.graphics.drawable.GradientDrawable tab = 
                (android.graphics.drawable.GradientDrawable) folderDrawable.findDrawableByLayerId(R.id.folder_tab);
        
        if (body != null) body.setColor(selectedColor);
        if (tab != null) tab.setColor(selectedColor);

        background.setBackgroundDrawable(folderDrawable);

        return convertView;
    }
}