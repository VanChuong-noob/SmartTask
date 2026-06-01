package com.androidapp.SmartTask;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class SmartFeatureManager {



    private DatabaseHelper dbHelper;
    private SharedPreferences prefs;

    public SmartFeatureManager(Context context) {
        this.dbHelper = new DatabaseHelper(context);
        this.prefs = context.getSharedPreferences("SmartTask", Context.MODE_PRIVATE);
    }



    public void checkFirstOpenToday(String userEmail) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String lastOpen = prefs.getString("last_open_date", "");
        if (!today.equals(lastOpen)) {
            prefs.edit().putString("last_open_date", today).apply();
            updateStreak();
        }
    }

    public void checkEveningReminder(String userEmail) {
        // Không tự động hiện
    }

    public String getSmartSuggestion(String userEmail) {
        int pending = dbHelper.getPendingCount(userEmail);
        int completed = dbHelper.getCompletedCount(userEmail);
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        if (pending == 0 && completed == 0) return "Hay them cong viec moi de bat dau!";
        if (pending == 0) return "Chuc mung! Da hoan thanh het cong viec!";
        if (hour < 12) return "Buoi sang tot lanh! Con " + pending + " viec can lam.";
        if (hour < 18) return "Buoi chieu hieu qua! " + pending + " viec dang cho.";
        return "Sap het ngay! Con " + pending + " viec, co gang hoan thanh nhe!";
    }

    public int getStreak() {
        return prefs.getInt("streak", 0);
    }

    private void updateStreak() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String lastActive = prefs.getString("last_active_date", "");
        String yesterday = getYesterday();
        int streak = prefs.getInt("streak", 0);

        if (lastActive.equals(yesterday)) streak++;
        else if (!lastActive.equals(today)) streak = 1;

        prefs.edit().putInt("streak", streak).putString("last_active_date", today).apply();
    }

    private String getYesterday() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
    }
}