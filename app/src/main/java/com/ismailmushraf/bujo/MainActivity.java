package com.ismailmushraf.bujo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.ismailmushraf.bujo.adapters.DrawerAdapter;
import com.ismailmushraf.bujo.db.DatabaseManager;
import com.ismailmushraf.bujo.fragments.DailyLogFragment;
import com.ismailmushraf.bujo.fragments.FutureLogFragment;
import com.ismailmushraf.bujo.fragments.InboxFragment;
import com.ismailmushraf.bujo.fragments.MigratedItemsFragment;
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
    private android.support.v7.widget.Toolbar toolbar;

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

        refreshDrawer();

        if (savedInstanceState == null) {
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

    public void refreshDrawer() {
        drawerItemsList.clear();
        drawerItemsList.add(new DrawerItem(DrawerItem.TYPE_SECTION, "JOURNAL INDEX", null));
        drawerItemsList.add(new DrawerItem(DrawerItem.TYPE_ITEM, "Inbox", "\uD83D\uDCE5")); // 📥 icon
        drawerItemsList.add(new DrawerItem(DrawerItem.TYPE_ITEM, "Today", "\uD83D\uDCDD"));
        drawerItemsList.add(new DrawerItem(DrawerItem.TYPE_ITEM, "Calendar", "📅"));
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
            } else if ("Calendar".equals(item.title)) {
                fragment = new FutureLogFragment();
            } else if ("Logbook".equals(item.title)) {
                fragment = new MigratedItemsFragment(); // Connects the new Logbook string to the fragment
            }else if ("Workouts".equals(item.title)) {
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
}
