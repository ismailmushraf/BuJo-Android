package com.ismailmushraf.bujo.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.ismailmushraf.bujo.MainActivity;
import com.ismailmushraf.bujo.R;
import com.ismailmushraf.bujo.db.DatabaseManager;
import com.ismailmushraf.bujo.models.Habit;
import com.ismailmushraf.bujo.utils.AppExecutors;

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
    private int loadGeneration;

    private static final class HabitLoadData {
        final List<Habit> habits;
        final Map<Integer, Integer> counts;
        final Map<Integer, Map<String, Boolean>> logs;
        HabitLoadData(List<Habit> habits, Map<Integer, Integer> counts, Map<Integer, Map<String, Boolean>> logs) {
            this.habits = habits; this.counts = counts; this.logs = logs;
        }
    }

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
        final EditText etNewHabit = (EditText) root.findViewById(R.id.et_new_habit);

        if (etNewHabit != null) {
            etNewHabit.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE || actionId == 0) {
                    processNewHabit(etNewHabit);
                    return true;
                }
                return false;
            });

            etNewHabit.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN &&
                        (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER || keyCode == KeyEvent.KEYCODE_PLUS)) {
                    processNewHabit(etNewHabit);
                    return true;
                }
                return false;
            });
        }

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (adapter != null && position >= 0 && position < adapter.getCount()) {
                Habit h = adapter.getItem(position);
                if (h != null) {
                    toggleHabitToday(h, view);
                }
            }
        });

        loadHabits();

        return root;
    }

    private void processNewHabit(EditText etNewHabit) {
        String name = etNewHabit.getText().toString().trim();
        if (!name.isEmpty()) {
            Habit h = new Habit();
            h.setName(name);
            h.setCommitmentDays(30);
            h.setStartDate(new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()));
            dbManager.insertHabit(h);
            loadHabits();
            etNewHabit.setText("");
        }
    }

    private void toggleHabitToday(Habit h, View view) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String todayStr = sdf.format(new Date());
        boolean currentlyCompleted = dbManager.isHabitCompletedOnDate(h.getId(), todayStr);
        boolean newCompleted = !currentlyCompleted;

        boolean onTime = false;
        if (newCompleted && h.hasTime()) {
            Calendar now = Calendar.getInstance();
            Calendar deadline = Calendar.getInstance();
            deadline.setTimeInMillis(h.getDeadlineTime());
            int nowTime = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
            int targetTime = deadline.get(Calendar.HOUR_OF_DAY) * 60 + deadline.get(Calendar.MINUTE);
            if (nowTime <= targetTime) {
                onTime = true;
            }
        }

        int appliedPoints = dbManager.logHabit(h.getId(), todayStr, newCompleted, onTime);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).animatePointsChange(appliedPoints, view);
            ((MainActivity) getActivity()).refreshProfileIcon();
        }

        loadHabits();
    }

    private void loadHabits() {
        final int request = ++loadGeneration;
        final String[] range = habitHistoryRange();
        final android.content.Context context = requireContext().getApplicationContext();
        AppExecutors.getInstance().diskIO().execute(() -> {
            DatabaseManager worker = new DatabaseManager(context);
            HabitLoadData loaded;
            try {
                worker.open();
                List<Habit> habits = worker.getAllHabits();
                Map<Integer, Integer> counts = worker.getAllHabitCompletionCounts();
                Map<Integer, Map<String, Boolean>> logs = new java.util.HashMap<>();
                for (Habit h : habits) logs.put(h.getId(), worker.getHabitCompletionMap(h.getId(), range[0], range[1]));
                loaded = new HabitLoadData(habits, counts, logs);
            } finally { worker.close(); }
            AppExecutors.getInstance().mainThread().execute(() -> {
                if (!isAdded() || request != loadGeneration) return;
                applyHabits(loaded);
            });
        });
    }

    private String[] habitHistoryRange() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar cal = Calendar.getInstance();
        String endDate = sdf.format(cal.getTime());
        cal.add(Calendar.DAY_OF_YEAR, -9);
        return new String[]{sdf.format(cal.getTime()), endDate};
    }

    private void applyHabits(HabitLoadData loaded) {
        if (loaded.habits.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
            adapter = new HabitAdapter(getActivity(), loaded.habits, loaded.counts, loaded.logs);
            listView.setAdapter(adapter);
        }
    }

    public void showAddHabitDialog() {
        if (getActivity() == null) return;

        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_add_habit, null);
        final EditText etName = view.findViewById(R.id.et_habit_name);
        final EditText etDays = view.findViewById(R.id.et_habit_days);
        final TextView btnTime = view.findViewById(R.id.btn_habit_time);

        final Calendar reminderTime = Calendar.getInstance();
        final boolean[] timeSet = {false};

        btnTime.setOnClickListener(v -> {
            new android.app.TimePickerDialog(getActivity(), (timePicker, hourOfDay, minute) -> {
                reminderTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                reminderTime.set(Calendar.MINUTE, minute);
                timeSet[0] = true;
                btnTime.setText(getString(com.ismailmushraf.bujo.R.string.format_habitsfragment_27,
                        String.valueOf(String.format(Locale.US, "%02d:%02d", hourOfDay, minute))));
            }, 12, 0, false).show();
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.BujoDialog);
        builder.setView(view);
        final AlertDialog dialog = builder.create();

        view.findViewById(R.id.btn_habit_cancel).setOnClickListener(v -> dialog.dismiss());

        view.findViewById(R.id.btn_habit_commit).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String daysStr = etDays.getText().toString().trim();
            if (!name.isEmpty() && !daysStr.isEmpty()) {
                dialog.dismiss();
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
        });

        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
                int width = (int) (metrics.widthPixels * 0.88);
                dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        });

        dialog.show();
    }

    private void showEditHabitTimeDialog(final Habit h) {
        final Calendar c = Calendar.getInstance();
        if (h.hasTime()) c.setTimeInMillis(h.getDeadlineTime());

        final android.widget.TimePicker tp = new android.widget.TimePicker(getActivity());
        tp.setCurrentHour(c.get(Calendar.HOUR_OF_DAY));
        tp.setCurrentMinute(c.get(Calendar.MINUTE));

        new AlertDialog.Builder(getActivity(), R.style.BujoDialog)
                .setTitle(getString(com.ismailmushraf.bujo.R.string.ui_edit_target_time_83b4a4))
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

    private void openHabitProgressPage(int habitId) {
        HabitProgressFragment fragment = HabitProgressFragment.newInstance(habitId);
        if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).pushFragment(fragment);
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
            TextView timeText = (TextView) convertView.findViewById(R.id.tv_habit_time);
            LinearLayout historyGrid = (LinearLayout) convertView.findViewById(R.id.layout_history_grid);

            name.setText(h.getName());

            // Calculate elapsed calendar days since startDate
            int elapsedDays = 1;
            if (h.getStartDate() != null && !h.getStartDate().isEmpty()) {
                try {
                    Date startDate = sdf.parse(h.getStartDate());
                    Date today = sdf.parse(todayStr);
                    long diffMs = today.getTime() - startDate.getTime();
                    elapsedDays = (int) (diffMs / (24 * 60 * 60 * 1000L)) + 1;
                } catch (Exception e) {
                    elapsedDays = 1;
                }
            }
            elapsedDays = Math.max(1, Math.min(elapsedDays, h.getCommitmentDays()));
            progressText.setText(getString(com.ismailmushraf.bujo.R.string.format_habitsfragment_26, String.valueOf(elapsedDays), String.valueOf(h.getCommitmentDays())));

            if (h.hasTime()) {
                timeText.setVisibility(View.VISIBLE);
                SimpleDateFormat tf = new SimpleDateFormat("h:mm a", Locale.US);
                timeText.setText(getString(com.ismailmushraf.bujo.R.string.format_habitsfragment_25, String.valueOf(tf.format(new Date(h.getDeadlineTime())))));
            } else {
                timeText.setVisibility(View.GONE);
            }

            // Populate 10-day history graph dots
            historyGrid.removeAllViews();
            Map<String, Boolean> habitLogs = historyLogs.get(h.getId());
            String habitStartDate = h.getStartDate();

            int dotSize = (int) (10 * getContext().getResources().getDisplayMetrics().density);
            int dotMargin = (int) (3 * getContext().getResources().getDisplayMetrics().density);

            for (String dateStr : last10Days) {
                View dot = new View(getContext());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dotSize, dotSize);
                lp.setMargins(dotMargin, 0, 0, 0);
                dot.setLayoutParams(lp);

                boolean isCompleted = (habitLogs != null && Boolean.TRUE.equals(habitLogs.get(dateStr)));
                boolean isBeforeStart = (habitStartDate != null && dateStr.compareTo(habitStartDate) < 0);
                boolean isToday = dateStr.equals(todayStr);

                if (isCompleted) {
                    dot.setBackgroundColor(getResources().getColor(R.color.points_positive)); // Green
                } else if (isBeforeStart) {
                    dot.setBackgroundColor(getResources().getColor(R.color.bujo_divider)); // Grey (habit didn't exist yet)
                } else if (!isToday) {
                    // Missed past day -> RED
                    dot.setBackgroundColor(getResources().getColor(R.color.bb10_folder_red)); // Red
                } else {
                    // Today, pending -> Grey
                    dot.setBackgroundColor(getResources().getColor(R.color.bujo_divider)); // Grey
                }
                historyGrid.addView(dot);
            }

            // Click listener on habit item row to toggle today's completion
            convertView.setOnClickListener(v -> {
                toggleHabitToday(h, v);
            });

            convertView.setOnLongClickListener(v -> {
                final android.app.Dialog dialog = new android.app.Dialog(getActivity(), android.R.style.Theme_Translucent_NoTitleBar);
                View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_bb10_context_sidebar, null);
                dialog.setContentView(view);

                if (dialog.getWindow() != null) {
                    android.view.WindowManager.LayoutParams lp = new android.view.WindowManager.LayoutParams();
                    lp.copyFrom(dialog.getWindow().getAttributes());
                    lp.width = android.view.WindowManager.LayoutParams.MATCH_PARENT;
                    lp.height = android.view.WindowManager.LayoutParams.MATCH_PARENT;
                    lp.gravity = android.view.Gravity.END;
                    lp.windowAnimations = R.style.BB10SidebarAnimation;
                    dialog.getWindow().setAttributes(lp);
                }

                view.findViewById(R.id.sidebar_dim_scrim).setOnClickListener(scrimView -> dialog.dismiss());

                TextView tvTitle = (TextView) view.findViewById(R.id.sidebar_task_title);
                tvTitle.setText(h.getName());

                ListView lvOptions = (ListView) view.findViewById(R.id.lv_sidebar_options);
                final List<String> optionsList = new java.util.ArrayList<>();
                optionsList.add("Progress Graph");
                optionsList.add("Edit Target Time");

                ArrayAdapter<String> adapterOptions = new ArrayAdapter<String>(getActivity(), R.layout.item_sidebar_option, R.id.tv_option_title, optionsList) {
                    @Override
                    public View getView(int position, View convertView1, ViewGroup parentGroup) {
                        if (convertView1 == null) {
                            convertView1 = LayoutInflater.from(getContext()).inflate(R.layout.item_sidebar_option, parentGroup, false);
                        }
                        TextView tv = (TextView) convertView1.findViewById(R.id.tv_option_title);
                        android.widget.ImageView iv = (android.widget.ImageView) convertView1.findViewById(R.id.iv_option_icon);
                        tv.setText(optionsList.get(position));
                        if (position == 0) {
                            iv.setImageResource(R.drawable.ic_bb10_graph);
                        } else {
                            iv.setImageResource(R.drawable.ic_bb10_compose);
                        }
                        return convertView1;
                    }
                };
                lvOptions.setAdapter(adapterOptions);

                lvOptions.setOnItemClickListener((parentAdapter, view1, pos, id) -> {
                    dialog.dismiss();
                    if (pos == 0) {
                        openHabitProgressPage(h.getId());
                    } else if (pos == 1) {
                        showEditHabitTimeDialog(h);
                    }
                });

                view.findViewById(R.id.sidebar_bottom_delete).setOnClickListener(deleteView -> {
                    dialog.dismiss();
                    com.ismailmushraf.bujo.utils.BB10DialogHelper.showConfirmDialog(getActivity(), "Delete Habit", "Are you sure you want to delete '" + h.getName() + "' and its history logs?", "Delete", () -> {
                        dbManager.deleteHabit(h.getId());
                        loadHabits();
                    });
                });

                dialog.show();
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
