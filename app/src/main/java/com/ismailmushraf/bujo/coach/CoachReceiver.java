package com.ismailmushraf.bujo.coach;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.ismailmushraf.bujo.MainActivity;
import com.ismailmushraf.bujo.R;
import java.util.Calendar;

public final class CoachReceiver extends BroadcastReceiver {
    public static final int NOTIFICATION_ID=71002;
    @Override public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs=CoachPreferences.get(context);
        if (!"com.ismailmushraf.bujo.COACH_CHECKIN".equals(intent.getAction())) {
            CoachScheduler.schedule(context); return;
        }
        long now=System.currentTimeMillis(), expected=prefs.getLong("scheduled",0);
        Calendar time=Calendar.getInstance();
        int hour=time.get(Calendar.HOUR_OF_DAY);
        boolean quiet=hour<prefs.getInt("start",8) || hour>prefs.getInt("end",20);
        boolean recent=now-prefs.getLong("last_checkin",0)<60*60*1000L;
        SharedPreferences timer=context.getSharedPreferences("pomodoro_prefs",Context.MODE_PRIVATE);
        boolean focus=timer.getBoolean("is_running",false) && !timer.getBoolean("is_paused",false)
                && timer.getBoolean("is_focus",false)
                && now-timer.getLong("save_time",0)<timer.getLong("time_left",0);
        if (prefs.getBoolean("enabled",false) && !quiet && !recent &&
                expected>0 && now>=expected && now-expected<2*60*60*1000L &&
                now-prefs.getLong("last_notification",0)>60*60*1000L) {
            if (focus) { CoachScheduler.snooze(context); return; }
            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                NotificationManager manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (manager!=null) {
                    if (Build.VERSION.SDK_INT>=26) manager.createNotificationChannel(new NotificationChannel(
                            "bujo_coach",context.getString(R.string.coach_title),NotificationManager.IMPORTANCE_DEFAULT));
                    Intent open=new Intent(context,MainActivity.class).putExtra("NAVIGATE_TO","Coach")
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    PendingIntent pending=PendingIntent.getActivity(context,71003,open,PendingIntent.FLAG_UPDATE_CURRENT |
                            (Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0));
                    try {
                        manager.notify(NOTIFICATION_ID,new NotificationCompat.Builder(context,"bujo_coach")
                                .setSmallIcon(android.R.drawable.ic_menu_agenda)
                                .setContentTitle(context.getString(R.string.coach_title))
                                .setContentText(context.getString(R.string.coach_notification))
                                .setTicker(context.getString(R.string.coach_notification))
                                .setContentIntent(pending).setAutoCancel(true).build());
                        prefs.edit().putLong("last_notification",now).apply();
                    } catch (SecurityException ignored) { /* Notification permission may change mid-delivery. */ }
                }
            }
        }
        prefs.edit().remove("snooze").apply();
        CoachScheduler.schedule(context);
    }
}
