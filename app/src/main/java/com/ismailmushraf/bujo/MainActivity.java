package com.ismailmushraf.bujo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

import com.ismailmushraf.bujo.adapters.DrawerAdapter;
import com.ismailmushraf.bujo.db.DatabaseManager;
import com.ismailmushraf.bujo.fragments.DailyLogFragment;
import com.ismailmushraf.bujo.fragments.FutureLogFragment;
import com.ismailmushraf.bujo.fragments.HabitsFragment;
import com.ismailmushraf.bujo.fragments.InboxFragment;
import com.ismailmushraf.bujo.fragments.MigratedItemsFragment;
import com.ismailmushraf.bujo.fragments.ProfileFragment;
import com.ismailmushraf.bujo.fragments.ProjectDetailFragment;
import com.ismailmushraf.bujo.fragments.ProjectsFragment;
import com.ismailmushraf.bujo.fragments.SettingsFragment;
import com.ismailmushraf.bujo.fragments.WorkoutFragment;
import com.ismailmushraf.bujo.models.DrawerItem;
import com.ismailmushraf.bujo.models.Project;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private DrawerAdapter drawerAdapter;
    private List<DrawerItem> drawerItemsList;
    private DatabaseManager dbManager;
    private View btnOverflow;
    private View btnFab;
    private View btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (ListView) findViewById(R.id.nav_drawer_list);

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
        btnFab = findViewById(R.id.bb10_fab);
        btnBack = findViewById(R.id.btn_bb10_back);
        
        btnOverflow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawerLayout.isDrawerOpen(drawerList)) {
                    drawerLayout.closeDrawer(drawerList);
                } else {
                    drawerLayout.openDrawer(drawerList);
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

        getSupportFragmentManager().addOnBackStackChangedListener(new android.support.v4.app.FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                updateBottomBarButtons(current);
            }
        });

        DatabaseManager.AuditResult audit = dbManager.evaluateDailyStreak();
        if (audit != null && audit.totalPenalty > 0) {
            showAuditModal(audit);
        }

        refreshDrawer();

        if (savedInstanceState == null) {
            String navigateTo = getIntent().getStringExtra("NAVIGATE_TO");
            if ("Pomodoro".equals(navigateTo)) {
                selectItem(5); 
            } else {
                android.content.SharedPreferences prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(this);
                String startup = prefs.getString("startup_screen", "Today");

                int startupIndex = 2; // Default to Today
                if ("Inbox".equals(startup)) {
                    startupIndex = 1;
                } else if ("Calendar".equals(startup)) {
                    startupIndex = 3;
                }
                selectItem(startupIndex);
            }
        }
    }

    public void refreshDrawer() {
        drawerItemsList.clear();
        drawerItemsList.add(new DrawerItem(DrawerItem.TYPE_SECTION, "JOURNAL INDEX", 0));
        drawerItemsList.add(new DrawerItem(DrawerItem.TYPE_ITEM, "Inbox", R.drawable.ic_inbox));
        drawerItemsList.add(new DrawerItem(DrawerItem.TYPE_ITEM, "Today", R.drawable.ic_today));
        drawerItemsList.add(new DrawerItem(DrawerItem.TYPE_ITEM, "Calendar", R.drawable.ic_calendar));
        drawerItemsList.add(new DrawerItem(DrawerItem.TYPE_ITEM, "Focus Timer", R.drawable.ic_bb10_timer));
        drawerItemsList.add(new DrawerItem(DrawerItem.TYPE_ITEM, "Logbook", R.drawable.ic_bb10_logbook));
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
        drawerLayout.closeDrawer(drawerList);
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
    protected void onPostResume() {
        super.onPostResume();
        // BlackBerry's Android 4.3 runtime can retain GradientDrawable color data
        // after recreation. Text uses the new resources, but boxed controls may not.
        // Reapply the active theme colors to the already-inflated large box controls.
        getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                refreshLegacyBoxColors(getWindow().getDecorView());
            }
        });
    }

    private void refreshLegacyBoxColors(View view) {
        if (view.getId() == R.id.bb10_fab) {
            return; // Skip the BB10 FAB so it retains its blue color
        }

        Drawable background = view.getBackground();
        int boxThreshold = (int) (48 * getResources().getDisplayMetrics().density);
        if (background instanceof GradientDrawable
                && (view.getWidth() >= boxThreshold || view.getHeight() >= boxThreshold)) {
            GradientDrawable box = (GradientDrawable) background.mutate();
            box.setColor(getResources().getColor(R.color.bujo_background));
            box.setStroke((int) (2 * getResources().getDisplayMetrics().density),
                    getResources().getColor(R.color.bujo_box_border));
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                refreshLegacyBoxColors(group.getChildAt(i));
            }
        }
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
        
        tvPenalty.setText("-" + result.totalPenalty + " pts");
        tvReason.setText("You missed " + result.missedTasks + " tasks recently.\nKeep up the discipline today!");
        
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
        tvPoints.setText("+" + totalPoints + " pts");
        
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

        android.content.SharedPreferences prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(this);
        if (amount > 0) com.ismailmushraf.bujo.utils.SoundHelper.playSuccess(this);
        else com.ismailmushraf.bujo.utils.SoundHelper.playPenalty(this);

        refreshProfileIcon(); // Immediate refresh

        if (!prefs.getBoolean("enable_animations", true)) {
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
        floatText.setText((amount > 0 ? "+" : "") + amount);
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
        if (drawerLayout.isDrawerOpen(drawerList)) {
            drawerLayout.closeDrawer(drawerList);
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
        android.content.SharedPreferences prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(this);
        String startup = prefs.getString("startup_screen", "Today");

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
            int startupIndex = 2; // Default to Today
            if ("Inbox".equals(startup)) {
                startupIndex = 1;
            } else if ("Calendar".equals(startup)) {
                startupIndex = 3;
            }
            selectItemWithPopAnimation(startupIndex); // Uses slide_in_left & slide_out_right for smooth back transition!
        } else {
            // 5. If we ARE on the default screen, let Android exit the app normally
            super.onBackPressed();
        }
    }

    private void navigateToFragment(Fragment fragment) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right);
        ft.replace(R.id.fragment_container, fragment);
        ft.addToBackStack(null);
        ft.commit();
        updateBottomBarButtons(fragment);
    }

    public void updateBottomBarButtons(Fragment fragment) {
        if (fragment == null) {
            fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        }

        View bottomBar = findViewById(R.id.bb10_bottom_bar);
        View btnBack = findViewById(R.id.btn_bb10_back);
        View btnFab = findViewById(R.id.bb10_fab);
        View btnOverflow = findViewById(R.id.btn_bb10_overflow);
        View btnProjects = findViewById(R.id.btn_bb10_projects);
        View btnHabits = findViewById(R.id.btn_bb10_habits);
        View btnWorkouts = findViewById(R.id.btn_bb10_workouts);
        View btnSettings = findViewById(R.id.btn_bb10_settings);
        TextView tvFabText = findViewById(R.id.tv_bb10_fab_text);

        if (fragment instanceof com.ismailmushraf.bujo.fragments.EditProjectFragment) {
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
        } else if (fragment instanceof com.ismailmushraf.bujo.fragments.FutureLogFragment && "Calendar".equals(startup)) {
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
            fragment instanceof com.ismailmushraf.bujo.fragments.FutureLogFragment) {
            if (btnFab != null) btnFab.setVisibility(View.GONE);
            if (btnOverflow != null) btnOverflow.setVisibility(View.GONE);
            if (tvFabText != null) tvFabText.setVisibility(View.GONE);
            return;
        }

        if (btnFab != null) btnFab.setVisibility(View.VISIBLE);
        if (btnOverflow != null) btnOverflow.setVisibility(View.VISIBLE);

        if (fragment instanceof com.ismailmushraf.bujo.fragments.HabitsFragment) {
            if (tvFabText != null) {
                tvFabText.setText("New Habit");
                tvFabText.setVisibility(View.VISIBLE);
            }
        } else if (fragment instanceof com.ismailmushraf.bujo.fragments.ProjectsFragment) {
            if (tvFabText != null) {
                tvFabText.setText("Create Project");
                tvFabText.setVisibility(View.VISIBLE);
            }
        } else {
            if (tvFabText != null) {
                tvFabText.setText("New Task");
                tvFabText.setVisibility(View.VISIBLE);
            }
        }
    }

    private void handleFabClick() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof com.ismailmushraf.bujo.fragments.HabitsFragment) {
            ((com.ismailmushraf.bujo.fragments.HabitsFragment) currentFragment).showAddHabitDialog();
        } else if (currentFragment instanceof com.ismailmushraf.bujo.fragments.ProjectsFragment) {
            ((com.ismailmushraf.bujo.fragments.ProjectsFragment) currentFragment).openCreateProjectScreen();
        }
    }
}
