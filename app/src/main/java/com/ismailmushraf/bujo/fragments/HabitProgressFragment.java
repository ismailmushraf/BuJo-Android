package com.ismailmushraf.bujo.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ismailmushraf.bujo.MainActivity;
import com.ismailmushraf.bujo.R;
import com.ismailmushraf.bujo.db.DatabaseManager;
import com.ismailmushraf.bujo.models.Habit;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HabitProgressFragment extends Fragment {

    private static final String ARG_HABIT_ID = "habit_id";

    private int habitId;
    private DatabaseManager dbManager;

    public static HabitProgressFragment newInstance(int habitId) {
        HabitProgressFragment fragment = new HabitProgressFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_HABIT_ID, habitId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_habit_progress, container, false);

        if (getArguments() != null) {
            habitId = getArguments().getInt(ARG_HABIT_ID);
        }

        dbManager = new DatabaseManager(getActivity());
        dbManager.open();

        Habit habit = null;
        List<Habit> allHabits = dbManager.getAllHabits();
        for (Habit h : allHabits) {
            if (h.getId() == habitId) {
                habit = h;
                break;
            }
        }

        if (habit == null) return root;

        TextView tvName = (TextView) root.findViewById(R.id.tv_progress_habit_name);
        TextView tvSubtitle = (TextView) root.findViewById(R.id.tv_progress_habit_subtitle);
        TextView tvDetails = (TextView) root.findViewById(R.id.tv_progress_habit_details);
        GridView gridView = (GridView) root.findViewById(R.id.gv_habit_progress);

        tvName.setText(habit.getName());

        SimpleDateFormat ymdSdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat displaySdf = new SimpleDateFormat("d MMM", Locale.US);
        String todayStr = ymdSdf.format(new Date());

        int elapsedDays = 1;
        if (habit.getStartDate() != null && !habit.getStartDate().isEmpty()) {
            try {
                Date startDate = ymdSdf.parse(habit.getStartDate());
                Date today = ymdSdf.parse(todayStr);
                long diffMs = today.getTime() - startDate.getTime();
                elapsedDays = (int) (diffMs / (24 * 60 * 60 * 1000L)) + 1;
            } catch (Exception e) {
                elapsedDays = 1;
            }
        }
        elapsedDays = Math.max(1, Math.min(elapsedDays, habit.getCommitmentDays()));
        tvSubtitle.setText("Day " + elapsedDays + " of " + habit.getCommitmentDays());

        int totalCompleted = dbManager.getHabitTotalCompletions(habit.getId());
        String detailsStr = "Started: " + (habit.getStartDate() != null ? habit.getStartDate() : "N/A") + "  |  Total Completed: " + totalCompleted + " days";
        if (habit.hasTime()) {
            SimpleDateFormat tf = new SimpleDateFormat("h:mm a", Locale.US);
            detailsStr += "\nTarget Goal Time: " + tf.format(new Date(habit.getDeadlineTime()));
        }
        tvDetails.setText(detailsStr);

        int totalDays = habit.getCommitmentDays() > 0 ? habit.getCommitmentDays() : 30;

        Calendar startCal = Calendar.getInstance();
        if (habit.getStartDate() != null && !habit.getStartDate().isEmpty()) {
            try {
                startCal.setTime(ymdSdf.parse(habit.getStartDate()));
            } catch (Exception e) {
                startCal = Calendar.getInstance();
            }
        }

        Calendar endCal = (Calendar) startCal.clone();
        endCal.add(Calendar.DAY_OF_YEAR, totalDays - 1);
        String startDateStr = ymdSdf.format(startCal.getTime());
        String endDateStr = ymdSdf.format(endCal.getTime());

        Map<String, Boolean> logsMap = dbManager.getHabitCompletionMap(habit.getId(), startDateStr, endDateStr);

        List<String[]> daysData = new ArrayList<>();
        Calendar currentCal = (Calendar) startCal.clone();
        for (int i = 0; i < totalDays; i++) {
            String dateStr = ymdSdf.format(currentCal.getTime());
            String dateLabel = displaySdf.format(currentCal.getTime());
            daysData.add(new String[]{dateStr, dateLabel, "Day " + (i + 1)});
            currentCal.add(Calendar.DAY_OF_YEAR, 1);
        }

        ArrayAdapter<String[]> gridAdapter = new ArrayAdapter<String[]>(getActivity(), 0, daysData) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    LinearLayout cell = new LinearLayout(getContext());
                    cell.setOrientation(LinearLayout.VERTICAL);
                    cell.setGravity(android.view.Gravity.CENTER);
                    cell.setPadding(12, 16, 12, 16);

                    TextView tvDay = new TextView(getContext());
                    tvDay.setGravity(android.view.Gravity.CENTER);
                    tvDay.setTextSize(10);
                    tvDay.setTextColor(0xFFFFFFFF);

                    TextView tvDate = new TextView(getContext());
                    tvDate.setGravity(android.view.Gravity.CENTER);
                    tvDate.setTextSize(12);
                    tvDate.setTypeface(null, android.graphics.Typeface.BOLD);
                    tvDate.setTextColor(0xFFFFFFFF);

                    cell.addView(tvDay);
                    cell.addView(tvDate);
                    convertView = cell;
                }

                String[] item = getItem(position);
                String dateStr = item[0];
                String dateLabel = item[1];
                String dayLabel = item[2];

                LinearLayout cell = (LinearLayout) convertView;
                TextView tvDay = (TextView) cell.getChildAt(0);
                TextView tvDate = (TextView) cell.getChildAt(1);

                tvDay.setText(dayLabel);
                tvDate.setText(dateLabel);

                boolean isCompleted = (logsMap != null && Boolean.TRUE.equals(logsMap.get(dateStr)));
                boolean isPast = dateStr.compareTo(todayStr) < 0;

                if (isCompleted) {
                    cell.setBackgroundColor(0xFF4CAF50); // Green
                } else if (isPast) {
                    cell.setBackgroundColor(0xFFD32F2F); // Red
                } else {
                    cell.setBackgroundColor(0xFF757575); // Grey (Today pending or Future)
                }

                return convertView;
            }
        };

        gridView.setAdapter(gridAdapter);
        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dbManager != null) dbManager.close();
    }
}
