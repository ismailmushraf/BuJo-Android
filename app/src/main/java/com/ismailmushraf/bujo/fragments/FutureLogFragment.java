package com.ismailmushraf.bujo.fragments;

import android.animation.LayoutTransition;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
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

    private GridView monthGrid;
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
    private Calendar currentMonth;
    private Calendar selectedDate;
    private List<Entry> deadlineEntries;
    private final List<Entry> displayedEntries = new ArrayList<>();
    private CalendarTaskAdapter calendarTaskAdapter;
    private MonthAdapter monthAdapter;
    private WeekAdapter weekAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_future_log, container, false);
        monthGrid = (GridView) root.findViewById(R.id.grid_calendar);
        weekGrid = (GridView) root.findViewById(R.id.grid_week);
        monthControls = root.findViewById(R.id.month_controls);
        monthSelectedBar = root.findViewById(R.id.month_selected_bar);
        agendaContainer = root.findViewById(R.id.agenda_container);
        monthTitle = (TextView) root.findViewById(R.id.tv_calendar_month);
        selectedDateTitle = (TextView) root.findViewById(R.id.tv_selected_date);
        monthSelectedDateTitle = (TextView) root.findViewById(R.id.tv_month_selected_date);
        weekNumberTitle = (TextView) root.findViewById(R.id.tv_week_number);
        agendaList = (ListView) root.findViewById(R.id.list_agenda);

        dbManager = new DatabaseManager(getActivity());
        dbManager.open();

        currentMonth = Calendar.getInstance();
        selectedDate = Calendar.getInstance();

        calendarTaskAdapter = new CalendarTaskAdapter(getActivity(), displayedEntries);
        agendaList.setAdapter(calendarTaskAdapter);

        LinearLayout rootLayout = (LinearLayout) root;
        LayoutTransition transition = rootLayout.getLayoutTransition();
        if (transition != null) {
            transition.setDuration(150);
        }

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
                            showMonth();
                        } else {
                            currentMonth.add(Calendar.MONTH, -1);
                            showMonth();
                        }
                        return true;
                    }
                }
                return false;
            }
        });

        monthGrid.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));

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

    private void showMonth() {
        deadlineEntries = dbManager.getEntriesWithDeadlines();
        monthControls.setVisibility(View.VISIBLE);
        monthSelectedBar.setVisibility(View.VISIBLE);
        monthGrid.setVisibility(View.VISIBLE);
        agendaContainer.setVisibility(View.GONE);

        monthTitle.setText(new SimpleDateFormat("MMMM yyyy", Locale.US).format(currentMonth.getTime()));
        monthSelectedDateTitle.setText(new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(selectedDate.getTime()));
        weekNumberTitle.setText("Week " + selectedDate.get(Calendar.WEEK_OF_YEAR));

        monthAdapter = new MonthAdapter(getActivity(), currentMonth);
        monthGrid.setAdapter(monthAdapter);

        monthGrid.post(() -> {
            if (monthGrid == null) return;
            int totalHeight = monthGrid.getHeight();
            if (totalHeight > 0) {
                int cellHeight = totalHeight / 6;
                if (cellHeight > 0 && monthAdapter != null) {
                    monthAdapter.setCellHeight(cellHeight);
                    monthAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    private void showAgendaForSelectedDate() {
        deadlineEntries = dbManager.getEntriesWithDeadlines();
        monthControls.setVisibility(View.GONE);
        monthSelectedBar.setVisibility(View.GONE);
        monthGrid.setVisibility(View.GONE);
        agendaContainer.setVisibility(View.VISIBLE);

        weekAdapter = new WeekAdapter(getActivity(), selectedDate);
        weekGrid.setAdapter(weekAdapter);

        selectedDateTitle.setText(new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(selectedDate.getTime()));
        TextView tvWeekNumberBanner = getView() != null ? getView().findViewById(R.id.tv_week_number_banner) : null;
        if (tvWeekNumberBanner != null) {
            tvWeekNumberBanner.setText("Week " + selectedDate.get(Calendar.WEEK_OF_YEAR));
        }

        loadEntriesForSelectedDate();
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
        showAgendaForSelectedDate();
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
            Entry entry = getItem(position);
            TextView tvTimeDue = convertView.findViewById(R.id.tv_time_due);
            TextView tvTaskTitle = convertView.findViewById(R.id.tv_task_title);

            if (entry != null) {
                if (entry.hasTime() && entry.getDeadline() > 0) {
                    tvTimeDue.setText(timeFormat.format(new Date(entry.getDeadline())));
                } else {
                    tvTimeDue.setText("Due");
                }
                tvTaskTitle.setText(entry.getContent() != null ? entry.getContent() : "");
            }

            convertView.setOnClickListener(v -> {
                if (getFragmentManager() != null && entry != null) {
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right);
                    ft.replace(R.id.fragment_container, EditTaskFragment.newInstance(entry.getId()));
                    ft.addToBackStack(null);
                    ft.commit();
                }
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

        public void setCellHeight(int cellHeight) {
            this.cellHeight = cellHeight;
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
            return bindDayView(context, day, convertView, false);
        }
    }

    private View bindDayView(Context context, Calendar date, View convertView, boolean muted) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_calendar_day, null, false);
        }
        TextView number = (TextView) convertView.findViewById(R.id.tv_day_number);
        ViewGroup dots = (ViewGroup) convertView.findViewById(R.id.layout_dots);
        dots.removeAllViews();

        boolean selected = isSameDay(date, selectedDate);
        number.setText(String.valueOf(date.get(Calendar.DAY_OF_MONTH)));

        if (selected) {
            convertView.setBackgroundColor(Color.parseColor("#769A30"));
            number.setTextColor(Color.WHITE);
        } else if (muted) {
            convertView.setBackgroundColor(Color.parseColor("#F4F4F4"));
            number.setTextColor(Color.parseColor("#777777"));
        } else {
            convertView.setBackgroundColor(Color.WHITE);
            number.setTextColor(Color.parseColor("#222222"));
        }

        long start = startOfDay(date);
        long end = endOfDay(date);
        List<Entry> dayEntries = new ArrayList<>();
        if (deadlineEntries != null) {
            for (Entry entry : deadlineEntries) {
                if (entry.getDeadline() >= start && entry.getDeadline() <= end) {
                    dayEntries.add(entry);
                }
            }
        }

        List<Project> allProjects = dbManager != null ? dbManager.getAllProjects() : new ArrayList<>();
        Map<Integer, Integer> projectColorMap = new HashMap<>();
        for (Project p : allProjects) {
            projectColorMap.put(p.getId(), p.getColor() != 0 ? p.getColor() : Color.parseColor("#00a8df"));
        }

        int maxIndicators = Math.min(dayEntries.size(), 3);
        int squarePx = (int) (5 * getResources().getDisplayMetrics().density);
        int marginPx = (int) (1.5f * getResources().getDisplayMetrics().density);

        for (int i = 0; i < maxIndicators; i++) {
            Entry e = dayEntries.get(i);
            View square = new View(context);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(squarePx, squarePx);
            lp.setMargins(0, marginPx, 0, marginPx);
            square.setLayoutParams(lp);

            Integer mappedColor = projectColorMap.get(e.getProjectId());
            int color = (mappedColor != null) ? mappedColor : Color.parseColor("#00a8df");
            square.setBackgroundColor(selected ? Color.WHITE : color);
            dots.addView(square);
        }

        return convertView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dbManager != null) dbManager.close();
    }
}
