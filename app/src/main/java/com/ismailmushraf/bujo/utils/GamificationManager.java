package com.ismailmushraf.bujo.utils;

import com.ismailmushraf.bujo.models.Entry;

public class GamificationManager {

    public static final int POINTS_WORKOUT_SET = 10;
    public static final int POINTS_DATE_COMMITMENT = 5;
    public static final int POINTS_HABIT_COMPLETE = 5;

    public static int calculateCommitmentReward(Entry entry) {
        if (entry == null) return 0;
        if (entry.getDeadline() > 0) {
            return POINTS_DATE_COMMITMENT;
        }
        return 0;
    }

    public static int calculateCompletionReward(Entry entry) {
        if (entry == null) return 0;
        return POINTS_DATE_COMMITMENT;
    }
}
