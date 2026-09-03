package com.ismailmushraf.bujo.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ismailmushraf.bujo.MainActivity;
import com.ismailmushraf.bujo.R;
import com.ismailmushraf.bujo.db.DatabaseManager;
import com.ismailmushraf.bujo.models.Habit;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HabitsFragment extends Fragment {

    private DatabaseManager dbManager;
    private ListView listView;
    private HabitAdapter adapter;
    private TextView tvEmpty;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_habits, container, false);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setToolbarTitle("HABITS");
            ((MainActivity) getActivity()).setToolbarSubtitle("");
        }

        dbManager = new DatabaseManager(getActivity());
        dbManager.open();

        listView = (ListView) root.findViewById(R.id.lv_habits);
        tvEmpty = (TextView) root.findViewById(R.id.tv_empty_habits);
        
        root.findViewById(R.id.btn_add_habit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddHabitDialog();
            }
        });

        loadHabits();

        return root;
    }

    private void loadHabits() {
        List<Habit> habits = dbManager.getAllHabits();
        if (habits.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Calendar cal = Calendar.getInstance();
            String endDate = sdf.format(cal.getTime());
            cal.add(Calendar.DAY_OF_YEAR, -9);
            String startDate = sdf.format(cal.getTime());

            Map<Integer, Integer> counts = dbManager.getAllHabitCompletionCounts();
            Map<Integer, Map<String, Boolean>> logs = new java.util.HashMap<>();
            for (Habit h : habits) {
                logs.put(h.getId(), dbManager.getHabitCompletionMap(h.getId(), startDate, endDate));
            }

            adapter = new HabitAdapter(getActivity(), habits, counts, logs);
            listView.setAdapter(adapter);
        }
    }

    private void showAddHabitDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
        b.setTitle("New Habit Commitment");

        LinearLayout layout = new LinearLayout(getActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);

        final EditText etName = new EditText(getActivity());
        etName.setHint("Habit Name (e.g. Meditate)");
        layout.addView(etName);

        final TextView label = new TextView(getActivity());
        label.setText("Commitment Duration (Days)");
        label.setPadding(0, 20, 0, 0);
        layout.addView(label);

        final EditText etDays = new EditText(getActivity());
        etDays.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etDays.setText("30");
        layout.addView(etDays);

        final Calendar reminderTime = Calendar.getInstance();
        final boolean[] timeSet = {false};
        
        final Button btnTime = new Button(getActivity());
        btnTime.setText("Set Goal Time (Optional)");
        btnTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new android.app.TimePickerDialog(getActivity(), new android.app.TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(android.widget.TimePicker view, int hourOfDay, int minute) {
                        reminderTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        reminderTime.set(Calendar.MINUTE, minute);
                        timeSet[0] = true;
                        btnTime.setText("Goal: " + String.format(Locale.US, "%02d:%02d", hourOfDay, minute));
                    }
                }, 12, 0, false).show();
            }
        });
        layout.addView(btnTime);

        b.setView(layout);
        b.setPositiveButton("Commit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = etName.getText().toString().trim();
                String daysStr = etDays.getText().toString().trim();
                if (!name.isEmpty() && !daysStr.isEmpty()) {
                    Habit h = new Habit();
                    h.setName(name);
                    h.setCommitmentDays(Integer.parseInt(daysStr));
                    h.setStartDate(new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()));
                    if (timeSet[0]) {
                        h.setHasTime(true);
                        h.setDeadlineTime(reminderTime.getTimeInMillis());
                    }
                    dbManager.insertHabit(h);
                    loadHabits();
                }
            }
        });
        b.setNegativeButton(android.R.string.cancel, null);
        b.show();
    }

    private void showEditHabitTimeDialog(final Habit h) {
        final Calendar c = Calendar.getInstance();
        if (h.hasTime()) c.setTimeInMillis(h.getDeadlineTime());

        final android.widget.TimePicker tp = new android.widget.TimePicker(getActivity());
        tp.setCurrentHour(c.get(Calendar.HOUR_OF_DAY));
        tp.setCurrentMinute(c.get(Calendar.MINUTE));

        new AlertDialog.Builder(getActivity())
                .setTitle("Edit Target Time")
                .setView(tp)
                .setPositiveButton("Set Time", (dialog, which) -> {
                    c.set(Calendar.HOUR_OF_DAY, tp.getCurrentHour());
                    c.set(Calendar.MINUTE, tp.getCurrentMinute());
                    h.setHasTime(true);
                    h.setDeadlineTime(c.getTimeInMillis());
                    dbManager.updateHabit(h);
                    loadHabits();
                })
                .setNeutralButton("Clear Time", (dialog, which) -> {
                    h.setHasTime(false);
                    h.setDeadlineTime(0);
                    dbManager.updateHabit(h);
                    loadHabits();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private class HabitAdapter extends ArrayAdapter<Habit> {
        private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        private final String todayStr = sdf.format(new Date());
        private final String[] last10Days = new String[10];
        private final Map<Integer, Integer> completionCounts;
        private final Map<Integer, Map<String, Boolean>> historyLogs;

        public HabitAdapter(Context context, List<Habit> objects, Map<Integer, Integer> counts, Map<Integer, Map<String, Boolean>> logs) {
            super(context, 0, objects);
            this.completionCounts = counts;
            this.historyLogs = logs;
            for (int i = 0; i < 10; i++) {
                Calendar c = Calendar.getInstance();
                c.add(Calendar.DAY_OF_YEAR, -(9 - i));
                last10Days[i] = sdf.format(c.getTime());
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_habit, parent, false);
            }

            final Habit h = getItem(position);
            TextView name = (TextView) convertView.findViewById(R.id.tv_habit_name);
            TextView progressText = (TextView) convertView.findViewById(R.id.tv_habit_progress);
            ProgressBar progress = (ProgressBar) convertView.findViewById(R.id.progress_habit);
            CheckBox cbToday = (CheckBox) convertView.findViewById(R.id.cb_habit_today);
            LinearLayout historyGrid = (LinearLayout) convertView.findViewById(R.id.layout_history_grid);

            name.setText(h.getName());
            
            Integer count = completionCounts.get(h.getId());
            int totalDone = (count == null) ? 0 : count;
            progressText.setText("Day " + totalDone + " of " + h.getCommitmentDays());
            
            int percent = (int)(((float)totalDone / h.getCommitmentDays()) * 100);
            progress.setProgress(Math.min(100, percent));

            cbToday.setOnCheckedChangeListener(null);
            boolean doneToday = dbManager.isHabitCompletedOnDate(h.getId(), todayStr);
            cbToday.setChecked(doneToday);
            cbToday.setAlpha(doneToday ? 0.5f : 1.0f);
            cbToday.setText(doneToday ? "WIN" : "DONE");

            cbToday.setOnCheckedChangeListener((buttonView, isChecked) -> {
                boolean onTime = false;

                if (isChecked) {
                    if (h.hasTime()) {
                        Calendar now = Calendar.getInstance();
                        Calendar deadline = Calendar.getInstance();
                        deadline.setTimeInMillis(h.getDeadlineTime());
                        int nowTime = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
                        int targetTime = deadline.get(Calendar.HOUR_OF_DAY) * 60 + deadline.get(Calendar.MINUTE);
                        if (nowTime <= targetTime) {
                            onTime = true;
                        }
                    }
                }

                int appliedPoints = dbManager.logHabit(h.getId(), todayStr, isChecked, onTime);

                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).animatePointsChange(appliedPoints, buttonView);
                    ((MainActivity) getActivity()).refreshProfileIcon();
                }

                loadHabits();
            });

            // Populate history grid (Last 10 days)
            historyGrid.removeAllViews();
            Map<String, Boolean> habitLogs = historyLogs.get(h.getId());
            for (String dateStr : last10Days) {
                View dot = new View(getContext());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(16, 16);
                lp.setMargins(4, 0, 0, 0);
                dot.setLayoutParams(lp);

                if (habitLogs != null && Boolean.TRUE.equals(habitLogs.get(dateStr))) {
                    dot.setBackgroundColor(0xFF4CAF50); // Green
                } else {
                    dot.setBackgroundColor(0xFFDDDDDD); // Grey
                }
                historyGrid.addView(dot);
            }

            convertView.setOnLongClickListener(v -> {
                String[] options = {"Edit Goal Time", "Delete Habit"};
                new AlertDialog.Builder(getActivity())
                        .setTitle(h.getName())
                        .setItems(options, (dialog, which) -> {
                            if (which == 0) {
                                showEditHabitTimeDialog(h);
                            } else if (which == 1) {
                                new AlertDialog.Builder(getActivity(), R.style.BujoDialog)
                                        .setTitle("Delete Habit")
                                        .setMessage("Are you sure you want to stop tracking this habit?")
                                        .setPositiveButton("Delete", (d, w) -> {
                                            dbManager.deleteHabit(h.getId());
                                            loadHabits();
                                        })
                                        .setNegativeButton(android.R.string.cancel, null)
                                        .show();
                            }
                        })
                        .show();
                return true;
            });

            return convertView;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dbManager.close();
    }
}
