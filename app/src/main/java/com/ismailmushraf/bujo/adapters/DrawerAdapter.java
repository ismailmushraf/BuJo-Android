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

    private static class ViewHolder {
        TextView tvSection;
        TextView tvTitle;
        android.widget.ImageView ivIcon;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DrawerItem item = getItem(position);
        int viewType = getItemViewType(position);
        ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            holder = new ViewHolder();
            if (viewType == DrawerItem.TYPE_SECTION) {
                convertView = inflater.inflate(R.layout.item_drawer_section, parent, false);
                holder.tvSection = (TextView) convertView.findViewById(R.id.drawer_section_title);
            } else {
                convertView = inflater.inflate(R.layout.item_drawer_row, parent, false);
                holder.tvTitle = (TextView) convertView.findViewById(R.id.drawer_title);
                holder.ivIcon = (android.widget.ImageView) convertView.findViewById(R.id.drawer_icon);
            }
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (viewType == DrawerItem.TYPE_SECTION) {
            if (holder.tvSection != null) holder.tvSection.setText(item.title);
        } else {
            if (holder.tvTitle != null) holder.tvTitle.setText(item.title);
            if (holder.ivIcon != null) {
                if (item.iconResId != 0) {
                    holder.ivIcon.setVisibility(View.VISIBLE);
                    holder.ivIcon.setImageResource(item.iconResId);
                } else {
                    holder.ivIcon.setVisibility(View.GONE);
                }
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
