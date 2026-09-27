package com.ismailmushraf.bujo.coach;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import java.util.Calendar;

public final class CoachScheduler {
    private CoachScheduler() {}
    public static long nextTime(long now, int startHour, int endHour) {
        if(startHour<0 || endHour>23 || startHour>=endHour) throw new IllegalArgumentException("Invalid waking hours");
        Calendar day = Calendar.getInstance(); day.setTimeInMillis(now);
        for (int offset=0; offset<2; offset++) {
            for (int hour=startHour; hour<=endHour; hour++) {
                if (hour != endHour && (hour-startHour)%4 != 0) continue;
                Calendar slot=(Calendar)day.clone(); slot.add(Calendar.DATE, offset);
                slot.set(Calendar.HOUR_OF_DAY,hour); slot.set(Calendar.MINUTE,0);
                slot.set(Calendar.SECOND,0); slot.set(Calendar.MILLISECOND,0);
                if (slot.getTimeInMillis()>now) return slot.getTimeInMillis();
            }
        }
        throw new IllegalArgumentException("Invalid waking hours");
    }
    private static PendingIntent alarm(Context context) {
        Intent intent = new Intent(context, CoachReceiver.class).setAction("com.ismailmushraf.bujo.COACH_CHECKIN");
        return PendingIntent.getBroadcast(context, 71001, intent, PendingIntent.FLAG_UPDATE_CURRENT |
                (Build.VERSION.SDK_INT>=23 ? PendingIntent.FLAG_IMMUTABLE : 0));
    }
    public static void schedule(Context context) {
        SharedPreferences prefs=CoachPreferences.get(context);
        AlarmManager manager=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        if (manager==null) return;
        PendingIntent pending=alarm(context);
        manager.cancel(pending);
        if (!prefs.getBoolean("enabled",false)) return;
        long now=System.currentTimeMillis();
        long next=nextTime(now,prefs.getInt("start",8),prefs.getInt("end",20));
        long snooze=prefs.getLong("snooze",0);
        if (snooze>now) {
            Calendar c=Calendar.getInstance(); c.setTimeInMillis(snooze);
            int h=c.get(Calendar.HOUR_OF_DAY);
            if (h>=prefs.getInt("start",8) && h<=prefs.getInt("end",20)) next=snooze;
        }
        prefs.edit().putLong("scheduled",next).apply();
        // Deliberately inexact: coaching is not a time-critical alarm.
        manager.set(AlarmManager.RTC_WAKEUP,next,pending);
    }
    public static void snooze(Context context) {
        CoachPreferences.get(context).edit().putLong("snooze",System.currentTimeMillis()+60*60*1000L).apply();
        schedule(context);
    }
}
