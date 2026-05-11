package com.androidapp.SmartTask;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Đặt lại buổi sáng reminder
            AlarmScheduler scheduler = new AlarmScheduler(context);
            scheduler.scheduleMorningReminder(8, 0);

            // Load tất cả task từ database và đặt lại alarm
            DatabaseHelper dbHelper = new DatabaseHelper(context);
            // Logic đặt lại tất cả alarm cho các task chưa hoàn thành
        }
    }
}