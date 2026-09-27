package com.ismailmushraf.bujo.fragments;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.widget.AppCompatImageView;

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
            ((MainActivity) getActivity()).setToolbarTitle(getString(R.string.profile_title));
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

        ((TextView)root.findViewById(R.id.tv_breakdown_tasks)).setText(getString(com.ismailmushraf.bujo.R.string.format_profilefragment_24, String.valueOf(taskPts)));
        ((TextView)root.findViewById(R.id.tv_breakdown_habits)).setText(getString(com.ismailmushraf.bujo.R.string.format_profilefragment_23, String.valueOf(habitPts)));
        ((TextView)root.findViewById(R.id.tv_breakdown_workouts)).setText(getString(com.ismailmushraf.bujo.R.string.format_profilefragment_22, String.valueOf(workoutPts)));

        final View rankBox = root.findViewById(R.id.layout_rank_box);
        final View breakdownLayout = root.findViewById(R.id.layout_points_breakdown);
        final AppCompatImageView ivChevron = root.findViewById(R.id.iv_expand_chevron);
        final ViewGroup rootContainer = root.findViewById(R.id.profile_root_layout);

        // Ensure children don't steal clicks
        root.findViewById(R.id.progress_rank).setClickable(false);

        rankBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (rootContainer != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    android.transition.TransitionManager.beginDelayedTransition(rootContainer);
                }
                if (breakdownLayout.getVisibility() == View.GONE) {
                    breakdownLayout.setVisibility(View.VISIBLE);
                    if (ivChevron != null) {
                        ivChevron.setImageResource(R.drawable.ic_bb10_chevron_up);
                    }
                } else {
                    breakdownLayout.setVisibility(View.GONE);
                    if (ivChevron != null) {
                        ivChevron.setImageResource(R.drawable.ic_bb10_chevron_down);
                    }
                }
            }
        });

        AppCompatImageView tvRankIcon = root.findViewById(R.id.tv_rank_icon);
        TextView tvRankTitle = root.findViewById(R.id.tv_rank_title);
        TextView tvProgressText = root.findViewById(R.id.tv_rank_progress_text);
        ProgressBar progressBar = root.findViewById(R.id.progress_rank);

        TextView tvCurrentStreak = root.findViewById(R.id.tv_current_streak);
        TextView tvLongestStreak = root.findViewById(R.id.tv_longest_streak);
        TextView tvRestTokens = root.findViewById(R.id.tv_rest_tokens);

        tvCurrentStreak.setText(getResources().getQuantityString(R.plurals.streak_days, currentStreak, currentStreak));
        tvLongestStreak.setText(getString(R.string.longest_streak,
                getResources().getQuantityString(R.plurals.streak_days, longestStreak, longestStreak)));
        tvRestTokens.setText(getString(com.ismailmushraf.bujo.R.string.format_profilefragment_20, String.valueOf(tokens)));

        // Logic to define ranks
        int minPoints = 0;
        int maxPoints = 500;
        String nextRank = getString(R.string.profile_rank_apprentice);

        if (points < 500) {
            tvRankIcon.setImageResource(R.drawable.ic_bb10_profile_wanderer);
            tvRankTitle.setText(getString(com.ismailmushraf.bujo.R.string.ui_level_1_wanderer_6d5da1));
            maxPoints = 500;
            nextRank = getString(R.string.profile_rank_apprentice);
        } else if (points < 2000) {
            tvRankIcon.setImageResource(R.drawable.ic_bb10_profile_book);
            tvRankTitle.setText(getString(com.ismailmushraf.bujo.R.string.ui_level_2_apprentice_ba52e3));
            minPoints = 500;
            maxPoints = 2000;
            nextRank = getString(R.string.profile_rank_scholar);
        } else if (points < 5000) {
            tvRankIcon.setImageResource(R.drawable.ic_bb10_profile_scholar);
            tvRankTitle.setText(getString(com.ismailmushraf.bujo.R.string.ui_level_3_scholar_2c2c6c));
            minPoints = 2000;
            maxPoints = 5000;
            nextRank = getString(R.string.profile_rank_ascendant);
        } else if (points < 10000) {
            tvRankIcon.setImageResource(R.drawable.ic_bb10_profile_mountain);
            tvRankTitle.setText(getString(com.ismailmushraf.bujo.R.string.ui_level_4_ascendant_6153be));
            minPoints = 5000;
            maxPoints = 10000;
            nextRank = getString(R.string.profile_rank_monk);
        } else {
            tvRankIcon.setImageResource(R.drawable.ic_bb10_profile_temple);
            tvRankTitle.setText(getString(com.ismailmushraf.bujo.R.string.ui_level_5_monk_e0c1bb));
            minPoints = 10000;
            maxPoints = 10000;
        }

        if (points >= 10000) {
            tvProgressText.setText(getString(com.ismailmushraf.bujo.R.string.format_profilefragment_19, String.valueOf(points)));
            progressBar.setProgress(100);
        } else {
            tvProgressText.setText(getString(com.ismailmushraf.bujo.R.string.format_profilefragment_18, String.valueOf(points), String.valueOf(maxPoints), String.valueOf(nextRank)));
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
            tvCompletionRate.setText(getString(com.ismailmushraf.bujo.R.string.format_profilefragment_17, String.valueOf(avgRate)));
        }

        LinearLayout goalLayout = (LinearLayout) root.findViewById(R.id.layout_goal_efficiency);
        List<Project> projects = dbManager.getAllProjects();
        root.findViewById(R.id.profile_empty_goals).setVisibility(projects.isEmpty() ? View.VISIBLE : View.GONE);
        
        LayoutInflater inf = LayoutInflater.from(getActivity());
        for (Project p : projects) {
            View itemView = inf.inflate(R.layout.item_goal_efficiency, goalLayout, false);
            
            TextView name = (TextView) itemView.findViewById(R.id.tv_project_name);
            TextView weight = (TextView) itemView.findViewById(R.id.tv_project_weight);
            ProgressBar effProgress = (ProgressBar) itemView.findViewById(R.id.progress_efficiency);
            TextView effText = (TextView) itemView.findViewById(R.id.tv_efficiency_percent);
            
            name.setText(p.getName());
            
            weight.setText(getString(R.string.profile_goal_weight, p.getWeight()));
            
            float efficiency = dbManager.getProjectEfficiency(p.getId());
            int effInt = (int)(efficiency * 100);
            effProgress.setProgress(effInt);
            effText.setText(getString(com.ismailmushraf.bujo.R.string.format_profilefragment_16, String.valueOf(effInt)));
            
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
