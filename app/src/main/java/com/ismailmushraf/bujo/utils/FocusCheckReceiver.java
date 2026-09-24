package com.ismailmushraf.bujo.utils;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.ismailmushraf.bujo.MainActivity;
import com.ismailmushraf.bujo.db.DatabaseManager;

public class FocusCheckReceiver extends BroadcastReceiver {
    public static final String ACTION_RESPOND = "com.ismailmushraf.bujo.ACTION_RESPOND";
    public static final String ACTION_PENALTY = "com.ismailmushraf.bujo.ACTION_PENALTY";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(999); // Cancel the "Are you there?" notification
        }

        if (ACTION_RESPOND.equals(action)) {
            // User responded in time
            Toast.makeText(context, "Focus confirmed!", Toast.LENGTH_SHORT).show();
            
            // Stop the penalty timer in the service if it's still running
            Intent stopPenaltyIntent = new Intent(context, com.ismailmushraf.bujo.services.PomodoroService.class);
            stopPenaltyIntent.setAction("STOP_PENALTY_TIMER");
            context.startService(stopPenaltyIntent);

        } else if (ACTION_PENALTY.equals(action)) {
            // 3 minutes passed without response
            DatabaseManager dbManager = new DatabaseManager(context);
            dbManager.open();
            dbManager.adjustPoints(-5, DatabaseManager.CAT_TASKS);
            dbManager.close();

            Toast.makeText(context, "Penalty: -5 points for inactivity.", Toast.LENGTH_LONG).show();
            
            // Notification to inform the user
            android.support.v4.app.NotificationCompat.Builder builder = new android.support.v4.app.NotificationCompat.Builder(context, "pomodoro_alerts")
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setContentTitle("Focus Lost")
                    .setContentText("Penalty of -5 points applied for missing focus check.")
                    .setPriority(android.support.v4.app.NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(android.app.Notification.DEFAULT_ALL)
                    .setAutoCancel(true);

            if (manager != null) {
                manager.notify(998, builder.build());
            }
        }
    }
}
