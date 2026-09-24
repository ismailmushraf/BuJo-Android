package com.ismailmushraf.bujo.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Looper;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.ismailmushraf.bujo.MainActivity;
import com.ismailmushraf.bujo.R;
import com.ismailmushraf.bujo.utils.FocusCheckReceiver;

import java.util.Locale;
import java.util.Random;

public class PomodoroService extends Service {

    private static final String CHANNEL_ID = "pomodoro_channel";
    private static final String ALERTS_CHANNEL_ID = "pomodoro_alerts";
    private static final int NOTIFICATION_ID = 1001;

    public interface OnTimerTickListener {
        void onTick(long millisUntilFinished, boolean isFocus);
        void onFinish();
    }

    private final IBinder binder = new PomodoroBinder();
    private OnTimerTickListener tickListener;
    
    private CountDownTimer countDownTimer;
    private long timeLeftInMillis;
    private int focusDuration = 25;
    private int breakDuration = 5;
    private boolean isFocusMode = true;
    private boolean isRunning = false;
    private boolean isPaused = false;
    private String currentTask = "Focus Session";

    private final Handler checkHandler = new Handler(Looper.getMainLooper());
    private final Handler penaltyHandler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    public class PomodoroBinder extends Binder {
        public PomodoroService getService() {
            return PomodoroService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        restoreState();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if ("STOP_PENALTY_TIMER".equals(action)) {
                penaltyHandler.removeCallbacksAndMessages(null);
            } else if ("SKIP_BREAK".equals(action)) {
                skipTimer();
            }
        }
        
        // If the service was killed and restarted, resume the timer if it was running
        if (isRunning && countDownTimer == null && !isPaused) {
            startTimer(timeLeftInMillis, isFocusMode, currentTask);
        }
        
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setTickListener(OnTimerTickListener listener) {
        this.tickListener = listener;
    }

    public void setDurations(int focusMins, int breakMins) {
        this.focusDuration = focusMins;
        this.breakDuration = breakMins;
        saveState();
    }

    public void startTimer(long millis, boolean isFocus, String task) {
        this.timeLeftInMillis = millis;
        this.isFocusMode = isFocus;
        this.currentTask = task;
        this.isRunning = true;
        this.isPaused = false;
        saveState();

        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateNotification();
                if (tickListener != null) tickListener.onTick(millisUntilFinished, isFocusMode);
                
                // Save state every 5 seconds to minimize loss on process kill
                if ((millisUntilFinished / 1000) % 5 == 0) {
                    saveState();
                }
            }

            @Override
            public void onFinish() {
                isRunning = false;
                isPaused = false;
                stopCheckLogic();

                if (isFocusMode) {
                    // Automatically transition to break
                    isFocusMode = false;
                    timeLeftInMillis = breakDuration * 60 * 1000L;
                    showCompletionNotification();
                    startTimer(timeLeftInMillis, false, currentTask);
                    if (tickListener != null) tickListener.onFinish();
                } else {
                    // Break finished, stop
                    isFocusMode = true; // Reset to focus for next time
                    timeLeftInMillis = 0;
                    saveState();
                    stopForeground(true);
                    showCompletionNotification();
                    if (tickListener != null) tickListener.onFinish();
                }
            }
        }.start();

        startForeground(NOTIFICATION_ID, getNotification());

        if (isFocusMode) {
            startCheckLogic();
        } else {
            stopCheckLogic();
        }
    }

    public void pauseTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        isRunning = false;
        isPaused = true;
        saveState();
        updateNotification();
        stopCheckLogic();
    }

    public void resumeTimer() {
        if (isPaused) {
            startTimer(timeLeftInMillis, isFocusMode, currentTask);
        }
    }

    public void stopTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        isRunning = false;
        isPaused = false;
        timeLeftInMillis = 0;
        saveState();
        stopForeground(true);
        stopCheckLogic();
    }

    public void skipTimer() {
        if (countDownTimer != null) countDownTimer.cancel();

        if (isFocusMode) {
            // Skip focus to break
            isFocusMode = false;
            timeLeftInMillis = breakDuration * 60 * 1000L;
            startTimer(timeLeftInMillis, false, currentTask);
        } else {
            // Skip break to focus
            isFocusMode = true;
            timeLeftInMillis = focusDuration * 60 * 1000L;
            isRunning = false;
            isPaused = false;
            saveState();
            stopForeground(true);
            stopCheckLogic();
            if (tickListener != null) {
                tickListener.onTick(timeLeftInMillis, isFocusMode);
                tickListener.onFinish();
            }
        }
    }

    private void startCheckLogic() {
        checkHandler.removeCallbacksAndMessages(null);
        
        // Engagement check ONLY for Focus sessions and if more than 3 mins remain
        if (isFocusMode && timeLeftInMillis > 3 * 60 * 1000) {
            long delay;
            if (timeLeftInMillis <= 6 * 60 * 1000) {
                // Short session (e.g. 5 min): trigger randomly between 1 min and 1 min before finish
                long maxDelay = timeLeftInMillis - (60 * 1000); // Leave at least 1 min buffer
                long minDelay = 60 * 1000;
                delay = minDelay + (long)(random.nextDouble() * (maxDelay - minDelay));
            } else {
                // Standard session: trigger between 5 and 15 minutes
                delay = (5 + random.nextInt(10)) * 60 * 1000L;
                // Ensure we don't schedule past the end
                if (delay > timeLeftInMillis - (60 * 1000)) {
                    delay = timeLeftInMillis / 2;
                }
            }
            checkHandler.postDelayed(this::sendEngagementCheck, delay);
        }
    }

    private void stopCheckLogic() {
        checkHandler.removeCallbacksAndMessages(null);
        penaltyHandler.removeCallbacksAndMessages(null);
    }

    private void sendEngagementCheck() {
        if (!isRunning || !isFocusMode) return;

        Intent respondIntent = new Intent(this, FocusCheckReceiver.class);
        respondIntent.setAction(FocusCheckReceiver.ACTION_RESPOND);
        PendingIntent respondPI = PendingIntent.getBroadcast(this, 0, respondIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, ALERTS_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(getString(R.string.engagement_check_title))
                .setContentText(getString(R.string.engagement_check_text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(true)
                .addAction(android.R.drawable.ic_input_add, getString(R.string.btn_im_here), respondPI);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(999, builder.build());
        }

        // Penalty after 3 minutes
        penaltyHandler.postDelayed(() -> {
            Intent penaltyIntent = new Intent(this, FocusCheckReceiver.class);
            penaltyIntent.setAction(FocusCheckReceiver.ACTION_PENALTY);
            sendBroadcast(penaltyIntent);
        }, 3 * 60 * 1000L);

        // Schedule next check if time remains
        startCheckLogic();
    }

    private void updateNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, getNotification());
        }
    }

    private void showCompletionNotification() {
        String title = isFocusMode ? "Focus Complete" : "Break Over";
        String text = isFocusMode ? "Time for a break!" : "Ready to focus again?";

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("NAVIGATE_TO", "Pomodoro");
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this, ALERTS_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_recent_history)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setDefaults(Notification.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(1002, notification);
        }
    }

    private Notification getNotification() {
        String mode = isFocusMode ? "FOCUS" : "BREAK";
        if (isPaused) mode = "PAUSED (" + mode + ")";
        long minutes = (timeLeftInMillis / 1000) / 60;
        long seconds = (timeLeftInMillis / 1000) % 60;
        String timeStr = String.format(Locale.US, "%02d:%02d", minutes, seconds);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.putExtra("NAVIGATE_TO", "Pomodoro");
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(mode + " - " + currentTask)
                .setContentText("Time remaining: " + timeStr)
                .setSmallIcon(android.R.drawable.ic_menu_recent_history)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true);

        if (!isFocusMode && isRunning) {
            Intent skipIntent = new Intent(this, PomodoroService.class);
            skipIntent.setAction("SKIP_BREAK");
            PendingIntent skipPI = PendingIntent.getService(this, 0, skipIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(android.R.drawable.ic_media_next, "SKIP", skipPI);
        }

        return builder.build();
    }

    private void saveState() {
        android.content.SharedPreferences prefs = getSharedPreferences("pomodoro_prefs", MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("time_left", timeLeftInMillis);
        editor.putInt("focus_duration", focusDuration);
        editor.putInt("break_duration", breakDuration);
        editor.putBoolean("is_focus", isFocusMode);
        editor.putBoolean("is_running", isRunning);
        editor.putBoolean("is_paused", isPaused);
        editor.putString("current_task", currentTask);
        editor.putLong("save_time", System.currentTimeMillis());
        editor.apply();
    }

    private void restoreState() {
        android.content.SharedPreferences prefs = getSharedPreferences("pomodoro_prefs", MODE_PRIVATE);
        isRunning = prefs.getBoolean("is_running", false);
        isPaused = prefs.getBoolean("is_paused", false);
        isFocusMode = prefs.getBoolean("is_focus", true);
        focusDuration = prefs.getInt("focus_duration", 25);
        breakDuration = prefs.getInt("break_duration", 5);
        currentTask = prefs.getString("current_task", "Focus Session");
        
        long savedTimeLeft = prefs.getLong("time_left", 0);
        long saveTime = prefs.getLong("save_time", 0);
        
        if (isRunning && !isPaused && saveTime > 0) {
            long timePassed = System.currentTimeMillis() - saveTime;
            timeLeftInMillis = savedTimeLeft - timePassed;
            if (timeLeftInMillis <= 0) {
                isRunning = false;
                timeLeftInMillis = 0;
            }
        } else {
            timeLeftInMillis = savedTimeLeft;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Pomodoro Timer Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationChannel alertsChannel = new NotificationChannel(
                    ALERTS_CHANNEL_ID,
                    "Pomodoro Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            alertsChannel.setDescription("High-priority alerts for session completion and focus checks.");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
                manager.createNotificationChannel(alertsChannel);
            }
        }
    }

    public boolean isRunning() { return isRunning; }
    public boolean isPaused() { return isPaused; }
    public boolean isFocusMode() { return isFocusMode; }
    public long getTimeLeft() { return timeLeftInMillis; }
    public String getCurrentTask() { return currentTask; }
    public int getFocusDuration() { return focusDuration; }
    public int getBreakDuration() { return breakDuration; }

    @Override
    public void onDestroy() {
        stopCheckLogic();
        if (countDownTimer != null) countDownTimer.cancel();
        super.onDestroy();
    }
}
