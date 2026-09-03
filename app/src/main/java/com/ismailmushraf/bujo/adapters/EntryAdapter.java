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
import com.ismailmushraf.bujo.utils.EntryUIHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EntryAdapter extends ArrayAdapter<Entry> {

    public interface OnEntryInteractionListener {
        void onEntryTextClick(Entry entry);
        void onEntryLongClick(Entry entry, View view);
    }

    private boolean showTags = true;
    private boolean isDailyLog = false;
    private OnEntryInteractionListener listener;
    private EntryUIHelper uiHelper;

    // --- Performance Optimizations: Reusable Objects ---
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.US);
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.US);
    private final int colorText;
    private final int colorTextSecondary;

    public EntryAdapter(Context context, List<Entry> entries, boolean showTags, boolean isDailyLog) {
        super(context, 0, entries);
        this.showTags = showTags;
        this.isDailyLog = isDailyLog;
        this.colorText = context.getResources().getColor(R.color.bujo_text);
        this.colorTextSecondary = context.getResources().getColor(R.color.bujo_text_secondary);
    }

    public EntryAdapter(Context context, List<Entry> entries, boolean showTags) {
        super(context, 0, entries);
        this.showTags = showTags;
        this.colorText = context.getResources().getColor(R.color.bujo_text);
        this.colorTextSecondary = context.getResources().getColor(R.color.bujo_text_secondary);
    }

    public EntryAdapter(Context context, List<Entry> entries) {
        super(context, 0, entries);
        this.colorText = context.getResources().getColor(R.color.bujo_text);
        this.colorTextSecondary = context.getResources().getColor(R.color.bujo_text_secondary);
    }

    public void setOnEntryInteractionListener(OnEntryInteractionListener listener) {
        this.listener = listener;
    }

    public void setUIHelper(EntryUIHelper helper) {
        this.uiHelper = helper;
    }

    private static class ViewHolder {
        TextView tvSignifier;
        TextView tvContent;
        TextView tvDeadline;
        TextView tvTag;
        View interactionArea;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final Entry entry = getItem(position);
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_entry_row, parent, false);
            holder = new ViewHolder();
            holder.tvSignifier = (TextView) convertView.findViewById(R.id.row_signifier);
            holder.tvContent = (TextView) convertView.findViewById(R.id.row_content);
            holder.tvDeadline = (TextView) convertView.findViewById(R.id.row_deadline);
            holder.tvTag = (TextView) convertView.findViewById(R.id.row_tag);
            holder.interactionArea = convertView.findViewById(R.id.row_interaction_area);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // --- Interaction Splitting ---
        
        // 1. Bullet/Checkbox Area -> Completion Toggle
        holder.tvSignifier.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (uiHelper != null) {
                    uiHelper.toggleEntryCompletion(entry, v);
                }
            }
        });

        // 2. Interaction Area -> Open Detail Modal
        holder.interactionArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onEntryTextClick(entry);
                }
            }
        });

        // 3. Universal Long Press (Both areas trigger the same menu)
        View.OnLongClickListener longClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (listener != null) {
                    listener.onEntryLongClick(entry, v);
                    return true;
                }
                return false;
            }
        };
        holder.tvSignifier.setOnLongClickListener(longClickListener);
        holder.interactionArea.setOnLongClickListener(longClickListener);

        // --- Formatting Rules ---
        if (entry.isMigrated()) {
            holder.tvSignifier.setBackgroundResource(android.R.color.transparent);
            holder.tvSignifier.setText(">");
        } else if ("*".equals(entry.getSignifier())) {
            holder.tvSignifier.setBackgroundResource(R.drawable.shape_bujo_box);
            holder.tvSignifier.setText(entry.isCompleted() ? "✓" : "");
        } else {
            holder.tvSignifier.setBackgroundResource(android.R.color.transparent);
            holder.tvSignifier.setText(entry.getSignifier());
        }

        if (entry.isCompleted()) {
            holder.tvContent.setPaintFlags(holder.tvContent.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvContent.setTextColor(colorTextSecondary);
        } else {
            holder.tvContent.setPaintFlags(holder.tvContent.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvContent.setTextColor(colorText);
        }

        holder.tvContent.setText(entry.getContent());

        if (entry.getDeadline() > 0) {
            holder.tvDeadline.setVisibility(View.VISIBLE);
            if (isDailyLog) {
                if (entry.hasTime()) {
                    holder.tvDeadline.setText("Time: " + timeFormat.format(new Date(entry.getDeadline())));
                } else {
                    holder.tvDeadline.setVisibility(View.GONE);
                }
            } else {
                SimpleDateFormat sdf = entry.hasTime() ? dateTimeFormat : dateFormat;
                holder.tvDeadline.setText((entry.hasTime() ? "Reminder: " : "Date: ") + sdf.format(new Date(entry.getDeadline())));
            }
        } else {
            holder.tvDeadline.setVisibility(View.GONE);
        }

        if (showTags && entry.getProjectTag() != null && !entry.getProjectTag().isEmpty()) {
            holder.tvTag.setVisibility(View.VISIBLE);
            holder.tvTag.setText("#" + entry.getProjectTag());
        } else {
            holder.tvTag.setVisibility(View.GONE);
        }

        return convertView;
    }
}
