package com.ismailmushraf.bujo.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.ismailmushraf.bujo.MainActivity;
import com.ismailmushraf.bujo.R;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String taskContent = intent.getStringExtra("TASK_CONTENT");
        int notificationId = intent.getIntExtra("TASK_ID", (int) System.currentTimeMillis());

        // Tapping the notification opens the app
        Intent mainIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "bujo_channel")
                // FIX 1: Use a safe, legacy system icon that BB10 understands
                .setSmallIcon(android.R.drawable.ic_menu_agenda)
                .setContentTitle("BuJo Reminder")
                .setContentText(taskContent)
                // FIX 2: Ticker text is strictly required to show up in the BlackBerry Hub
                .setTicker("BuJo: " + taskContent)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(notificationId, builder.build());
        }
    }
}