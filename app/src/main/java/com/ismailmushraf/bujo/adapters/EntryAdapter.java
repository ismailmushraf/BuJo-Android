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

public class EntryAdapter extends ArrayAdapter<Object> {

    public interface OnEntryInteractionListener {
        void onEntryTextClick(Entry entry);
        void onEntryLongClick(Entry entry, View view);
    }

    private static final int TYPE_ENTRY = 0;
    private static final int TYPE_HEADER = 1;

    private boolean showTags = true;
    private boolean isDailyLog = false;
    private OnEntryInteractionListener listener;
    private EntryUIHelper uiHelper;

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.US);
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.US);
    private final int colorText;
    private final int colorTextSecondary;

    public EntryAdapter(Context context, List<Object> entries, boolean showTags, boolean isDailyLog) {
        super(context, 0, entries);
        this.showTags = showTags;
        this.isDailyLog = isDailyLog;
        this.colorText = context.getResources().getColor(R.color.bujo_text);
        this.colorTextSecondary = context.getResources().getColor(R.color.bujo_text_secondary);
    }

    public EntryAdapter(Context context, List<Object> entries, boolean showTags) {
        super(context, 0, entries);
        this.showTags = showTags;
        this.colorText = context.getResources().getColor(R.color.bujo_text);
        this.colorTextSecondary = context.getResources().getColor(R.color.bujo_text_secondary);
    }

    public EntryAdapter(Context context, List<Object> entries) {
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

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return (getItem(position) instanceof String) ? TYPE_HEADER : TYPE_ENTRY;
    }

    private static class EntryViewHolder {
        TextView tvSignifier;
        TextView tvContent;
        TextView tvDeadline;
        TextView tvTag;
        TextView tvLock;
        View interactionArea;
    }

    private static class HeaderViewHolder {
        TextView tvTitle;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int type = getItemViewType(position);

        if (type == TYPE_HEADER) {
            return getHeaderView((String) getItem(position), convertView, parent);
        } else {
            return getEntryView((Entry) getItem(position), convertView, parent);
        }
    }

    private View getHeaderView(String title, View convertView, ViewGroup parent) {
        HeaderViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_drawer_section, parent, false);
            holder = new HeaderViewHolder();
            holder.tvTitle = (TextView) convertView.findViewById(R.id.drawer_section_title);
            convertView.setTag(holder);
        } else {
            holder = (HeaderViewHolder) convertView.getTag();
        }
        holder.tvTitle.setText(title);
        return convertView;
    }

    private View getEntryView(final Entry entry, View convertView, ViewGroup parent) {
        EntryViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_entry_row, parent, false);
            holder = new EntryViewHolder();
            holder.tvSignifier = (TextView) convertView.findViewById(R.id.row_signifier);
            holder.tvContent = (TextView) convertView.findViewById(R.id.row_content);
            holder.tvDeadline = (TextView) convertView.findViewById(R.id.row_deadline);
            holder.tvTag = (TextView) convertView.findViewById(R.id.row_tag);
            holder.tvLock = (TextView) convertView.findViewById(R.id.row_lock_indicator);
            holder.interactionArea = convertView.findViewById(R.id.row_interaction_area);
            convertView.setTag(holder);
        } else {
            holder = (EntryViewHolder) convertView.getTag();
        }

        // 1. Bullet/Checkbox Area -> Completion Toggle
        holder.tvSignifier.setOnClickListener(v -> {
            if (uiHelper != null) {
                uiHelper.toggleEntryCompletion(entry, v);
            }
        });

        // 2. Interaction Area -> Open Detail Modal
        holder.interactionArea.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEntryTextClick(entry);
            }
        });

        // 3. Universal Long Press
        View.OnLongClickListener longClickListener = v -> {
            if (listener != null) {
                listener.onEntryLongClick(entry, v);
                return true;
            }
            return false;
        };
        holder.tvSignifier.setOnLongClickListener(longClickListener);
        holder.interactionArea.setOnLongClickListener(longClickListener);

        // --- Formatting Rules ---
        if (entry.isMigrated()) {
            holder.tvSignifier.setBackgroundResource(android.R.color.transparent);
            holder.tvSignifier.setText(">");
            holder.tvSignifier.setTextColor(colorText);
        } else if ("*".equals(entry.getSignifier())) {
            // BB10 style checkmark
            if (entry.isCompleted()) {
                holder.tvSignifier.setBackgroundResource(R.drawable.bb10_checkbox_checked_bg);
                holder.tvSignifier.setText("✓");
                holder.tvSignifier.setTextColor(getContext().getResources().getColor(android.R.color.white));
            } else {
                holder.tvSignifier.setBackgroundResource(R.drawable.bb10_checkbox_unchecked_bg);
                holder.tvSignifier.setText("");
                holder.tvSignifier.setTextColor(colorText);
            }
        } else if ("o".equals(entry.getSignifier())) {
            holder.tvSignifier.setBackgroundResource(entry.isCompleted() ? R.drawable.ic_event_completed : R.drawable.ic_calendar);
            holder.tvSignifier.setText("");
        } else {
            holder.tvSignifier.setBackgroundResource(android.R.color.transparent);
            holder.tvSignifier.setText(entry.getSignifier());
            holder.tvSignifier.setTextColor(colorText);
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

        if (holder.tvLock != null) {
            holder.tvLock.setVisibility(entry.isLocked() ? View.VISIBLE : View.GONE);
        }

        return convertView;
    }
}
