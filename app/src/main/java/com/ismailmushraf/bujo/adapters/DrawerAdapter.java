package com.ismailmushraf.bujo.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.ismailmushraf.bujo.R;
import com.ismailmushraf.bujo.models.DrawerItem;

import java.util.List;

public class DrawerAdapter extends ArrayAdapter<DrawerItem> {

    private int selectedPosition = -1;

    public DrawerAdapter(Context context, List<DrawerItem> objects) {
        super(context, 0, objects);
    }

    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
        notifyDataSetChanged();
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).getType();
    }

    @Override
    public boolean isEnabled(int position) {
        return getItem(position).getType() != DrawerItem.TYPE_SECTION;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DrawerItem item = getItem(position);
        int viewType = getItemViewType(position);

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            if (viewType == DrawerItem.TYPE_SECTION) {
                convertView = inflater.inflate(R.layout.item_drawer_section, parent, false);
            } else {
                convertView = inflater.inflate(R.layout.item_drawer_row, parent, false);
            }
        }

        if (viewType == DrawerItem.TYPE_SECTION) {
            TextView tvSection = (TextView) convertView.findViewById(R.id.drawer_section_title);
            tvSection.setText(item.title);
        } else {
            TextView tvTitle = (TextView) convertView.findViewById(R.id.drawer_title);
            TextView tvIcon = (TextView) convertView.findViewById(R.id.drawer_icon);

            tvTitle.setText(item.title);
            if (item.icon != null) {
                tvIcon.setVisibility(View.VISIBLE);
                tvIcon.setText(item.icon);
            } else {
                tvIcon.setVisibility(View.GONE);
            }

            // Highlighting based on selection
            if (position == selectedPosition) {
                convertView.setBackgroundResource(R.color.blue_selection);
            } else {
                convertView.setBackgroundResource(android.R.color.transparent);
            }
        }

        return convertView;
    }
}
