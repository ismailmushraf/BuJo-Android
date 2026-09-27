package com.ismailmushraf.bujo.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class AppPreferences {

    public static final String KEY_STARTUP_SCREEN = "startup_screen";
    public static final String KEY_ENABLE_ANIMATIONS = "enable_animations";
    public static final String KEY_ENABLE_SOUNDS = "enable_sounds";

    private static SharedPreferences getPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static String getStartupScreen(Context context) {
        return getPrefs(context).getString(KEY_STARTUP_SCREEN, "Today");
    }

    public static void setStartupScreen(Context context, String value) {
        getPrefs(context).edit().putString(KEY_STARTUP_SCREEN, value).apply();
    }

    public static boolean isAnimationsEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_ENABLE_ANIMATIONS, true);
    }

    public static void setAnimationsEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_ENABLE_ANIMATIONS, enabled).apply();
    }

    public static boolean isSoundsEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_ENABLE_SOUNDS, true);
    }

    public static void setSoundsEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_ENABLE_SOUNDS, enabled).apply();
    }
}
