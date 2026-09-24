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
        
        // Cycle colors
        int[] bgs = {
            R.drawable.bb10_folder_dark,
            R.drawable.bb10_folder_blue,
            R.drawable.bb10_folder_yellow,
            R.drawable.bb10_folder_green,
            R.drawable.bb10_folder_purple,
            R.drawable.bb10_folder_red
        };
        background.setBackgroundResource(bgs[position % bgs.length]);

        return convertView;
    }
}