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
    private boolean completionTogglesEnabled = true;
    private OnEntryInteractionListener listener;
    private EntryUIHelper uiHelper;

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.US);
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.US);
    private final int colorText;
    private final int colorTextSecondary;
    private final Date reusableDate = new Date();
    private final com.ismailmushraf.bujo.utils.CustomStrikethroughSpan strikethroughSpan;

    public EntryAdapter(Context context, List<Object> entries, boolean showTags, boolean isDailyLog) {
        super(context, 0, entries);
        this.showTags = showTags;
        this.isDailyLog = isDailyLog;
        this.colorText = context.getResources().getColor(R.color.bujo_text);
        this.colorTextSecondary = context.getResources().getColor(R.color.bujo_text_secondary);
        this.strikethroughSpan = new com.ismailmushraf.bujo.utils.CustomStrikethroughSpan(colorTextSecondary, context.getResources().getColor(R.color.bb10_folder_red));
    }

    public EntryAdapter(Context context, List<Object> entries, boolean showTags) {
        this(context, entries, showTags, false);
    }

    public EntryAdapter(Context context, List<Object> entries) {
        this(context, entries, true, false);
    }

    private long getStartOfToday() {
        java.util.Calendar todayCal = java.util.Calendar.getInstance();
        todayCal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        todayCal.set(java.util.Calendar.MINUTE, 0);
        todayCal.set(java.util.Calendar.SECOND, 0);
        todayCal.set(java.util.Calendar.MILLISECOND, 0);
        return todayCal.getTimeInMillis();
    }

    private synchronized String formatDate(SimpleDateFormat sdf, long timeMs) {
        reusableDate.setTime(timeMs);
        return sdf.format(reusableDate);
    }

    public void setOnEntryInteractionListener(OnEntryInteractionListener listener) {
        this.listener = listener;
    }

    public void setUIHelper(EntryUIHelper helper) {
        this.uiHelper = helper;
    }

    /** Disables the checkbox action for read-only task contexts such as Logbook. */
    public void setCompletionTogglesEnabled(boolean enabled) {
        completionTogglesEnabled = enabled;
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
        View ivLock;
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
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_section_header, parent, false);
            holder = new HeaderViewHolder();
            holder.tvTitle = (TextView) convertView.findViewById(R.id.section_header_title);
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
            holder.ivLock = convertView.findViewById(R.id.row_lock_indicator);
            holder.interactionArea = convertView.findViewById(R.id.row_interaction_area);
            convertView.setTag(holder);
        } else {
            holder = (EntryViewHolder) convertView.getTag();
        }

        // 1. Bullet/Checkbox Area -> Completion Toggle
        holder.tvSignifier.setOnClickListener(v -> {
            if (completionTogglesEnabled && uiHelper != null) {
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
        } else {
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
        }

        String content = entry.getContent() != null ? entry.getContent() : "";
        if (entry.isCompleted()) {
            android.text.SpannableString spannable = new android.text.SpannableString(content);
            spannable.setSpan(strikethroughSpan, 0, content.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            holder.tvContent.setText(spannable);
        } else {
            holder.tvContent.setPaintFlags(holder.tvContent.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvContent.setText(content);
            holder.tvContent.setTextColor(colorText);
        }

        if (entry.getDeadline() > 0) {
            holder.tvDeadline.setVisibility(View.VISIBLE);

            long startOfToday = getStartOfToday();
            boolean isOverdue = !entry.isCompleted() && entry.getDeadline() < startOfToday;

            if (isDailyLog) {
                if (entry.hasTime()) {
                    holder.tvDeadline.setText(getContext().getString(com.ismailmushraf.bujo.R.string.format_entryadapter_3, String.valueOf(formatDate(timeFormat, entry.getDeadline()))));
                    holder.tvDeadline.setTextColor(colorTextSecondary);
                } else {
                    holder.tvDeadline.setVisibility(View.GONE);
                }
            } else {
                SimpleDateFormat sdf = entry.hasTime() ? dateTimeFormat : dateFormat;
                String deadlineText = (entry.hasTime() ? "Reminder: " : "Date: ") + formatDate(sdf, entry.getDeadline());
                if (isOverdue) {
                    holder.tvDeadline.setText(getContext().getString(com.ismailmushraf.bujo.R.string.format_entryadapter_2, String.valueOf(deadlineText)));
                    holder.tvDeadline.setTextColor(getContext().getResources().getColor(R.color.bb10_folder_red));
                } else {
                    holder.tvDeadline.setText(deadlineText);
                    holder.tvDeadline.setTextColor(colorTextSecondary);
                }
            }
        } else {
            holder.tvDeadline.setVisibility(View.GONE);
        }

        if (showTags && entry.getProjectTag() != null && !entry.getProjectTag().isEmpty()) {
            holder.tvTag.setVisibility(View.VISIBLE);
            holder.tvTag.setText(getContext().getString(com.ismailmushraf.bujo.R.string.format_entryadapter_1, String.valueOf(entry.getProjectTag())));
        } else {
            holder.tvTag.setVisibility(View.GONE);
        }

        if (holder.ivLock != null) {
            holder.ivLock.setVisibility(entry.isLocked() ? View.VISIBLE : View.GONE);
        }

        return convertView;
    }
}
