package com.ismailmushraf.bujo.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ismailmushraf.bujo.MainActivity;
import com.ismailmushraf.bujo.R;
import com.ismailmushraf.bujo.db.DatabaseManager;
import com.ismailmushraf.bujo.models.Project;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class ProfileFragment extends Fragment {

    private DatabaseManager dbManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setToolbarTitle("PROFILE");
            ((MainActivity) getActivity()).setToolbarSubtitle("");
        }

        dbManager = new DatabaseManager(getActivity());
        dbManager.open();

        int[] stats = dbManager.getUserStats();
        int points = stats[0];
        int currentStreak = stats[1];
        int longestStreak = stats[2];
        int tokens = stats[3];
        int taskPts = stats[4];
        int habitPts = stats[5];
        int workoutPts = stats[6];

        ((TextView)root.findViewById(R.id.tv_breakdown_tasks)).setText(taskPts + " pts");
        ((TextView)root.findViewById(R.id.tv_breakdown_habits)).setText(habitPts + " pts");
        ((TextView)root.findViewById(R.id.tv_breakdown_workouts)).setText(workoutPts + " pts");

        final View rankBox = root.findViewById(R.id.layout_rank_box);
        final View breakdownLayout = root.findViewById(R.id.layout_points_breakdown);
        final TextView tvIndicator = root.findViewById(R.id.tv_expand_indicator);

        // Ensure children don't steal clicks
        root.findViewById(R.id.progress_rank).setClickable(false);

        rankBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (breakdownLayout.getVisibility() == View.GONE) {
                    breakdownLayout.setVisibility(View.VISIBLE);
                    tvIndicator.setText("▲ Tap to hide breakdown");
                } else {
                    breakdownLayout.setVisibility(View.GONE);
                    tvIndicator.setText("▼ Tap to see breakdown");
                }
            }
        });

        TextView tvRankIcon = root.findViewById(R.id.tv_rank_icon);
        TextView tvRankTitle = root.findViewById(R.id.tv_rank_title);
        TextView tvProgressText = root.findViewById(R.id.tv_rank_progress_text);
        ProgressBar progressBar = root.findViewById(R.id.progress_rank);

        TextView tvCurrentStreak = root.findViewById(R.id.tv_current_streak);
        TextView tvLongestStreak = root.findViewById(R.id.tv_longest_streak);
        TextView tvRestTokens = root.findViewById(R.id.tv_rest_tokens);

        tvCurrentStreak.setText(currentStreak + (currentStreak == 1 ? " Day" : " Days"));
        tvLongestStreak.setText("Longest Streak: " + longestStreak
                + (longestStreak == 1 ? " Day" : " Days"));
        tvRestTokens.setText(tokens + " / 2");

        // Logic to define ranks
        int minPoints = 0;
        int maxPoints = 500;
        String nextRank = "Apprentice";

        if (points < 500) {
            tvRankIcon.setText("🚶");
            tvRankTitle.setText("LEVEL 1: WANDERER");
            maxPoints = 500;
            nextRank = "Apprentice";
        } else if (points < 2000) {
            tvRankIcon.setText("📖");
            tvRankTitle.setText("LEVEL 2: APPRENTICE");
            minPoints = 500;
            maxPoints = 2000;
            nextRank = "Scholar";
        } else if (points < 5000) {
            tvRankIcon.setText("🎓");
            tvRankTitle.setText("LEVEL 3: SCHOLAR");
            minPoints = 2000;
            maxPoints = 5000;
            nextRank = "Ascendant";
        } else if (points < 10000) {
            tvRankIcon.setText("⛰️");
            tvRankTitle.setText("LEVEL 4: ASCENDANT");
            minPoints = 5000;
            maxPoints = 10000;
            nextRank = "Monk";
        } else {
            tvRankIcon.setText("⛩️");
            tvRankTitle.setText("LEVEL 5: MONK");
            minPoints = 10000;
            maxPoints = 10000;
            nextRank = "Maximum Discipline Reached";
        }

        if (points >= 10000) {
            tvProgressText.setText(points + " Total Discipline XP");
            progressBar.setProgress(100);
        } else {
            tvProgressText.setText(points + " / " + maxPoints + " XP to " + nextRank);
            int progressPercent = (int) (((float) (points - minPoints) / (maxPoints - minPoints)) * 100);
            progressBar.setProgress(progressPercent);
        }

        // Habit Discipline Logic
        TextView tvPerfectDays = root.findViewById(R.id.tv_habit_perfect_days);
        TextView tvCompletionRate = root.findViewById(R.id.tv_habit_completion_rate);

        // Set defaults
        tvPerfectDays.setText("0");
        tvCompletionRate.setText("0%");
        
        List<com.ismailmushraf.bujo.models.Habit> habits = dbManager.getAllHabits();
        if (!habits.isEmpty()) {
            int perfectDays = 0;
            float totalCompletionSum = 0;
            int daysWithActiveHabits = 0;
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            
            // 1. Calculate date range
            Calendar calRange = Calendar.getInstance();
            String endDate = sdf.format(calRange.getTime());
            calRange.add(Calendar.DAY_OF_YEAR, -29);
            String startDate = sdf.format(calRange.getTime());

            // 2. Pre-fetch ALL logs for ALL habits in the 30-day range
            // Map<HabitID, Map<DateStr, Completed>>
            java.util.Map<Integer, java.util.Map<String, Boolean>> allLogs = new java.util.HashMap<>();
            for (com.ismailmushraf.bujo.models.Habit h : habits) {
                allLogs.put(h.getId(), dbManager.getHabitCompletionMap(h.getId(), startDate, endDate));
            }

            // 3. Analyze each day
            for (int i = 0; i < 30; i++) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_YEAR, -i);
                String d = sdf.format(cal.getTime());
                
                int activeHabitsOnDate = 0;
                int completedToday = 0;
                
                for (com.ismailmushraf.bujo.models.Habit h : habits) {
                    if (d.compareTo(h.getStartDate()) >= 0) {
                        activeHabitsOnDate++;
                        java.util.Map<String, Boolean> habitLogs = allLogs.get(h.getId());
                        if (habitLogs != null && Boolean.TRUE.equals(habitLogs.get(d))) {
                            completedToday++;
                        }
                    }
                }
                
                if (activeHabitsOnDate > 0) {
                    daysWithActiveHabits++;
                    if (completedToday == activeHabitsOnDate) perfectDays++;
                    totalCompletionSum += (float)completedToday / activeHabitsOnDate;
                }
            }
            
            tvPerfectDays.setText(String.valueOf(perfectDays));
            int avgRate = 0;
            if (daysWithActiveHabits > 0) {
                avgRate = (int)((totalCompletionSum / daysWithActiveHabits) * 100);
            }
            tvCompletionRate.setText(avgRate + "%");
        }

        LinearLayout goalLayout = (LinearLayout) root.findViewById(R.id.layout_goal_efficiency);
        List<Project> projects = dbManager.getAllProjects();
        
        LayoutInflater inf = LayoutInflater.from(getActivity());
        for (Project p : projects) {
            View itemView = inf.inflate(R.layout.item_goal_efficiency, goalLayout, false);
            
            TextView name = (TextView) itemView.findViewById(R.id.tv_project_name);
            TextView weight = (TextView) itemView.findViewById(R.id.tv_project_weight);
            ProgressBar effProgress = (ProgressBar) itemView.findViewById(R.id.progress_efficiency);
            TextView effText = (TextView) itemView.findViewById(R.id.tv_efficiency_percent);
            
            name.setText(p.getName());
            
            StringBuilder stars = new StringBuilder();
            for (int i = 0; i < p.getWeight(); i++) stars.append("★");
            weight.setText(stars.toString());
            
            float efficiency = dbManager.getProjectEfficiency(p.getId());
            int effInt = (int)(efficiency * 100);
            effProgress.setProgress(effInt);
            effText.setText(effInt + "% Efficiency");
            
            goalLayout.addView(itemView);
        }

        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dbManager.close();
    }
}
