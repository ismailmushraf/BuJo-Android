package com.ismailmushraf.bujo.fragments;

import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.ismailmushraf.bujo.MainActivity;
import com.ismailmushraf.bujo.R;
import com.ismailmushraf.bujo.db.DatabaseManager;
import com.ismailmushraf.bujo.models.Entry;
import com.ismailmushraf.bujo.models.Project;
import com.ismailmushraf.bujo.utils.EntryUIHelper;
import com.ismailmushraf.bujo.utils.AppExecutors;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Calendar view with a full-month overview and a compact, actionable day agenda. */
public class FutureLogFragment extends Fragment {

    private com.ismailmushraf.bujo.utils.CalendarGridView monthGrid;
    private GridView weekGrid;
    private View monthControls;
    private View monthSelectedBar;
    private View agendaContainer;
    private TextView monthTitle;
    private TextView selectedDateTitle;
    private TextView monthSelectedDateTitle;
    private TextView weekNumberTitle;
    private ListView agendaList;

    private DatabaseManager dbManager;
    private EntryUIHelper uiHelper;
    private Calendar currentMonth;
    private Calendar selectedDate;
    private List<Entry> deadlineEntries;
    private final List<Entry> displayedEntries = new ArrayList<>();
    private CalendarTaskAdapter calendarTaskAdapter;
    private MonthAdapter monthAdapter;
    private WeekAdapter weekAdapter;
    private int deadlineLoadGeneration;
    private Map<Long, List<Integer>> indicatorColorsByDay = Collections.emptyMap();
    private int selectedDayColor;
    private int selectedDayTextColor;
    private int calendarBackgroundColor;
    private int calendarTextColor;
    private int calendarMutedTextColor;
    private int defaultIndicatorColor;
    private int unfiledIndicatorColor;
    private int indicatorSizePx;
    private int indicatorMarginPx;
    private float transitionDistancePx;
    private int monthTransitionGeneration;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_future_log, container, false);
        monthGrid = root.findViewById(R.id.grid_calendar);
        weekGrid = (GridView) root.findViewById(R.id.grid_week);
        monthControls = root.findViewById(R.id.month_controls);
        monthSelectedBar = root.findViewById(R.id.month_selected_bar);
        agendaContainer = root.findViewById(R.id.agenda_container);
        monthTitle = (TextView) root.findViewById(R.id.tv_calendar_month);
        selectedDateTitle = (TextView) root.findViewById(R.id.tv_selected_date);
        monthSelectedDateTitle = (TextView) root.findViewById(R.id.tv_month_selected_date);
        weekNumberTitle = (TextView) root.findViewById(R.id.tv_week_number);
        agendaList = (ListView) root.findViewById(R.id.list_agenda);

        selectedDayColor = getResources().getColor(R.color.literal_769a30);
        selectedDayTextColor = getResources().getColor(R.color.on_accent);
        calendarBackgroundColor = getResources().getColor(R.color.bujo_background);
        calendarTextColor = getResources().getColor(R.color.bujo_text);
        calendarMutedTextColor = getResources().getColor(R.color.bujo_text_secondary);
        defaultIndicatorColor = getResources().getColor(R.color.bb10_blue);
        unfiledIndicatorColor = getResources().getColor(R.color.bujo_divider);
        float density = getResources().getDisplayMetrics().density;
        indicatorSizePx = (int) (5 * density);
        indicatorMarginPx = (int) (1.5f * density);
        transitionDistancePx = 18 * density;

        dbManager = new DatabaseManager(getActivity());
        dbManager.open();

        uiHelper = new EntryUIHelper(getActivity(), dbManager, new EntryUIHelper.OnEntryUpdatedListener() {
            @Override
            public void onEntryUpdated() {
                refreshCalendarData();
            }
        });

        currentMonth = Calendar.getInstance();
        selectedDate = Calendar.getInstance();

        calendarTaskAdapter = new CalendarTaskAdapter(getActivity(), displayedEntries);
        agendaList.setAdapter(calendarTaskAdapter);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setToolbarTitle("CALENDAR");
            ((MainActivity) getActivity()).setToolbarSubtitle("");
        }

        root.findViewById(R.id.layout_selected_date_banner).setOnClickListener(v -> showMonth());
        monthSelectedBar.setOnClickListener(v -> showAgendaForSelectedDate());

        monthGrid.setOnItemClickListener((parent, view, position, id) -> {
            selectedDate = (Calendar) monthAdapter.getItem(position);
            currentMonth = (Calendar) selectedDate.clone();
            showAgendaForSelectedDate();
        });

        weekGrid.setOnItemClickListener((parent, view, position, id) -> {
            selectedDate = (Calendar) weekAdapter.getItem(position);
            showAgendaForSelectedDate();
        });

        final GestureDetector gestureDetector = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 80;
            private static final int SWIPE_VELOCITY_THRESHOLD = 80;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();

                if (Math.abs(diffY) > Math.abs(diffX)) {
                    if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY < 0) {
                            currentMonth.add(Calendar.MONTH, 1);
                            showMonthWithAnimation(true);
                        } else {
                            currentMonth.add(Calendar.MONTH, -1);
                            showMonthWithAnimation(false);
                        }
                        return true;
                    }
                }
                return false;
            }
        });

        monthGrid.setOnTouchListener((v, event) -> {
            boolean handled = gestureDetector.onTouchEvent(event);
            if (!handled && event.getAction() == MotionEvent.ACTION_UP) v.performClick();
            return handled;
        });

        showMonth();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (agendaContainer != null && agendaContainer.getVisibility() == View.VISIBLE) {
            showAgendaForSelectedDate();
        } else {
            showMonth();
        }
    }

    private void showMonthWithAnimation(boolean slideUp) {
        if (deadlineEntries == null) {
            loadDeadlineEntries(() -> animateMonthChange(slideUp));
        } else {
            animateMonthChange(slideUp);
        }
    }

    private void animateMonthChange(boolean movingForward) {
        if (monthGrid == null || monthGrid.getVisibility() != View.VISIBLE) {
            renderMonth();
            return;
        }

        final int transition = ++monthTransitionGeneration;
        final float outgoingOffset = movingForward ? -transitionDistancePx : transitionDistancePx;
        monthGrid.animate().cancel();
        monthGrid.setAlpha(1f);
        monthGrid.setTranslationY(0f);
        monthGrid.animate()
                .alpha(0f)
                .translationY(outgoingOffset)
                .setDuration(70)
                .withEndAction(() -> {
                    if (!isAdded() || transition != monthTransitionGeneration) return;
                    renderMonth();
                    monthGrid.setTranslationY(-outgoingOffset);
                    monthGrid.setAlpha(0f);
                    monthGrid.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(130)
                            .start();
                })
                .start();
    }

    private void showMonth() {
        if (deadlineEntries == null) {
            loadDeadlineEntries(this::renderMonth);
        } else {
            renderMonth();
        }
    }

    private void renderMonth() {
        boolean returningFromAgenda = agendaContainer.getVisibility() == View.VISIBLE;
        monthControls.setVisibility(View.VISIBLE);
        monthSelectedBar.setVisibility(View.VISIBLE);
        monthGrid.setVisibility(View.VISIBLE);
        agendaContainer.setVisibility(View.GONE);

        monthTitle.setText(new SimpleDateFormat("MMMM yyyy", Locale.US).format(currentMonth.getTime()));
        monthSelectedDateTitle.setText(new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(selectedDate.getTime()));
        weekNumberTitle.setText(getString(com.ismailmushraf.bujo.R.string.format_futurelogfragment_30, String.valueOf(selectedDate.get(Calendar.WEEK_OF_YEAR))));

        if (monthAdapter == null) {
            monthAdapter = new MonthAdapter(getActivity(), currentMonth);
            monthGrid.setAdapter(monthAdapter);
        } else {
            monthAdapter.setMonth(currentMonth);
            monthAdapter.notifyDataSetChanged();
        }

        final MonthAdapter adapter = monthAdapter;
        monthGrid.post(() -> {
            if (monthGrid == null || adapter != monthAdapter) return;
            int totalHeight = monthGrid.getHeight();
            if (totalHeight > 0) {
                int cellHeight = totalHeight / 6;
                if (cellHeight > 0 && adapter.setCellHeight(cellHeight)) {
                    adapter.notifyDataSetChanged();
                }
            }
        });

        if (returningFromAgenda) {
            reveal(monthGrid, -transitionDistancePx);
        }
    }

    private void showAgendaForSelectedDate() {
        if (deadlineEntries == null) {
            loadDeadlineEntries(this::renderAgendaForSelectedDate);
        } else {
            renderAgendaForSelectedDate();
        }
    }

    private void renderAgendaForSelectedDate() {
        boolean expandingFromMonth = monthGrid.getVisibility() == View.VISIBLE;
        monthControls.setVisibility(View.GONE);
        monthSelectedBar.setVisibility(View.GONE);
        monthGrid.setVisibility(View.GONE);
        agendaContainer.setVisibility(View.VISIBLE);

        weekAdapter = new WeekAdapter(getActivity(), selectedDate);
        weekGrid.setAdapter(weekAdapter);

        selectedDateTitle.setText(new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(selectedDate.getTime()));
        TextView tvWeekNumberBanner = getView() != null ? getView().findViewById(R.id.tv_week_number_banner) : null;
        if (tvWeekNumberBanner != null) {
            tvWeekNumberBanner.setText(getString(com.ismailmushraf.bujo.R.string.format_futurelogfragment_29, String.valueOf(selectedDate.get(Calendar.WEEK_OF_YEAR))));
        }

        loadEntriesForSelectedDate();

        if (expandingFromMonth) {
            reveal(agendaContainer, transitionDistancePx);
        }
    }

    private void reveal(View view, float startOffset) {
        view.animate().cancel();
        view.setAlpha(0f);
        view.setTranslationY(startOffset);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(140)
                .start();
    }

    private void loadDeadlineEntries(Runnable onLoaded) {
        final int request = ++deadlineLoadGeneration;
        final android.content.Context context = requireContext().getApplicationContext();
        AppExecutors.getInstance().diskIO().execute(() -> {
            DatabaseManager worker = new DatabaseManager(context);
            CalendarLoadData loaded;
            try {
                worker.open();
                List<Entry> entries = worker.getEntriesWithDeadlines();
                Map<Integer, Integer> projectColors = new HashMap<>();
                for (Project project : worker.getAllProjects()) {
                    projectColors.put(project.getId(), project.getColor() != 0
                            ? project.getColor() : defaultIndicatorColor);
                }

                Map<Long, List<Integer>> indicators = new HashMap<>();
                for (Entry entry : entries) {
                    long day = dayKey(entry.getDeadline());
                    List<Integer> colors = indicators.get(day);
                    if (colors == null) {
                        colors = new ArrayList<>(3);
                        indicators.put(day, colors);
                    }
                    if (colors.size() < 3) {
                        Integer projectColor = projectColors.get(entry.getProjectId());
                        colors.add(entry.getProjectId() == 0
                                ? unfiledIndicatorColor
                                : (projectColor != null ? projectColor : defaultIndicatorColor));
                    }
                }
                loaded = new CalendarLoadData(entries, indicators);
            }
            finally { worker.close(); }
            AppExecutors.getInstance().mainThread().execute(() -> {
                if (!isAdded() || request != deadlineLoadGeneration) return;
                deadlineEntries = loaded.entries;
                indicatorColorsByDay = loaded.indicatorColors;
                onLoaded.run();
            });
        });
    }

    private static long dayKey(long timeInMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeInMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private static final class CalendarLoadData {
        final List<Entry> entries;
        final Map<Long, List<Integer>> indicatorColors;

        CalendarLoadData(List<Entry> entries, Map<Long, List<Integer>> indicatorColors) {
            this.entries = entries;
            this.indicatorColors = indicatorColors;
        }
    }

    private void loadEntriesForSelectedDate() {
        displayedEntries.clear();
        long start = startOfDay(selectedDate);
        long end = endOfDay(selectedDate);
        for (Entry entry : deadlineEntries) {
            if (entry.getDeadline() >= start && entry.getDeadline() <= end) {
                displayedEntries.add(entry);
            }
        }

        // Untimed "Due" tasks on TOP (sorted by createdAt), timed tasks below (sorted by deadline)
        Collections.sort(displayedEntries, (e1, e2) -> {
            boolean t1 = e1.hasTime();
            boolean t2 = e2.hasTime();
            if (!t1 && !t2) {
                return Long.compare(e1.getCreatedAt(), e2.getCreatedAt());
            }
            if (!t1) return -1;
            if (!t2) return 1;
            return Long.compare(e1.getDeadline(), e2.getDeadline());
        });

        calendarTaskAdapter.notifyDataSetChanged();
    }

    private void reloadVisibleEntries() {
        refreshCalendarData();
    }

    private void refreshCalendarData() {
        loadDeadlineEntries(() -> {
            if (agendaContainer != null && agendaContainer.getVisibility() == View.VISIBLE) {
                renderAgendaForSelectedDate();
            } else {
                renderMonth();
            }
        });
    }

    /** Refreshes cached indicators after Calendar becomes visible again. */
    public void refreshAfterNavigation() {
        if (deadlineEntries != null) {
            refreshCalendarData();
        }
    }

    /** Refreshes the visible agenda or month after saving through the task editor. */
    public void refreshFromTaskEditor() {
        if (isAdded() && deadlineEntries != null) {
            refreshCalendarData();
        }
    }

    private long startOfDay(Calendar calendar) {
        Calendar copy = (Calendar) calendar.clone();
        copy.set(Calendar.HOUR_OF_DAY, 0); copy.set(Calendar.MINUTE, 0);
        copy.set(Calendar.SECOND, 0); copy.set(Calendar.MILLISECOND, 0);
        return copy.getTimeInMillis();
    }

    private long endOfDay(Calendar calendar) {
        Calendar copy = (Calendar) calendar.clone();
        copy.set(Calendar.HOUR_OF_DAY, 23); copy.set(Calendar.MINUTE, 59);
        copy.set(Calendar.SECOND, 59); copy.set(Calendar.MILLISECOND, 999);
        return copy.getTimeInMillis();
    }

    private boolean isSameDay(Calendar first, Calendar second) {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                && first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR);
    }

    private class CalendarTaskAdapter extends ArrayAdapter<Entry> {
        private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.US);

        CalendarTaskAdapter(Context context, List<Entry> entries) {
            super(context, 0, entries);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_calendar_task_row, parent, false);
            }
            final Entry entry = getItem(position);
            TextView tvTimeDue = convertView.findViewById(R.id.tv_time_due);
            TextView tvTaskTitle = convertView.findViewById(R.id.tv_task_title);
            TextView tvTaskProject = convertView.findViewById(R.id.tv_task_project);
            TextView tvTaskTick = convertView.findViewById(R.id.tv_task_status_tick);

            if (entry != null) {
                if (entry.hasTime() && entry.getDeadline() > 0) {
                    tvTimeDue.setText(timeFormat.format(new Date(entry.getDeadline())));
                } else {
                    tvTimeDue.setText(getString(com.ismailmushraf.bujo.R.string.ui_due_145caf));
                }

                String content = entry.getContent() != null ? entry.getContent() : "";
                if (entry.isCompleted()) {
                    android.text.SpannableString styledContent = new android.text.SpannableString(content);
                    styledContent.setSpan(new android.text.style.ForegroundColorSpan(
                            getContext().getResources().getColor(R.color.bujo_text_secondary)),
                            0, content.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    styledContent.setSpan(new com.ismailmushraf.bujo.utils.RedStrikethroughLineSpan(
                            getContext().getResources().getColor(R.color.bb10_folder_red)),
                            0, content.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    tvTaskTitle.setPaintFlags(tvTaskTitle.getPaintFlags()
                            & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
                    tvTaskTitle.setText(styledContent);
                } else {
                    tvTaskTitle.setPaintFlags(tvTaskTitle.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
                    tvTaskTitle.setTextColor(getContext().getResources().getColor(R.color.bujo_text));
                    tvTaskTitle.setText(content);
                }

                if (entry.getProjectTag() != null && !entry.getProjectTag().trim().isEmpty()) {
                    tvTaskProject.setVisibility(View.VISIBLE);
                    tvTaskProject.setText(getString(com.ismailmushraf.bujo.R.string.format_futurelogfragment_28, String.valueOf(entry.getProjectTag().trim())));
                } else {
                    tvTaskProject.setVisibility(View.GONE);
                }

                if (entry.isCompleted()) {
                    tvTaskTick.setBackgroundResource(R.drawable.bb10_checkbox_checked_bg);
                    tvTaskTick.setText("✓");
                } else {
                    tvTaskTick.setBackgroundResource(R.drawable.bb10_checkbox_unchecked_bg);
                    tvTaskTick.setText("");
                }

                tvTaskTick.setOnClickListener(v -> {
                    if (uiHelper != null) {
                        uiHelper.toggleEntryCompletion(entry, v);
                        notifyDataSetChanged();
                    }
                });
            }

            convertView.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity && entry != null)
                    ((MainActivity) getActivity()).pushFragment(EditTaskFragment.newInstance(entry.getId()));
            });

            return convertView;
        }
    }

    private class MonthAdapter extends BaseAdapter {
        private final Context context;
        private final List<Calendar> days = new ArrayList<>();
        private int cellHeight = 0;

        MonthAdapter(Context context, Calendar source) {
            this.context = context;
            setMonth(source);
        }

        void setMonth(Calendar source) {
            days.clear();
            Calendar month = (Calendar) source.clone();
            month.set(Calendar.DAY_OF_MONTH, 1);
            month.set(Calendar.HOUR_OF_DAY, 12);
            Calendar first = (Calendar) month.clone();
            int dayOfWeek = first.get(Calendar.DAY_OF_WEEK);
            int daysBeforeMonth = (dayOfWeek == Calendar.SUNDAY) ? 6 : dayOfWeek - Calendar.MONDAY;
            first.add(Calendar.DAY_OF_MONTH, -daysBeforeMonth);

            for (int i = 0; i < 42; i++) {
                Calendar day = (Calendar) first.clone();
                day.add(Calendar.DAY_OF_MONTH, i);
                days.add(day);
            }
        }

        public boolean setCellHeight(int cellHeight) {
            if (this.cellHeight == cellHeight) return false;
            this.cellHeight = cellHeight;
            return true;
        }

        @Override public int getCount() { return days.size(); }
        @Override public Object getItem(int position) { return days.get(position); }
        @Override public long getItemId(int position) { return position; }
        @Override public View getView(int position, View convertView, ViewGroup parent) {
            Calendar day = days.get(position);
            View view = bindDayView(context, day, convertView,
                    day.get(Calendar.MONTH) != currentMonth.get(Calendar.MONTH));
            if (cellHeight > 0) {
                view.setLayoutParams(new android.widget.AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, cellHeight));
            }
            return view;
        }
    }

    private class WeekAdapter extends BaseAdapter {
        private final Context context;
        private final List<Calendar> days = new ArrayList<>();
        WeekAdapter(Context context, Calendar selected) {
            this.context = context;
            Calendar first = (Calendar) selected.clone();
            int dayOfWeek = first.get(Calendar.DAY_OF_WEEK);
            int daysBeforeMonth = (dayOfWeek == Calendar.SUNDAY) ? 6 : dayOfWeek - Calendar.MONDAY;
            first.add(Calendar.DAY_OF_MONTH, -daysBeforeMonth);

            for (int i = 0; i < 7; i++) {
                Calendar day = (Calendar) first.clone();
                day.add(Calendar.DAY_OF_MONTH, i);
                days.add(day);
            }
        }
        @Override public int getCount() { return days.size(); }
        @Override public Object getItem(int position) { return days.get(position); }
        @Override public long getItemId(int position) { return position; }
        @Override public View getView(int position, View convertView, ViewGroup parent) {
            Calendar day = days.get(position);
            View view = bindDayView(context, day, convertView, false);
            int heightPx = (int) (44 * getResources().getDisplayMetrics().density);
            view.setLayoutParams(new android.widget.AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx));
            return view;
        }
    }

    private View bindDayView(Context context, Calendar date, View convertView, boolean muted) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_calendar_day, null, false);
            DayViewHolder holder = new DayViewHolder();
            holder.number = (TextView) convertView.findViewById(R.id.tv_day_number);
            LinearLayout dots = (LinearLayout) convertView.findViewById(R.id.layout_dots);
            for (int i = 0; i < 3; i++) {
                View indicator = new View(context);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(indicatorSizePx, indicatorSizePx);
                params.setMargins(0, indicatorMarginPx, 0, indicatorMarginPx);
                indicator.setLayoutParams(params);
                indicator.setVisibility(View.GONE);
                dots.addView(indicator);
                holder.indicators[i] = indicator;
            }
            convertView.setTag(holder);
        }
        DayViewHolder holder = (DayViewHolder) convertView.getTag();
        TextView number = holder.number;

        boolean selected = isSameDay(date, selectedDate);
        number.setText(String.valueOf(date.get(Calendar.DAY_OF_MONTH)));

        if (selected) {
            convertView.setBackgroundColor(selectedDayColor);
            number.setTextColor(selectedDayTextColor);
        } else if (muted) {
            convertView.setBackgroundColor(calendarBackgroundColor);
            number.setTextColor(calendarMutedTextColor);
        } else {
            convertView.setBackgroundColor(selectedDayTextColor);
            number.setTextColor(calendarTextColor);
        }

        List<Integer> colors = indicatorColorsByDay.get(startOfDay(date));
        int indicatorCount = colors == null ? 0 : colors.size();
        for (int i = 0; i < holder.indicators.length; i++) {
            View indicator = holder.indicators[i];
            if (i < indicatorCount) {
                indicator.setVisibility(View.VISIBLE);
                indicator.setBackgroundColor(selected ? selectedDayTextColor : colors.get(i));
            } else {
                indicator.setVisibility(View.GONE);
            }
        }

        return convertView;
    }

    private static final class DayViewHolder {
        TextView number;
        final View[] indicators = new View[3];
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dbManager != null) dbManager.close();
    }
}
