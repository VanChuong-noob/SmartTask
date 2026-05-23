package com.androidapp.SmartTask;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "===== ALARM TRIGGERED =====");

        String title = intent.getStringExtra("task_title");
        String time = intent.getStringExtra("task_time");
        int taskId = intent.getIntExtra("task_id", -1);

        if (title == null) title = "Nhac nho cong viec";
        if (time == null) time = "";

        showNotification(context, title, time, taskId);
    }

    private void showNotification(Context context, String title, String time, int taskId) {
        String channelId = "task_alarm_channel";

        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Nhac nho cong viec",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Thong bao nhac viec");
            channel.enableVibration(true);
            manager.createNotificationChannel(channel);
        }

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, taskId, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("⏰ " + title)
                .setContentText("Den gio: " + time + ". Hay hoan thanh nhe!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 500, 200, 500});

        manager.notify(taskId != -1 ? taskId : (int) System.currentTimeMillis(), builder.build());
        Log.d(TAG, "Notification shown: " + title);
    }
}