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
    private ActionBarDrawerToggle drawerToggle;
    private DrawerAdapter drawerAdapter;
    private List<DrawerItem> drawerItemsList;
    private DatabaseManager dbManager;
    private Toolbar toolbar;
    private Menu mainMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (ListView) findViewById(R.id.nav_drawer_list);

        // Header view matching screenshot with back arrow
        View headerView = getLayoutInflater().inflate(R.layout.nav_header, drawerList, false);
        drawerList.addHeaderView(headerView, null, false);

        dbManager = new DatabaseManager(this);
        dbManager.open();

        drawerItemsList = new ArrayList<>();
        drawerAdapter = new DrawerAdapter(this, drawerItemsList);
        drawerList.setAdapter(drawerAdapter);

        // Header navigation back click closes drawer
        headerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.closeDrawer(drawerList);
            }
        });

        View btnSettings = headerView.findViewById(R.id.header_settings);
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.fragment_container, new SettingsFragment());
                ft.commit();

                drawerAdapter.setSelectedPosition(-1); // Removes highlight from the main list
                drawerLayout.closeDrawer(drawerList);
            }
        });

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

        toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);

        drawerToggle = new ActionBarDrawerToggle(
                this, drawerLayout, R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu();
            }
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
            }
        };
        drawerLayout.setDrawerListener(drawerToggle);

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
        drawerItemsList.add(new DrawerItem(DrawerItem.TYPE_SECTION, "JOURNAL INDEX", null));
        drawerItemsList.add(new DrawerItem(DrawerItem.TYPE_ITEM, "Inbox", "\uD83D\uDCE5")); // 📥 icon
        drawerItemsList.add(new DrawerItem(DrawerItem.TYPE_ITEM, "Today", "\uD83D\uDCDD"));
        drawerItemsList.add(new DrawerItem(DrawerItem.TYPE_ITEM, "Habits", "\uD83C\uDF31"));
        drawerItemsList.add(new DrawerItem(DrawerItem.TYPE_ITEM, "Calendar", "📅"));
        drawerItemsList.add(new DrawerItem(DrawerItem.TYPE_ITEM, "Focus Timer", "\u23F0"));
        drawerItemsList.add(new DrawerItem(DrawerItem.TYPE_ITEM, "Logbook", ">")); // Changed name
        drawerItemsList.add(new DrawerItem(DrawerItem.TYPE_ITEM, "Workouts", "\uD83D\uDCAA")); // Add Workout module
        drawerItemsList.add(new DrawerItem(DrawerItem.TYPE_ITEM, "Projects", "+"));
        List<Project> projects = dbManager.getAllProjects();
        for (Project p : projects) {
            drawerItemsList.add(new DrawerItem(DrawerItem.TYPE_PROJECT, p.getName(), "-", p.getId()));
        }
        drawerAdapter.notifyDataSetChanged();
    }

    private void selectItem(int position) {
        Fragment fragment = null;
        DrawerItem item = drawerItemsList.get(position);

        if (item.getType() == DrawerItem.TYPE_ITEM) {
            if ("Inbox".equals(item.title)) {
                fragment = new InboxFragment();
            } else if ("Today".equals(item.title)) {
                fragment = new DailyLogFragment();
            } else if ("Habits".equals(item.title)) {
                fragment = new HabitsFragment();
            } else if ("Calendar".equals(item.title)) {
                fragment = new FutureLogFragment();
            } else if ("Logbook".equals(item.title)) {
                fragment = new MigratedItemsFragment(); // Connects the new Logbook string to the fragment
            } else if ("Focus Timer".equals(item.title)) {
                fragment = new com.ismailmushraf.bujo.fragments.PomodoroFragment();
            } else if ("Workouts".equals(item.title)) {
                fragment = new WorkoutFragment(); // Handle Workout click
            } else if ("Projects".equals(item.title)) {
                fragment = new ProjectsFragment();
            }
        } else if (item.getType() == DrawerItem.TYPE_PROJECT) {
            fragment = ProjectDetailFragment.newInstance(item.projectId, item.title);
        }

        if (fragment != null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.fragment_container, fragment);
            ft.commit();
        }

        drawerAdapter.setSelectedPosition(position);
        drawerLayout.closeDrawer(drawerList);
    }

    public void showDailyLog() {
        selectItem(1);
    }

    public void setToolbarTitle(String title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    public void setToolbarSubtitle(String subtitle) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(subtitle);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbManager != null) {
            dbManager.close();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.mainMenu = menu;
        getMenuInflater().inflate(R.menu.main_menu, menu);

        MenuItem profileItem = menu.findItem(R.id.action_profile);
        View actionView = MenuItemCompat.getActionView(profileItem);

        if (actionView != null) {
            TextView tvProfileIcon = actionView.findViewById(R.id.tv_menu_profile_icon);
            TextView tvProfilePoints = actionView.findViewById(R.id.tv_menu_profile_points);

            // Update the icon based on current user points
            updateProfileIconText(tvProfileIcon, tvProfilePoints);

            // Click listener to navigate directly to ProfileFragment
            actionView.setOnClickListener(v -> {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.fragment_container, new ProfileFragment());
                ft.addToBackStack(null); // Allows returning via back button
                ft.commit();
                drawerAdapter.setSelectedPosition(-1); // Unhighlight drawer items
            });
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem profileItem = menu.findItem(R.id.action_profile);
        if (profileItem != null) {
            View actionView = MenuItemCompat.getActionView(profileItem);
            if (actionView != null) {
                TextView tvProfileIcon = actionView.findViewById(R.id.tv_menu_profile_icon);
                TextView tvProfilePoints = actionView.findViewById(R.id.tv_menu_profile_points);
                updateProfileIconText(tvProfileIcon, tvProfilePoints);
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    private void updateProfileIconText(TextView tvProfileIcon, TextView tvProfilePoints) {
        if (tvProfileIcon == null || dbManager == null) return;

        int[] stats = dbManager.getUserStats();
        int points = stats[0];

        String rankIcon = "🚶"; // Default Level 1
        int nextLevelPoints = 500;
        if (points >= 10000) {
            rankIcon = "⛩️"; // Level 5: Monk
            nextLevelPoints = -1;
        } else if (points >= 5000) {
            rankIcon = "⛰️"; // Level 4: Ascendant
            nextLevelPoints = 10000;
        } else if (points >= 2000) {
            rankIcon = "🎓"; // Level 3: Scholar
            nextLevelPoints = 5000;
        } else if (points >= 500) {
            rankIcon = "📖"; // Level 2: Apprentice
            nextLevelPoints = 2000;
        }

        tvProfileIcon.setText(rankIcon);
        if (tvProfilePoints != null) {
            if (nextLevelPoints == -1) {
                tvProfilePoints.setText(String.valueOf(points));
            } else {
                tvProfilePoints.setText(points + "/" + nextLevelPoints);
            }
        }
    }

    // Public method to refresh toolbar icon whenever points are earned/deducted
    public void refreshProfileIcon() {
        if (mainMenu != null) {
            MenuItem profileItem = mainMenu.findItem(R.id.action_profile);
            if (profileItem != null) {
                View actionView = MenuItemCompat.getActionView(profileItem);
                if (actionView != null) {
                    TextView tvProfileIcon = actionView.findViewById(R.id.tv_menu_profile_icon);
                    TextView tvProfilePoints = actionView.findViewById(R.id.tv_menu_profile_points);
                    updateProfileIconText(tvProfileIcon, tvProfilePoints);
                }
            }
        }
        supportInvalidateOptionsMenu();
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
        
        // Find the profile icon in the toolbar
        View profileView = null;
        if (mainMenu != null) {
            MenuItem profileItem = mainMenu.findItem(R.id.action_profile);
            if (profileItem != null) profileView = MenuItemCompat.getActionView(profileItem);
        }
        float targetX = root.getWidth() - location[0] - floatText.getMeasuredWidth();
        float targetY = -location[1];
        
        if (profileView != null) {
            int[] dest = new int[2];
            profileView.getLocationInWindow(dest);
            targetX = (dest[0] + (profileView.getWidth() / 2f)) - location[0];
            targetY = dest[1] - location[1];
        }

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
            selectItem(startupIndex); // Use your existing method to load the fragment
        } else {
            // 5. If we ARE on the default screen, let Android exit the app normally
            super.onBackPressed();
        }
    }
}
