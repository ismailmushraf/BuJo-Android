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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EntryAdapter extends ArrayAdapter<Entry> {

    private boolean showTags = true;

    public EntryAdapter(Context context, List<Entry> entries, boolean showTags) {
        super(context, 0, entries);
        this.showTags = showTags;
    }

    public EntryAdapter(Context context, List<Entry> entries) {
        super(context, 0, entries);
    }

    private static class ViewHolder {
        TextView tvSignifier;
        TextView tvContent;
        TextView tvDeadline;
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
            holder.tvDeadline = (TextView) convertView.findViewById(R.id.row_deadline);
            holder.tvTag = (TextView) convertView.findViewById(R.id.row_tag);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Apply formatting rules based on status
        if (entry.isMigrated()) {
            holder.tvSignifier.setBackgroundResource(android.R.color.transparent);
            holder.tvSignifier.setText(">");
            holder.tvSignifier.setTextSize(20);
        } else if ("*".equals(entry.getSignifier())) {
            holder.tvSignifier.setBackgroundResource(R.drawable.shape_bujo_box);
            if (entry.isCompleted()) {
                holder.tvSignifier.setText("✓");
                holder.tvSignifier.setTextSize(16);
            } else {
                holder.tvSignifier.setText("");
            }
        } else {
            holder.tvSignifier.setBackgroundResource(android.R.color.transparent);
            holder.tvSignifier.setText(entry.getSignifier());
            holder.tvSignifier.setTextSize(20);
        }

        if (entry.isCompleted()) {
            holder.tvContent.setPaintFlags(holder.tvContent.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            // Fetch the dynamic secondary text color (adapts to light/dark)
            holder.tvContent.setTextColor(getContext().getResources().getColor(R.color.bujo_text_secondary));
        } else {
            holder.tvContent.setPaintFlags(holder.tvContent.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            // Fetch the dynamic primary text color (adapts to light/dark)
            holder.tvContent.setTextColor(getContext().getResources().getColor(R.color.bujo_text));
        }

        holder.tvContent.setText(entry.getContent());

        // Render Deadline if present
        if (entry.getDeadline() > 0) {
            holder.tvDeadline.setVisibility(View.VISIBLE);
            if (entry.hasTime()) {
                // Time was explicitly set, show the clock
                SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US);
                holder.tvDeadline.setText("Reminder: " + sdf.format(new Date(entry.getDeadline())));
            } else {
                // No time was set, just show the date
                SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);
                holder.tvDeadline.setText("Date: " + sdf.format(new Date(entry.getDeadline())));
            }
        } else {
            holder.tvDeadline.setVisibility(View.GONE);
        }

        // Render tag properly using the showTags boolean
        if (showTags && entry.getProjectTag() != null && !entry.getProjectTag().isEmpty()) {
            holder.tvTag.setVisibility(View.VISIBLE);
            holder.tvTag.setText("#" + entry.getProjectTag());
        } else {
            holder.tvTag.setVisibility(View.GONE);
        }

        return convertView;
    }
}
