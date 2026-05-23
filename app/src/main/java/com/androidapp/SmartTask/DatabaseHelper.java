package com.androidapp.SmartTask;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {

    public DatabaseHelper(Context context) {
        super(context, "SmartTaskDB", null, 3);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users(id INTEGER PRIMARY KEY AUTOINCREMENT, email TEXT UNIQUE, password TEXT, name TEXT)");
        db.execSQL("CREATE TABLE tasks(id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, description TEXT, time TEXT, date TEXT, completed INTEGER DEFAULT 0, user_email TEXT, location_name TEXT, location_lat REAL DEFAULT 0, location_lng REAL DEFAULT 0, location_reminder INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE task_history(id INTEGER PRIMARY KEY AUTOINCREMENT, user_email TEXT, task_date TEXT, completed_count INTEGER DEFAULT 0, pending_count INTEGER DEFAULT 0, total_count INTEGER DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE tasks ADD COLUMN location_name TEXT DEFAULT ''");
            db.execSQL("ALTER TABLE tasks ADD COLUMN location_lat REAL DEFAULT 0");
            db.execSQL("ALTER TABLE tasks ADD COLUMN location_lng REAL DEFAULT 0");
            db.execSQL("ALTER TABLE tasks ADD COLUMN location_reminder INTEGER DEFAULT 0");
        }
        if (oldVersion < 3) {
            db.execSQL("CREATE TABLE IF NOT EXISTS task_history(id INTEGER PRIMARY KEY AUTOINCREMENT, user_email TEXT, task_date TEXT UNIQUE, completed_count INTEGER DEFAULT 0, pending_count INTEGER DEFAULT 0, total_count INTEGER DEFAULT 0)");
        }
    }

    // ============ USER METHODS ============
    public boolean registerUser(String email, String password, String name) {
        ContentValues cv = new ContentValues();
        cv.put("email", email);
        cv.put("password", password);
        cv.put("name", name);
        return getWritableDatabase().insert("users", null, cv) != -1;
    }

    public boolean checkUser(String email, String password) {
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM users WHERE email=? AND password=?", new String[]{email, password});
        boolean exists = c.getCount() > 0;
        c.close();
        return exists;
    }

    public boolean checkEmailExists(String email) {
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM users WHERE email=?", new String[]{email});
        boolean exists = c.getCount() > 0;
        c.close();
        return exists;
    }

    public String getUserName(String email) {
        Cursor c = getReadableDatabase().rawQuery("SELECT name FROM users WHERE email=?", new String[]{email});
        String name = "";
        if (c.moveToFirst()) name = c.getString(0);
        c.close();
        return name;
    }

    // ============ TASK CRUD ============
    public long addTask(Task task) {
        ContentValues cv = new ContentValues();
        cv.put("title", task.getTitle());
        cv.put("description", task.getDescription());
        cv.put("time", task.getTime());
        cv.put("date", task.getDate());
        cv.put("completed", task.isCompleted() ? 1 : 0);
        cv.put("user_email", task.getUserEmail());
        cv.put("location_name", task.getLocationName());
        cv.put("location_lat", task.getLocationLat());
        cv.put("location_lng", task.getLocationLng());
        cv.put("location_reminder", task.isLocationReminder() ? 1 : 0);
        long id = getWritableDatabase().insert("tasks", null, cv);
        saveDailyHistory(task.getUserEmail());
        return id;
    }

    public List<Task> getAllTasks(String userEmail) {
        List<Task> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM tasks WHERE user_email=? ORDER BY completed ASC, id DESC", new String[]{userEmail});
        if (c.moveToFirst()) {
            do {
                Task t = new Task();
                t.setId(c.getInt(0));
                t.setTitle(c.getString(1));
                t.setDescription(c.getString(2));
                t.setTime(c.getString(3));
                t.setDate(c.getString(4));
                t.setCompleted(c.getInt(5) == 1);
                t.setUserEmail(c.getString(6));
                t.setLocationName(c.getString(7));
                t.setLocationLat(c.getDouble(8));
                t.setLocationLng(c.getDouble(9));
                t.setLocationReminder(c.getInt(10) == 1);
                list.add(t);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public List<Task> getLocationReminderTasks(String userEmail) {
        List<Task> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM tasks WHERE user_email=? AND location_reminder=1 AND completed=0", new String[]{userEmail});
        if (c.moveToFirst()) {
            do {
                Task t = new Task();
                t.setId(c.getInt(0));
                t.setTitle(c.getString(1));
                t.setDescription(c.getString(2));
                t.setTime(c.getString(3));
                t.setDate(c.getString(4));
                t.setCompleted(c.getInt(5) == 1);
                t.setUserEmail(c.getString(6));
                t.setLocationName(c.getString(7));
                t.setLocationLat(c.getDouble(8));
                t.setLocationLng(c.getDouble(9));
                t.setLocationReminder(c.getInt(10) == 1);
                list.add(t);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public int updateTask(Task task) {
        ContentValues cv = new ContentValues();
        cv.put("title", task.getTitle());
        cv.put("description", task.getDescription());
        cv.put("time", task.getTime());
        cv.put("date", task.getDate());
        cv.put("completed", task.isCompleted() ? 1 : 0);
        cv.put("location_name", task.getLocationName());
        cv.put("location_lat", task.getLocationLat());
        cv.put("location_lng", task.getLocationLng());
        cv.put("location_reminder", task.isLocationReminder() ? 1 : 0);
        int rows = getWritableDatabase().update("tasks", cv, "id=?", new String[]{String.valueOf(task.getId())});
        saveDailyHistory(task.getUserEmail());
        return rows;
    }

    public void toggleTaskComplete(int taskId, boolean completed) {
        ContentValues cv = new ContentValues();
        cv.put("completed", completed ? 1 : 0);
        getWritableDatabase().update("tasks", cv, "id=?", new String[]{String.valueOf(taskId)});
    }

    public int deleteTask(int taskId) {
        int rows = getWritableDatabase().delete("tasks", "id=?", new String[]{String.valueOf(taskId)});
        return rows;
    }

    public int deleteCompletedTasks(String userEmail) {
        int rows = getWritableDatabase().delete("tasks", "user_email=? AND completed=?", new String[]{userEmail, "1"});
        saveDailyHistory(userEmail);
        return rows;
    }

    // ============ STATS ============
    public int getCompletedCount(String userEmail) {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM tasks WHERE user_email=? AND completed=1", new String[]{userEmail});
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    public int getPendingCount(String userEmail) {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM tasks WHERE user_email=? AND completed=0", new String[]{userEmail});
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    // ============ DAILY HISTORY ============
    private void saveDailyHistory(String userEmail) {
        String today = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        int completed = getCompletedCount(userEmail);
        int pending = getPendingCount(userEmail);
        int total = completed + pending;

        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("user_email", userEmail);
        cv.put("task_date", today);
        cv.put("completed_count", completed);
        cv.put("pending_count", pending);
        cv.put("total_count", total);

        Cursor c = db.rawQuery("SELECT id FROM task_history WHERE user_email=? AND task_date=?", new String[]{userEmail, today});
        if (c.moveToFirst()) {
            db.update("task_history", cv, "user_email=? AND task_date=?", new String[]{userEmail, today});
        } else {
            db.insert("task_history", null, cv);
        }
        c.close();
    }

    public Map<String, Integer> getLast7DaysStats(String userEmail) {
        Map<String, Integer> stats = new LinkedHashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Calendar cal = Calendar.getInstance();

        for (int i = 6; i >= 0; i--) {
            cal.add(Calendar.DATE, -i);
            String date = sdf.format(cal.getTime());
            stats.put(date, 0);
            cal = Calendar.getInstance();
        }

        Cursor c = getReadableDatabase().rawQuery(
                "SELECT task_date, completed_count FROM task_history WHERE user_email=? ORDER BY task_date ASC",
                new String[]{userEmail});
        if (c.moveToFirst()) {
            do {
                String date = c.getString(0);
                int count = c.getInt(1);
                if (stats.containsKey(date)) {
                    stats.put(date, count);
                }
            } while (c.moveToNext());
        }
        c.close();

        // Nếu hôm nay chưa có trong history, lấy từ tasks hiện tại
        String today = sdf.format(new Date());
        if (!stats.containsKey(today) || stats.get(today) == 0) {
            stats.put(today, getCompletedCount(userEmail));
        }

        return stats;
    }

    public int getTotalCompletedAllTime(String userEmail) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT SUM(completed_count) FROM task_history WHERE user_email=?",
                new String[]{userEmail});
        int total = 0;
        if (c.moveToFirst()) total = c.getInt(0);
        c.close();
        return total;
    }

    // Helper class
    private static class LinkedHashMap<K, V> extends java.util.LinkedHashMap<K, V> {
        // Just to make LinkedHashMap accessible
    }
}