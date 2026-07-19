package com.ismailmushraf.bujo.fragments;

import android.animation.LayoutTransition;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.ismailmushraf.bujo.MainActivity;
import com.ismailmushraf.bujo.R;
import com.ismailmushraf.bujo.adapters.EntryAdapter;
import com.ismailmushraf.bujo.db.DatabaseManager;
import com.ismailmushraf.bujo.models.Entry;
import com.ismailmushraf.bujo.utils.EntryUIHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.List;

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
    private EntryUIHelper uiHelper;
    private Calendar currentMonth;
    private Calendar selectedDate;
    private List<Entry> deadlineEntries;
    private final List<Entry> displayedEntries = new ArrayList<>();
    private EntryAdapter agendaAdapter;
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

        uiHelper = new EntryUIHelper(getActivity(), dbManager, new EntryUIHelper.OnEntryUpdatedListener() {
            @Override
            public void onEntryUpdated() {
                // If a date is changed, reload the entries to reflect accurately on the UI
                if (agendaContainer.getVisibility() == View.VISIBLE) {
                    reloadVisibleEntries();
                } else {
                    showMonth();
                }
            }
        });

        currentMonth = Calendar.getInstance();
        selectedDate = Calendar.getInstance();
        agendaAdapter = new EntryAdapter(getActivity(), displayedEntries);
        agendaList.setAdapter(agendaAdapter);

        // Speed up the automatic animations
        LinearLayout rootLayout = (LinearLayout) root;
        LayoutTransition transition = rootLayout.getLayoutTransition();
        if (transition != null) {
            transition.setDuration(150); // 150ms is incredibly snappy and clean
        }

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setToolbarTitle("CALENDAR");
            ((MainActivity) getActivity()).setToolbarSubtitle("");
        }

        ((Button) root.findViewById(R.id.btn_prev_month)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { currentMonth.add(Calendar.MONTH, -1); showMonth(); }
        });
        ((Button) root.findViewById(R.id.btn_next_month)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { currentMonth.add(Calendar.MONTH, 1); showMonth(); }
        });
        ((Button) root.findViewById(R.id.btn_today)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                currentMonth = Calendar.getInstance();
                selectedDate = Calendar.getInstance();
                showMonth();
            }
        });

        // Changed to use the entire layout container as the touch target
        root.findViewById(R.id.layout_selected_date_banner).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showMonth(); }
        });

        monthGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedDate = (Calendar) monthAdapter.getItem(position);
                currentMonth = (Calendar) selectedDate.clone();
                showAgendaForSelectedDate();
            }
        });
        weekGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedDate = (Calendar) weekAdapter.getItem(position);
                showAgendaForSelectedDate();
            }
        });
        agendaList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Entry entry = displayedEntries.get(position);
                entry.setCompleted(!entry.isCompleted());
                dbManager.updateEntry(entry);
                reloadVisibleEntries();
            }
        });
        agendaList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                uiHelper.showContextDialog(displayedEntries.get(position));
                return true;
            }
        });

        showMonth();
        return root;
    }

    private void showMonth() {
        deadlineEntries = dbManager.getEntriesWithDeadlines();
        monthControls.setVisibility(View.VISIBLE);
        monthSelectedBar.setVisibility(View.VISIBLE);
        monthGrid.setVisibility(View.VISIBLE);
        agendaContainer.setVisibility(View.GONE);
        monthTitle.setText(new SimpleDateFormat("MMMM yyyy", Locale.US)
                .format(currentMonth.getTime()).toUpperCase(Locale.US));
        monthSelectedDateTitle.setText(new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US)
                .format(selectedDate.getTime()));
        weekNumberTitle.setText("Week " + selectedDate.get(Calendar.WEEK_OF_YEAR));
        monthAdapter = new MonthAdapter(getActivity(), currentMonth);
        monthGrid.setAdapter(monthAdapter);
    }

    private void showAgendaForSelectedDate() {
        deadlineEntries = dbManager.getEntriesWithDeadlines();
        monthControls.setVisibility(View.GONE);
        monthSelectedBar.setVisibility(View.GONE);
        monthGrid.setVisibility(View.GONE);
        agendaContainer.setVisibility(View.VISIBLE);
        weekAdapter = new WeekAdapter(getActivity(), selectedDate);
        weekGrid.setAdapter(weekAdapter);
        selectedDateTitle.setText(new SimpleDateFormat("EEEE, MMMM d", Locale.US)
                .format(selectedDate.getTime()).toUpperCase(Locale.US));
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
        agendaAdapter.notifyDataSetChanged();
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

    private class MonthAdapter extends BaseAdapter {
        private final Context context;
        private final List<Calendar> days = new ArrayList<>();

        MonthAdapter(Context context, Calendar source) {
            this.context = context;
            Calendar month = (Calendar) source.clone();
            month.set(Calendar.DAY_OF_MONTH, 1);
            month.set(Calendar.HOUR_OF_DAY, 12);
            Calendar first = (Calendar) month.clone();
            first.add(Calendar.DAY_OF_MONTH, -(first.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY));
            for (int i = 0; i < 42; i++) {
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
            return bindDayView(context, day, convertView,
                    day.get(Calendar.MONTH) != currentMonth.get(Calendar.MONTH));
        }
    }

    private class WeekAdapter extends BaseAdapter {
        private final Context context;
        private final List<Calendar> days = new ArrayList<>();
        WeekAdapter(Context context, Calendar selected) {
            this.context = context;
            Calendar first = (Calendar) selected.clone();
            first.add(Calendar.DAY_OF_MONTH, -(first.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY));
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
        if (convertView == null) convertView = LayoutInflater.from(context)
                .inflate(R.layout.item_calendar_day, null, false);
        TextView number = (TextView) convertView.findViewById(R.id.tv_day_number);
        ViewGroup dots = (ViewGroup) convertView.findViewById(R.id.layout_dots);
        dots.removeAllViews();
        boolean selected = isSameDay(date, selectedDate);
        number.setText(String.valueOf(date.get(Calendar.DAY_OF_MONTH)));
        number.setTextColor(selected ? getResources().getColor(R.color.white)
                : (muted ? getResources().getColor(android.R.color.darker_gray)
                   : getResources().getColor(R.color.bujo_text)));
        int dotCount = 0;
        long start = startOfDay(date), end = endOfDay(date);
        for (Entry entry : deadlineEntries) if (entry.getDeadline() >= start && entry.getDeadline() <= end) dotCount++;
        for (int i = 0; i < Math.min(dotCount, 3); i++) {
            View dot = new View(context);
            dot.setLayoutParams(new ViewGroup.LayoutParams(10, 10));
            dot.setBackgroundResource(R.drawable.shape_bujo_dot_active);
            dots.addView(dot);
        }
        convertView.setBackgroundResource(selected
                ? R.drawable.shape_bujo_calendar_selection : android.R.color.transparent);
        return convertView;
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (dbManager != null) dbManager.close();
    }
}