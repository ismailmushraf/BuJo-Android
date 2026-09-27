package com.ismailmushraf.bujo.utils;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.util.Log;

public class SoundHelper {

    private static final String TAG = "SoundHelper";

    public static void playSuccess(Context context) {
        if (!isSoundEnabled(context)) return;
        try {
            ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
            tg.startTone(ToneGenerator.TONE_PROP_BEEP, 150);
        } catch (Exception e) {
            Log.e(TAG, "Error playing success sound", e);
        }
    }

    public static void playPenalty(Context context) {
        if (!isSoundEnabled(context)) return;
        try {
            ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
            tg.startTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 200);
        } catch (Exception e) {
            Log.e(TAG, "Error playing penalty sound", e);
        }
    }

    private static boolean isSoundEnabled(Context context) {
        return AppPreferences.isSoundsEnabled(context);
    }
}
