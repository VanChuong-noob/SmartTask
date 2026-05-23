package com.androidapp.SmartTask;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AchievementManager {

    private Context context;
    private DatabaseHelper dbHelper;
    private SharedPreferences prefs;

    public AchievementManager(Context context) {
        this.context = context;
        this.dbHelper = new DatabaseHelper(context);
        this.prefs = context.getSharedPreferences("SmartTask", Context.MODE_PRIVATE);
    }

    public List<Achievement> getAllAchievements(String userEmail) {
        List<Achievement> list = new ArrayList<>();
        int totalCompleted = getTotalCompleted(userEmail);
        int streak = prefs.getInt("streak", 0);
        int todayCompleted = getTodayCompleted(userEmail);

        Achievement a1 = new Achievement("🥉", "Beginner", "Hoan thanh 10 cong viec", "total");
        a1.setUnlocked(totalCompleted >= 10);
        list.add(a1);

        Achievement a2 = new Achievement("🥈", "Hard Worker", "Hoan thanh 50 cong viec", "total");
        a2.setUnlocked(totalCompleted >= 50);
        list.add(a2);

        Achievement a3 = new Achievement("🥇", "Task Master", "Hoan thanh 100 cong viec", "total");
        a3.setUnlocked(totalCompleted >= 100);
        list.add(a3);

        Achievement a4 = new Achievement("👑", "Legend", "Hoan thanh 500 cong viec", "total");
        a4.setUnlocked(totalCompleted >= 500);
        list.add(a4);

        Achievement a5 = new Achievement("🔥", "3 Day Streak", "Duy tri streak 3 ngay", "streak");
        a5.setUnlocked(streak >= 3);
        list.add(a5);

        Achievement a6 = new Achievement("💪", "Week Warrior", "Duy tri streak 7 ngay", "streak");
        a6.setUnlocked(streak >= 7);
        list.add(a6);

        Achievement a7 = new Achievement("🌟", "Monthly Master", "Duy tri streak 30 ngay", "streak");
        a7.setUnlocked(streak >= 30);
        list.add(a7);

        Achievement a8 = new Achievement("✨", "Perfect Day", "Hoan thanh het task trong ngay", "daily");
        a8.setUnlocked(todayCompleted > 0 && dbHelper.getPendingCount(userEmail) == 0);
        list.add(a8);

        Achievement a9 = new Achievement("🌅", "Early Bird", "Hoan thanh task truoc 8h sang", "early");
        a9.setUnlocked(isEarlyBird(userEmail));
        list.add(a9);

        Achievement a10 = new Achievement("🦉", "Night Owl", "Hoan thanh task sau 10h toi", "night");
        a10.setUnlocked(isNightOwl(userEmail));
        list.add(a10);

        return list;
    }

    public int getUnlockedCount(String userEmail) {
        int count = 0;
        List<Achievement> list = getAllAchievements(userEmail);
        for (Achievement a : list) {
            if (a.isUnlocked()) {
                count++;
            }
        }
        return count;
    }

    public Achievement checkNewAchievement(String userEmail) {
        List<Achievement> list = getAllAchievements(userEmail);
        for (Achievement a : list) {
            if (a.isUnlocked() && !isAchievementNotified(a.getType())) {
                markAchievementNotified(a.getType());
                return a;
            }
        }
        return null;
    }

    private int getTotalCompleted(String userEmail) {
        return dbHelper.getCompletedCount(userEmail) + prefs.getInt("lifetime_completed", 0);
    }

    private int getTodayCompleted(String userEmail) {
        String today = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        int count = 0;
        List<Task> tasks = dbHelper.getAllTasks(userEmail);
        for (Task t : tasks) {
            if (t.isCompleted() && t.getDate().equals(today)) {
                count++;
            }
        }
        return count;
    }

    private boolean isEarlyBird(String userEmail) {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        String today = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        List<Task> tasks = dbHelper.getAllTasks(userEmail);
        for (Task t : tasks) {
            if (t.isCompleted() && t.getDate().equals(today) && hour < 8) {
                return true;
            }
        }
        return false;
    }

    private boolean isNightOwl(String userEmail) {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        String today = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        List<Task> tasks = dbHelper.getAllTasks(userEmail);
        for (Task t : tasks) {
            if (t.isCompleted() && t.getDate().equals(today) && hour >= 22) {
                return true;
            }
        }
        return false;
    }

    public void updateLifetimeStats(String userEmail) {
        int currentCompleted = dbHelper.getCompletedCount(userEmail);
        int lifetime = prefs.getInt("lifetime_completed", 0);
        if (currentCompleted > lifetime) {
            prefs.edit().putInt("lifetime_completed", currentCompleted).apply();
        }
    }

    private boolean isAchievementNotified(String type) {
        return prefs.getBoolean("achievement_" + type, false);
    }

    private void markAchievementNotified(String type) {
        prefs.edit().putBoolean("achievement_" + type, true).apply();
    }
}