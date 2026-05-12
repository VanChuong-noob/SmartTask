package com.androidapp.SmartTask;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "===== ALARM RECEIVED =====");

        String taskTitle = intent.getStringExtra("task_title");
        String taskTime = intent.getStringExtra("task_time");
        int taskId = intent.getIntExtra("task_id", -1);

        Log.d(TAG, "Task: " + taskTitle + " - Time: " + taskTime + " - ID: " + taskId);

        if (taskTitle == null) taskTitle = "Task Reminder";
        if (taskTime == null) taskTime = "";

        // Wake up device nếu đang sleep
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = null;
        if (pm != null) {
            wakeLock = pm.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "SmartTask:AlarmWakeLock"
            );
            wakeLock.acquire(10 * 1000); // 10 seconds
        }

        showNotification(context, taskTitle, taskTime, taskId);

        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    private void showNotification(Context context, String title, String time, int taskId) {
        String channelId = "task_alarm_channel";

        // Tạo notification channel
        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Task Alarm",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Task reminder notifications");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            channel.setShowBadge(true);
            manager.createNotificationChannel(channel);
        }

        // Intent mở app khi click notification
        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        openIntent.putExtra("from_notification", true);
        openIntent.putExtra("task_id", taskId);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                taskId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Intent đánh dấu hoàn thành
        Intent completeIntent = new Intent(context, AlarmReceiver.class);
        completeIntent.setAction("COMPLETE_TASK");
        completeIntent.putExtra("task_id", taskId);
        PendingIntent completePendingIntent = PendingIntent.getBroadcast(
                context,
                taskId + 10000,
                completeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("⏰ " + title)
                .setContentText("Đến giờ: " + time + ". Hoàn thành ngay!")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("📋 Task: " + title + "\n⏰ Giờ: " + time + "\n\nChạm để mở app hoặc chạm 'Hoàn thành' để đánh dấu xong."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.ic_menu_edit, "Hoàn thành", completePendingIntent)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 500, 200, 500})
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setOngoing(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        // Hiển thị notification với ID duy nhất
        int notificationId = (taskId != -1) ? taskId : (int) System.currentTimeMillis();
        manager.notify(notificationId, builder.build());

        Log.d(TAG, "Notification shown with ID: " + notificationId);
    }
}