package com.ismailmushraf.bujo.coach;

import android.content.Context;
import android.content.SharedPreferences;

public final class CoachPreferences {
    private CoachPreferences() {}
    public static SharedPreferences get(Context context) {
        return context.getSharedPreferences("coach_private", Context.MODE_PRIVATE);
    }
    public static String model(Context context) {
        String saved=get(context).getString("model", "");
        // Older settings screens could persist an empty value. Keep a usable default.
        return saved==null || saved.trim().isEmpty() ? "gemini-2.5-flash" : saved.trim();
    }
    public static boolean validModel(String model) {
        return model != null && model.matches("[a-zA-Z0-9][a-zA-Z0-9._-]{0,99}");
    }
}
