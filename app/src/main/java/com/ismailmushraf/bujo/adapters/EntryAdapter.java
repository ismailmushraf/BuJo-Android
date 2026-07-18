package com.ismailmushraf.bujo.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.ismailmushraf.bujo.R;
import com.ismailmushraf.bujo.models.Entry;
import java.util.List;

public class EntryAdapter extends ArrayAdapter<Entry> {

    public EntryAdapter(Context context, List<Entry> entries) {
        super(context, 0, entries);
    }

    private static class ViewHolder {
        TextView tvSignifier;
        TextView tvContent;
        TextView tvTag;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Entry entry = getItem(position);
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_entry_row, parent, false);
            holder = new ViewHolder();
            holder.tvSignifier = (TextView) convertView.findViewById(R.id.row_signifier);
            holder.tvContent = (TextView) convertView.findViewById(R.id.row_content);
            holder.tvTag = (TextView) convertView.findViewById(R.id.row_tag);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Apply formatting rules based on status
        if (entry.isCompleted()) {
            holder.tvSignifier.setText("X");
            holder.tvContent.setPaintFlags(holder.tvContent.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvContent.setTextColor(0xFF888888);
        } else {
            holder.tvSignifier.setText(entry.getSignifier());
            holder.tvContent.setPaintFlags(holder.tvContent.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvContent.setTextColor(0xFF111111);
        }

        holder.tvContent.setText(entry.getContent());

        // Show tag if it belongs to a project collection
        if (entry.getProjectTag() != null && !entry.getProjectTag().isEmpty()) {
            holder.tvTag.setVisibility(View.VISIBLE);
            holder.tvTag.setText("#" + entry.getProjectTag());
        } else {
            holder.tvTag.setVisibility(View.GONE);
        }

        return convertView;
    }
}