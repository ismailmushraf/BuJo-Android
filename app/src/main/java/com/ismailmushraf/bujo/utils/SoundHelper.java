package com.ismailmushraf.bujo.utils;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class SoundHelper {

    private static final String TAG = "SoundHelper";
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public static void playSuccess(Context context) {
        if (!isSoundEnabled(context)) return;
        try {
            ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
            playAndRelease(tg, ToneGenerator.TONE_PROP_BEEP, 150);
        } catch (Exception e) {
            Log.e(TAG, "Error playing success sound", e);
        }
    }

    public static void playPenalty(Context context) {
        if (!isSoundEnabled(context)) return;
        try {
            ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
            playAndRelease(tg, ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 200);
        } catch (Exception e) {
            Log.e(TAG, "Error playing penalty sound", e);
        }
    }

    private static boolean isSoundEnabled(Context context) {
        return AppPreferences.isSoundsEnabled(context);
    }

    private static void playAndRelease(final ToneGenerator generator, int tone, int durationMs) {
        generator.startTone(tone, durationMs);
        MAIN_HANDLER.postDelayed(generator::release, durationMs + 50L);
    }
}
