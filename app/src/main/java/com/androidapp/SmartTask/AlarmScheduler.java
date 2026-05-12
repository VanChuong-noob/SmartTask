package com.androidapp.SmartTask;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;

public class AlarmScheduler {

    private final Context context;
    private static final String TAG = "AlarmScheduler";

    public AlarmScheduler(Context context) {
        this.context = context.getApplicationContext(); // Dùng application context
    }

    public void scheduleTaskReminder(int taskId, String taskTitle, int hour, int minute) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction("TASK_REMINDER_" + taskId);
        intent.putExtra("task_title", taskTitle);
        intent.putExtra("task_time", String.format("%02d:%02d", hour, minute));
        intent.putExtra("task_id", taskId);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);

        // Dùng flag quan trọng để đảm bảo PendingIntent luôn mới
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId,
                intent,
                flags
        );

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // Nếu giờ đã qua, đặt cho ngày mai
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        long triggerTime = calendar.getTimeInMillis();
        Log.d(TAG, "Setting alarm for: " + taskTitle + " at " + triggerTime);

        try {
            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Android 12+ cần check quyền
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                triggerTime,
                                pendingIntent
                        );
                    } else {
                        // Fallback: dùng setAlarmClock (luôn được phép)
                        alarmManager.setAlarmClock(
                                new AlarmManager.AlarmClockInfo(triggerTime, pendingIntent),
                                pendingIntent
                        );
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                    );
                } else {
                    alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                    );
                }
                Log.d(TAG, "Alarm set successfully!");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for exact alarm", e);
            // Fallback: dùng set()
            if (alarmManager != null) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
        }
    }

    public void scheduleMorningReminder(int hour, int minute) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction("MORNING_REMINDER");
        intent.putExtra("task_title", "Chào buổi sáng! Hãy kiểm tra công việc hôm nay ☀️");
        intent.putExtra("task_time", String.format("%02d:%02d", hour, minute));
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                9999,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        long triggerTime = calendar.getTimeInMillis();

        try {
            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    alarmManager.setAlarmClock(
                            new AlarmManager.AlarmClockInfo(triggerTime, pendingIntent),
                            pendingIntent
                    );
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                    );
                }
            }
            Log.d(TAG, "Morning reminder set for: " + triggerTime);
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to set morning reminder", e);
        }
    }

    public void cancelTaskReminder(int taskId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction("TASK_REMINDER_" + taskId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            Log.d(TAG, "Cancelled alarm for task: " + taskId);
        }
    }

    // Test alarm trong 10 giây
    public void scheduleTestAlarm(String taskTitle) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction("TEST_ALARM");
        intent.putExtra("task_title", taskTitle);
        intent.putExtra("task_time", "TEST");
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                7777,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Set alarm 10 giây sau
        long triggerTime = System.currentTimeMillis() + 10 * 1000;

        try {
            if (alarmManager != null) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
                Log.d(TAG, "Test alarm set for 10 seconds later");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to set test alarm", e);
        }
    }
}