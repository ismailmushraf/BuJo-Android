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
        return saved==null || saved.trim().isEmpty() ? "gemini-2.5-flash" : saved.trim();
    }
    public static boolean validModel(String model) {
        return model != null && model.matches("[a-zA-Z0-9][a-zA-Z0-9._-]{0,99}");
    }
    public static String vibe(Context context) {
        String saved=get(context).getString("vibe", "cheerful");
        return saved==null || saved.trim().isEmpty() ? "cheerful" : saved.trim();
    }
    public static boolean validVibe(String vibe) {
        return "cheerful".equals(vibe) || "direct".equals(vibe) || "mindful".equals(vibe);
    }
}
