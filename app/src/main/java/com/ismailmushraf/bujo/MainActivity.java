package com.ismailmushraf.bujo;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

import com.ismailmushraf.bujo.adapters.DrawerAdapter;
import com.ismailmushraf.bujo.db.DatabaseManager;
import com.ismailmushraf.bujo.fragments.DailyLogFragment;
import com.ismailmushraf.bujo.fragments.FutureLogFragment;
import com.ismailmushraf.bujo.fragments.InboxFragment;
import com.ismailmushraf.bujo.fragments.MigratedItemsFragment;
import com.ismailmushraf.bujo.fragments.ProjectDetailFragment;
import com.ismailmushraf.bujo.fragments.SettingsFragment;
import com.ismailmushraf.bujo.models.DrawerItem;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private View drawerView;
    private DrawerAdapter drawerAdapter;
    private List<DrawerItem> drawerItemsList;
    private DatabaseManager dbManager;
    private View btnOverflow;
    private ImageView btnFab;
    private View btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 501);
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        drawerList = findViewById(R.id.nav_drawer_list);
        drawerView = findViewById(R.id.nav_drawer_container);

        // Header view matching screenshot with back arrow
        dbManager = new DatabaseManager(this);
        dbManager.open();

        drawerItemsList = new ArrayList<>();
        drawerAdapter = new DrawerAdapter(this, drawerItemsList);
        drawerList.setAdapter(drawerAdapter);

        drawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Adjust position for header
                int adjustedPosition = position - drawerList.getHeaderViewsCount();
                if (adjustedPosition >= 0 && adjustedPosition < drawerItemsList.size()) {
                    DrawerItem item = drawerItemsList.get(adjustedPosition);
                    if (item.getType() != DrawerItem.TYPE_SECTION) {
                        selectItem(adjustedPosition);
                    }
                }
            }
        });

        btnOverflow = findViewById(R.id.btn_bb10_overflow);
        btnFab = (ImageView) findViewById(R.id.bb10_fab);
        btnBack = findViewById(R.id.btn_bb10_back);
        
        btnOverflow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (current instanceof ProjectDetailFragment) {
                    ((ProjectDetailFragment) current).openRightSidebar();
                } else if (current instanceof com.ismailmushraf.bujo.fragments.WorkoutFragment) {
                    ((com.ismailmushraf.bujo.fragments.WorkoutFragment) current).openRightSidebar();
                } else if (drawerLayout.isDrawerOpen(drawerView)) {
                    drawerLayout.closeDrawer(drawerView);
                } else {
                    drawerLayout.openDrawer(drawerView);
                }
            }
        });
        
        btnFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleFabClick();
            }
        });
        
        if (btnBack != null) {
            btnBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
        }

        View.OnClickListener resetClickListener = v -> {
            Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (current instanceof com.ismailmushraf.bujo.fragments.PomodoroFragment) {
                ((com.ismailmushraf.bujo.fragments.PomodoroFragment) current).handleResetClick();
            }
        };

        View btnReset = findViewById(R.id.btn_bb10_reset);
        if (btnReset != null) {
            btnReset.setOnClickListener(resetClickListener);
        }

        View btnProjects = findViewById(R.id.btn_bb10_projects);
        if (btnProjects != null) {
            btnProjects.setOnClickListener(v -> navigateToFragment(new com.ismailmushraf.bujo.fragments.ProjectsFragment()));
        }

        View btnHabits = findViewById(R.id.btn_bb10_habits);
        if (btnHabits != null) {
            btnHabits.setOnClickListener(v -> navigateToFragment(new com.ismailmushraf.bujo.fragments.HabitsFragment()));
        }

        View btnWorkouts = findViewById(R.id.btn_bb10_workouts);
        if (btnWorkouts != null) {
            btnWorkouts.setOnClickListener(v -> navigateToFragment(new com.ismailmushraf.bujo.fragments.WorkoutFragment()));
        }

        View btnSettings = findViewById(R.id.btn_bb10_settings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> navigateToFragment(new SettingsFragment()));
        }

        getSupportFragmentManager().addOnBackStackChangedListener(new androidx.fragment.app.FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                updateBottomBarButtons(current);
            }
        });

        com.ismailmushraf.bujo.utils.AppExecutors.getInstance().diskIO().execute(() -> {
            DatabaseManager workerDatabase = new DatabaseManager(getApplicationContext());
            DatabaseManager.AuditResult audit;
            try {
                workerDatabase.open();
                audit = workerDatabase.evaluateDailyStreak();
            } finally {
                workerDatabase.close();
            }
            if (audit != null && audit.totalPenalty > 0) {
                com.ismailmushraf.bujo.utils.AppExecutors.getInstance().mainThread().execute(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        showAuditModal(audit);
                    }
                });
            }
        });

        refreshDrawer();

        if (savedInstanceState == null) {
            String navigateTo = getIntent().getStringExtra("NAVIGATE_TO");
            if ("Pomodoro".equals(navigateTo)) {
                selectItem(3); 
            } else {
                String startup = com.ismailmushraf.bujo.utils.AppPreferences.getStartupScreen(this);

                int startupIndex = 1; // Default to Today
                if ("Inbox".equals(startup)) {
                    startupIndex = 0;
                } else if ("Calendar".equals(startup)) {
                    startupIndex = 2;
                }
                selectItem(startupIndex);
            }
        }
    }

    public void refreshDrawer() {
        drawerItemsList.clear();
        drawerItemsList.add(new DrawerItem(DrawerItem.TYPE_ITEM, "Inbox", R.drawable.ic_inbox));
        drawerItemsList.add(new DrawerItem(DrawerItem.TYPE_ITEM, "Today", R.drawable.ic_today));
        drawerItemsList.add(new DrawerItem(DrawerItem.TYPE_ITEM, "Calendar", R.drawable.ic_calendar));
        drawerItemsList.add(new DrawerItem(DrawerItem.TYPE_ITEM, "Focus Timer", R.drawable.ic_bb10_timer));
        drawerItemsList.add(new DrawerItem(DrawerItem.TYPE_ITEM, "Logbook", R.drawable.ic_bb10_logbook));
        drawerItemsList.add(new DrawerItem(DrawerItem.TYPE_ITEM, "Profile", R.drawable.ic_today));
        drawerAdapter.notifyDataSetChanged();
    }

    private void selectItem(int position) {
        selectItemWithCustomAnim(position, R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void selectItemWithPopAnimation(int position) {
        selectItemWithCustomAnim(position, R.anim.slide_in_left, R.anim.slide_out_right);
    }

    private void selectItemWithCustomAnim(int position, int enterAnim, int exitAnim) {
        if (position < 0 || position >= drawerItemsList.size()) return;
        Fragment fragment = null;
        DrawerItem item = drawerItemsList.get(position);

        if (item.getType() == DrawerItem.TYPE_ITEM) {
            if ("Inbox".equals(item.title)) {
                fragment = new InboxFragment();
            } else if ("Today".equals(item.title)) {
                fragment = new DailyLogFragment();
            } else if ("Calendar".equals(item.title)) {
                fragment = new FutureLogFragment();
            } else if ("Logbook".equals(item.title)) {
                fragment = new MigratedItemsFragment();
            } else if ("Focus Timer".equals(item.title)) {
                fragment = new com.ismailmushraf.bujo.fragments.PomodoroFragment();
            } else if ("Profile".equals(item.title)) {
                fragment = new com.ismailmushraf.bujo.fragments.ProfileFragment();
            }
        }

        if (fragment != null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setCustomAnimations(enterAnim, exitAnim);
            ft.replace(R.id.fragment_container, fragment);
            ft.commit();
            updateBottomBarButtons(fragment);
        }

        drawerAdapter.setSelectedPosition(position);
        drawerLayout.closeDrawer(drawerView);
    }

    public void showDailyLog() {
        selectItem(1);
    }

    public void setToolbarTitle(String title) {
        // No top toolbar anymore, fragments will manage their own titles.
    }

    public void setToolbarSubtitle(String subtitle) {
        // No top toolbar anymore
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbManager != null) {
            dbManager.close();
        }
    }

    // Profile Menu functionality will be refactored to use the new BB10 layout instead of the Top Menu.
    public void refreshProfileIcon() {
        // To be updated
    }

    private void showAuditModal(DatabaseManager.AuditResult result) {
        android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(this, R.style.BujoDialog);
        final View v = getLayoutInflater().inflate(R.layout.dialog_audit, null);
        
        TextView tvPenalty = v.findViewById(R.id.audit_penalty_text);
        TextView tvReason = v.findViewById(R.id.audit_reason_text);
        
        tvPenalty.setText(getString(com.ismailmushraf.bujo.R.string.format_mainactivity_10, String.valueOf(result.totalPenalty)));
        tvReason.setText(getString(com.ismailmushraf.bujo.R.string.format_mainactivity_9, String.valueOf(result.missedTasks)));
        
        b.setView(v);
        final android.app.AlertDialog dialog = b.create();
        
        dialog.setOnShowListener(new android.content.DialogInterface.OnShowListener() {
            @Override
            public void onShow(android.content.DialogInterface dialogInterface) {
                if (dialog.getWindow() != null) {
                    android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(metrics);
                    int width = (int) (metrics.widthPixels * 0.90);
                    dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
                }
            }
        });

        View.OnClickListener dismissAction = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                animatePointsChange(-result.totalPenalty, view);
                dialog.dismiss();
            }
        };

        v.findViewById(R.id.btn_audit_ok).setOnClickListener(dismissAction);
        v.findViewById(R.id.dialog_root).setOnClickListener(dismissAction);
        
        dialog.show();
    }

    public void showPlanningBonusModal(final int totalPoints) {
        android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(this, R.style.BujoDialog);
        final View v = getLayoutInflater().inflate(R.layout.dialog_planning_bonus, null);
        
        TextView tvPoints = v.findViewById(R.id.bonus_points_text);
        tvPoints.setText(getString(com.ismailmushraf.bujo.R.string.format_mainactivity_8, String.valueOf(totalPoints)));
        
        b.setView(v);
        final android.app.AlertDialog dialog = b.create();

        dialog.setOnShowListener(new android.content.DialogInterface.OnShowListener() {
            @Override
            public void onShow(android.content.DialogInterface dialogInterface) {
                if (dialog.getWindow() != null) {
                    android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(metrics);
                    int width = (int) (metrics.widthPixels * 0.90);
                    dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
                }
            }
        });

        View.OnClickListener collectAction = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                animatePointsChange(totalPoints, view);
                dialog.dismiss();
            }
        };

        v.findViewById(R.id.btn_bonus_collect).setOnClickListener(collectAction);
        v.findViewById(R.id.dialog_root).setOnClickListener(collectAction);
        
        dialog.show();
    }

    public void animatePointsChange(final int amount, View sourceView) {
        if (amount == 0) return;

        if (amount > 0) com.ismailmushraf.bujo.utils.SoundHelper.playSuccess(this);
        else com.ismailmushraf.bujo.utils.SoundHelper.playPenalty(this);

        refreshProfileIcon(); // Immediate refresh

        if (!com.ismailmushraf.bujo.utils.AppPreferences.isAnimationsEnabled(this)) {
            return;
        }

        // Get coordinates of the source view
        int[] location = new int[2];
        if (sourceView != null) {
            sourceView.getLocationInWindow(location);
        } else {
            // Default to center if no source view
            location[0] = getWindow().getDecorView().getWidth() / 2;
            location[1] = getWindow().getDecorView().getHeight() / 2;
        }
        
        final TextView floatText = new TextView(this);
        floatText.setText(getString(R.string.points_delta, amount > 0 ? "+" : "", amount));
        int colorRes = amount > 0 ? R.color.points_positive : R.color.points_negative;
        floatText.setTextColor(getResources().getColor(colorRes));
        floatText.setTextSize(20);
        floatText.setTypeface(null, android.graphics.Typeface.BOLD);
        
        final ViewGroup root = (ViewGroup) getWindow().getDecorView();
        root.addView(floatText);
        
        floatText.setX(location[0]);
        floatText.setY(location[1]);
        
        // Target coordinate
        float targetX = root.getWidth() / 2f - location[0];
        float targetY = root.getHeight() - 100 - location[1];

        floatText.animate()
                .translationX(targetX)
                .translationY(targetY)
                .alpha(0)
                .setDuration(1000)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        root.removeView(floatText);
                        refreshProfileIcon();
                    }
                })
                .start();
    }

    @Override
    public void onBackPressed() {
        // 1. If the navigation drawer is open, close it first
        if (drawerLayout.isDrawerOpen(drawerView)) {
            drawerLayout.closeDrawer(drawerView);
            return;
        }

        // 2. If we are deep inside a flow (like Project Details or Workout History), pop back to the previous screen
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            return;
        }

        // 3. Determine which fragment is currently visible
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        // Fetch the user's preferred startup screen from Settings
        String startup = com.ismailmushraf.bujo.utils.AppPreferences.getStartupScreen(this);

        // Check if we are already on the default screen
        boolean isDefaultScreen = false;
        if (currentFragment instanceof com.ismailmushraf.bujo.fragments.DailyLogFragment && "Today".equals(startup)) {
            isDefaultScreen = true;
        } else if (currentFragment instanceof com.ismailmushraf.bujo.fragments.InboxFragment && "Inbox".equals(startup)) {
            isDefaultScreen = true;
        } else if (currentFragment instanceof com.ismailmushraf.bujo.fragments.FutureLogFragment && "Calendar".equals(startup)) {
            isDefaultScreen = true;
        }

        // 4. If we are NOT on the default screen, navigate there instead of exiting
        if (!isDefaultScreen) {
            int startupIndex = 1; // Default to Today
            if ("Inbox".equals(startup)) {
                startupIndex = 0;
            } else if ("Calendar".equals(startup)) {
                startupIndex = 2;
            }
            selectItemWithPopAnimation(startupIndex); // Uses slide_in_left & slide_out_right for smooth back transition!
        } else {
            // 5. If we ARE on the default screen, let Android exit the app normally
            super.onBackPressed();
        }
    }

    public void pushFragment(Fragment fragment) {
        if (fragment == null) return;
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right);
        ft.replace(R.id.fragment_container, fragment);
        ft.addToBackStack(null);
        ft.commit();
        updateBottomBarButtons(fragment);
    }

    private void navigateToFragment(Fragment fragment) {
        pushFragment(fragment);
    }

    public void updateBottomBarButtons(Fragment fragment) {
        if (fragment == null) {
            fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        }

        View bottomBar = findViewById(R.id.bb10_bottom_bar);
        View btnBack = findViewById(R.id.btn_bb10_back);
        ImageView fabView = findViewById(R.id.bb10_fab);
        View btnOverflow = findViewById(R.id.btn_bb10_overflow);
        View btnProjects = findViewById(R.id.btn_bb10_projects);
        View btnHabits = findViewById(R.id.btn_bb10_habits);
        View btnWorkouts = findViewById(R.id.btn_bb10_workouts);
        View btnSettings = findViewById(R.id.btn_bb10_settings);
        TextView tvFabText = findViewById(R.id.tv_bb10_fab_text);
        boolean timerScreen = fragment instanceof com.ismailmushraf.bujo.fragments.PomodoroFragment;
        for (int id : new int[]{R.id.btn_bb10_reset, R.id.space_bb10_pomodoro_1,
                R.id.space_bb10_pomodoro_2, R.id.space_bb10_pomodoro_5}) {
            findViewById(id).setVisibility(timerScreen ? View.VISIBLE : View.GONE);
        }
        if (timerScreen && btnFab != null) btnFab.setVisibility(View.VISIBLE);

        if (fragment instanceof com.ismailmushraf.bujo.fragments.EditProjectFragment ||
            fragment instanceof com.ismailmushraf.bujo.fragments.EditTaskFragment) {
            if (bottomBar != null) bottomBar.setVisibility(View.GONE);
            if (btnFab != null) btnFab.setVisibility(View.GONE);
            return;
        } else {
            if (bottomBar != null) bottomBar.setVisibility(View.VISIBLE);
        }

        android.content.SharedPreferences prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(this);
        String startup = prefs.getString("startup_screen", "Today");

        boolean isPrimaryScreen = false;
        if (fragment instanceof com.ismailmushraf.bujo.fragments.DailyLogFragment && "Today".equals(startup)) {
            isPrimaryScreen = true;
        } else if (fragment instanceof com.ismailmushraf.bujo.fragments.InboxFragment && "Inbox".equals(startup)) {
            isPrimaryScreen = true;
        }

        if (btnBack != null) {
            btnBack.setVisibility(isPrimaryScreen ? View.INVISIBLE : View.VISIBLE);
        }

        int primaryOnlyNavVisibility = isPrimaryScreen ? View.VISIBLE : View.GONE;
        if (btnProjects != null) btnProjects.setVisibility(primaryOnlyNavVisibility);
        if (btnHabits != null) btnHabits.setVisibility(primaryOnlyNavVisibility);
        if (btnWorkouts != null) btnWorkouts.setVisibility(primaryOnlyNavVisibility);
        if (btnSettings != null) btnSettings.setVisibility(primaryOnlyNavVisibility);

        if (fragment instanceof com.ismailmushraf.bujo.fragments.SettingsFragment ||
            fragment instanceof com.ismailmushraf.bujo.fragments.FutureLogFragment ||
            fragment instanceof com.ismailmushraf.bujo.fragments.HabitProgressFragment ||
            fragment instanceof com.ismailmushraf.bujo.fragments.ProjectHistoryFragment ||
            fragment instanceof com.ismailmushraf.bujo.fragments.MigratedItemsFragment) {
            if (btnFab != null) btnFab.setVisibility(View.GONE);
            if (btnOverflow != null) btnOverflow.setVisibility(View.INVISIBLE);
            if (tvFabText != null) tvFabText.setVisibility(View.GONE);
            return;
        }

        View btnResetBar = findViewById(R.id.btn_bb10_reset);
        View space1 = findViewById(R.id.space_bb10_pomodoro_1);
        View space2 = findViewById(R.id.space_bb10_pomodoro_2);
        View space5 = findViewById(R.id.space_bb10_pomodoro_5);

        if (fragment instanceof com.ismailmushraf.bujo.fragments.PomodoroFragment) {
            if (fabView != null) fabView.setVisibility(View.VISIBLE);
            if (btnOverflow != null) btnOverflow.setVisibility(View.INVISIBLE);
            if (tvFabText != null) tvFabText.setVisibility(View.VISIBLE);
            if (btnResetBar != null) btnResetBar.setVisibility(View.VISIBLE);
            if (space1 != null) space1.setVisibility(View.VISIBLE);
            if (space2 != null) space2.setVisibility(View.VISIBLE);
            if (space5 != null) space5.setVisibility(View.VISIBLE);
            return;
        } else {
            if (btnResetBar != null) btnResetBar.setVisibility(View.GONE);
            if (space1 != null) space1.setVisibility(View.GONE);
            if (space2 != null) space2.setVisibility(View.GONE);
            if (space5 != null) space5.setVisibility(View.GONE);
            if (fabView != null) fabView.setImageResource(R.drawable.ic_bb10_compose);
        }

        boolean isOverflowVisible = isPrimaryScreen || 
                                    (fragment instanceof ProjectDetailFragment) || 
                                    (fragment instanceof com.ismailmushraf.bujo.fragments.WorkoutFragment);
                                    
        boolean hideFab = (fragment instanceof com.ismailmushraf.bujo.fragments.WorkoutFragment) || 
                          (fragment instanceof com.ismailmushraf.bujo.fragments.WorkoutHistoryFragment);

        if (btnFab != null) btnFab.setVisibility(hideFab ? View.GONE : View.VISIBLE);
        if (btnOverflow != null) btnOverflow.setVisibility(isOverflowVisible ? View.VISIBLE : View.INVISIBLE);

        if (fragment instanceof com.ismailmushraf.bujo.fragments.HabitsFragment) {
            if (tvFabText != null) {
                tvFabText.setText(getString(com.ismailmushraf.bujo.R.string.ui_new_habit_bc5fea));
                tvFabText.setVisibility(View.VISIBLE);
            }
        } else if (fragment instanceof com.ismailmushraf.bujo.fragments.ProjectsFragment) {
            if (tvFabText != null) {
                tvFabText.setText(getString(com.ismailmushraf.bujo.R.string.ui_create_project_1cab44));
                tvFabText.setVisibility(View.VISIBLE);
            }
        } else if (hideFab) {
            if (tvFabText != null) tvFabText.setVisibility(View.GONE);
        } else {
            if (tvFabText != null) {
                tvFabText.setText(getString(com.ismailmushraf.bujo.R.string.ui_new_task_cc3dbd));
                tvFabText.setVisibility(View.VISIBLE);
            }
        }
    }

    public void updateFabForPomodoro(boolean isRunning, boolean isPaused, boolean isFocus) {
        ImageView fab = findViewById(R.id.bb10_fab);
        TextView tvFabText = findViewById(R.id.tv_bb10_fab_text);
        View btnResetBar = findViewById(R.id.btn_bb10_reset);

        if (fab != null) {
            if (isRunning) {
                fab.setImageResource(R.drawable.ic_bb10_pause);
            } else {
                fab.setImageResource(R.drawable.ic_bb10_play);
            }
        }

        if (tvFabText != null) {
            if (isRunning) {
                tvFabText.setText(getString(com.ismailmushraf.bujo.R.string.ui_pause_781961));
            } else if (isPaused) {
                tvFabText.setText(getString(com.ismailmushraf.bujo.R.string.ui_resume_b3bd0b));
            } else {
                tvFabText.setText(isFocus ? "Start" : "Start Break");
            }
            tvFabText.setVisibility(View.VISIBLE);
            if (fab != null) fab.setContentDescription(tvFabText.getText());
        }

        if (btnResetBar != null) {
            btnResetBar.setEnabled(true);
            btnResetBar.setAlpha(isRunning ? 0.5f : 1.0f);
        }
    }

    private void handleFabClick() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof com.ismailmushraf.bujo.fragments.HabitsFragment) {
            ((com.ismailmushraf.bujo.fragments.HabitsFragment) currentFragment).showAddHabitDialog();
        } else if (currentFragment instanceof com.ismailmushraf.bujo.fragments.ProjectsFragment) {
            ((com.ismailmushraf.bujo.fragments.ProjectsFragment) currentFragment).openCreateProjectScreen();
        } else if (currentFragment instanceof ProjectDetailFragment) {
            ProjectDetailFragment pdf = (ProjectDetailFragment) currentFragment;
            navigateToFragment(com.ismailmushraf.bujo.fragments.EditTaskFragment.newInstanceForCreate(pdf.getProjectId(), pdf.getProjectName()));
        } else if (currentFragment instanceof com.ismailmushraf.bujo.fragments.PomodoroFragment) {
            ((com.ismailmushraf.bujo.fragments.PomodoroFragment) currentFragment).handlePrimaryClick();
        } else {
            navigateToFragment(com.ismailmushraf.bujo.fragments.EditTaskFragment.newInstanceForCreate());
        }
    }
}
