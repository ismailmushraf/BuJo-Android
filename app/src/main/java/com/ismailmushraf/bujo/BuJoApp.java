package com.ismailmushraf.bujo;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatDelegate;

public class BuJoApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Read the saved theme preference.
        // Default is MODE_NIGHT_AUTO (which changes based on time of day on Android 4.3)
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int themeMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_AUTO);

        AppCompatDelegate.setDefaultNightMode(themeMode);
    }
}