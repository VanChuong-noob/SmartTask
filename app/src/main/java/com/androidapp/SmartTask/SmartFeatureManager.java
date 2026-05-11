package com.androidapp.SmartTask;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SmartFeatureManager {

    private Context context;
    private DatabaseHelper dbHelper;
    private NotificationHelper notificationHelper;
    private SharedPreferences prefs;

    public SmartFeatureManager(Context context) {
        this.context = context;
        this.dbHelper = new DatabaseHelper(context);
        this.notificationHelper = new NotificationHelper(context);
        this.prefs = context.getSharedPreferences("SmartTask", Context.MODE_PRIVATE);
    }

    // Check lần mở app đầu tiên trong ngày
    public void checkFirstOpenToday(String userEmail) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String lastOpenDate = prefs.getString("last_open_date", "");

        if (!today.equals(lastOpenDate)) {
            // Update last open date
            prefs.edit().putString("last_open_date", today).apply();

            // Update streak
            updateStreak();

            // Lấy tên user
            String userName = dbHelper.getUserName(userEmail);

            // Tìm task có thời gian buổi sáng
            List<Task> morningTasks = getMorningTasks(userEmail);

            // Gửi notification buổi sáng
            notificationHelper.showMorningReminder(userName);

            // Nhắc từng task buổi sáng
            for (Task task : morningTasks) {
                if (!task.isCompleted()) {
                    notificationHelper.showTaskReminder(task.getTitle(), task.getTime());
                    break; // Chỉ nhắc 1 task
                }
            }

            // Check streak
            checkStreak();
        }
    }

    // Check nhắc việc buổi tối
    public void checkEveningReminder(String userEmail) {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);

        // Nhắc lúc 20:00 - 20:05
        if (hour == 20 && minute >= 0 && minute <= 5) {
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            String lastEveningReminder = prefs.getString("last_evening_reminder", "");

            if (!today.equals(lastEveningReminder)) {
                prefs.edit().putString("last_evening_reminder", today).apply();

                int pendingCount = dbHelper.getPendingCount(userEmail);
                if (pendingCount > 0) {
                    notificationHelper.showEveningReminder(pendingCount);
                }
            }
        }
    }

    // Check task quá hạn
    public void checkOverdueTasks(String userEmail) {
        List<Task> allTasks = dbHelper.getAllTasks(userEmail);
        String today = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

        for (Task task : allTasks) {
            if (!task.isCompleted() && !task.getDate().isEmpty()) {
                // So sánh ngày (đơn giản)
                if (isDatePast(task.getDate(), today)) {
                    // Task quá hạn - có thể thêm logic đánh dấu
                }
            }
        }
    }

    // Lấy task buổi sáng (có thời gian từ 5:00 - 12:00)
    private List<Task> getMorningTasks(String userEmail) {
        List<Task> allTasks = dbHelper.getAllTasks(userEmail);
        List<Task> morningTasks = new java.util.ArrayList<>();

        for (Task task : allTasks) {
            if (!task.isCompleted()) {
                String time = task.getTime();
                // Parse giờ từ string "09:00 AM"
                if (time.contains("AM") || isMorningHour(time)) {
                    morningTasks.add(task);
                }
            }
        }
        return morningTasks;
    }

    private boolean isMorningHour(String timeStr) {
        try {
            // Đơn giản: check nếu giờ từ 5-12
            String[] parts = timeStr.split(":");
            if (parts.length > 0) {
                int hour = Integer.parseInt(parts[0].trim());
                return hour >= 5 && hour <= 12;
            }
        } catch (Exception e) {
            // Bỏ qua
        }
        return false;
    }

    // So sánh ngày đơn giản
    private boolean isDatePast(String taskDate, String today) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date taskD = sdf.parse(taskDate);
            Date todayD = sdf.parse(today);
            return taskD != null && taskD.before(todayD);
        } catch (Exception e) {
            return false;
        }
    }

    // Streak system
    private void updateStreak() {
        int streak = prefs.getInt("streak", 0);
        String lastActiveDate = prefs.getString("last_active_date", "");
        String yesterday = getYesterdayDate();

        if (lastActiveDate.equals(yesterday)) {
            // Tiếp tục streak
            streak++;
        } else if (!lastActiveDate.equals(getTodayDate())) {
            // Reset streak nếu bỏ lỡ 1 ngày
            streak = 1;
        }

        prefs.edit()
                .putInt("streak", streak)
                .putString("last_active_date", getTodayDate())
                .apply();
    }

    private void checkStreak() {
        int streak = prefs.getInt("streak", 0);
        if (streak > 0 && streak % 5 == 0) {
            // Mỗi 5 ngày streak sẽ có thông báo đặc biệt
            notificationHelper.showStreakReminder(streak);
        }
    }

    public int getStreak() {
        return prefs.getInt("streak", 0);
    }

    public String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private String getYesterdayDate() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
    }

    // Gợi ý task thông minh dựa trên thói quen
    public String getSmartSuggestion(String userEmail) {
        List<Task> allTasks = dbHelper.getAllTasks(userEmail);
        int completedCount = dbHelper.getCompletedCount(userEmail);
        int pendingCount = dbHelper.getPendingCount(userEmail);
        int totalCount = dbHelper.getTotalCount(userEmail);

        if (totalCount == 0) {
            return "Hay them cong viec moi de bat dau!";
        }

        if (pendingCount == 0 && completedCount > 0) {
            return "Chuc mung! Ban da hoan thanh tat ca cong viec!";
        }

        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        if (hour < 12) {
            return "Buoi sang vui ve! Ban co " + pendingCount + " viec can lam hom nay.";
        } else if (hour < 18) {
            return "Buoi chieu lam viec hieu qua nhe! " + pendingCount + " viec dang cho.";
        } else {
            if (pendingCount > 3) {
                return "Con nhieu viec qua! Hay uu tien nhung task quan trong truoc.";
            } else {
                return "Sap het ngay roi! Co gang hoan thanh not nhe!";
            }
        }
    }
}