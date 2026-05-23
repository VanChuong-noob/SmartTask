package com.androidapp.SmartTask;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LocationService extends Service {

    private static final String TAG = "LocationService";
    private static final String CHANNEL_ID = "location_service_channel";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private DatabaseHelper dbHelper;
    private String currentUser;

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        dbHelper = new DatabaseHelper(this);
        startForegroundNotification();
        Log.d(TAG, "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            currentUser = intent.getStringExtra("user_email");
        }
        startLocationUpdates();
        Log.d(TAG, "Service started");
        return START_STICKY;
    }

    private void startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SmartTask GPS",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Dang theo doi vi tri");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SmartTask GPS")
                .setContentText("Dang theo doi vi tri de nhac nho...")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pi)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        startForeground(1001, notification);
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 30000)
                .setMinUpdateIntervalMillis(15000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                Location location = locationResult.getLastLocation();
                if (location != null && currentUser != null) {
                    checkNearbyTasksWithTime(location);
                }
            }
        };

        try {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied", e);
        }
    }

    private void checkNearbyTasksWithTime(Location currentLocation) {
        List<Task> tasks = dbHelper.getLocationReminderTasks(currentUser);
        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        int currentMinute = now.get(Calendar.MINUTE);

        for (Task task : tasks) {
            if (!task.isCompleted() && task.isLocationReminder() && !task.getLocationName().isEmpty()) {

                // Check GPS: trong bán kính 200m
                double distance = calculateDistance(
                        currentLocation.getLatitude(), currentLocation.getLongitude(),
                        task.getLocationLat(), task.getLocationLng());

                if (distance < 200) {
                    // Check Time: gần đến giờ hẹn (trong vòng 15 phút tới)
                    boolean nearTime = isNearTaskTime(task.getTime(), currentHour, currentMinute);

                    if (nearTime) {
                        showLocationNotification(task);
                        task.setLocationReminder(false);
                        dbHelper.updateTask(task);
                    }
                }
            }
        }
    }

    private boolean isNearTaskTime(String taskTime, int currentHour, int currentMinute) {
        if (taskTime == null || taskTime.isEmpty() || taskTime.equals("Chua dat gio")) {
            return false;
        }

        try {
            // Parse time từ format "09:15 AM"
            String[] parts = taskTime.split(" ");
            if (parts.length < 2) return false;

            String[] timeParts = parts[0].split(":");
            int taskHour = Integer.parseInt(timeParts[0]);
            int taskMinute = Integer.parseInt(timeParts[1]);
            String amPm = parts[1];

            // Chuyển sang 24h
            if (amPm.equals("PM") && taskHour < 12) taskHour += 12;
            if (amPm.equals("AM") && taskHour == 12) taskHour = 0;

            // Tính số phút từ 0h
            int taskTotalMinutes = taskHour * 60 + taskMinute;
            int currentTotalMinutes = currentHour * 60 + currentMinute;

            // Check nếu hiện tại đang trong khoảng 15 phút TRƯỚC giờ hẹn
            int diff = taskTotalMinutes - currentTotalMinutes;

            // Nếu còn 1-15 phút nữa là đến giờ
            return diff >= 1 && diff <= 15;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing time: " + taskTime, e);
            return false;
        }
    }

    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private void showLocationNotification(Task task) {
        String channelId = "location_reminder";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "Nhac vi tri", NotificationManager.IMPORTANCE_HIGH);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pi = PendingIntent.getActivity(this, task.getId(), openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle("📍 Ban dang o gan: " + task.getLocationName())
                .setContentText("⏰ Sap den gio: " + task.getTime() + " - " + task.getTitle())
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("📍 Vi tri: " + task.getLocationName()
                                + "\n⏰ Gio: " + task.getTime()
                                + "\n📋 Task: " + task.getTitle()
                                + "\n\nHay chuan bi hoan thanh!"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 500, 200, 500});

        NotificationManagerCompat.from(this).notify(task.getId() + 10000, builder.build());
        Log.d(TAG, "Location+Time notification: " + task.getTitle());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        Log.d(TAG, "Service destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}