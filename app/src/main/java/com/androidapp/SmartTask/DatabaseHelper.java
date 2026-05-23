package com.androidapp.SmartTask;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    public DatabaseHelper(Context context) {
        super(context, "SmartTaskDB", null, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users(id INTEGER PRIMARY KEY AUTOINCREMENT, email TEXT UNIQUE, password TEXT, name TEXT)");
        db.execSQL("CREATE TABLE tasks(id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, description TEXT, time TEXT, date TEXT, completed INTEGER DEFAULT 0, user_email TEXT, location_name TEXT, location_lat REAL DEFAULT 0, location_lng REAL DEFAULT 0, location_reminder INTEGER DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE tasks ADD COLUMN location_name TEXT DEFAULT ''");
            db.execSQL("ALTER TABLE tasks ADD COLUMN location_lat REAL DEFAULT 0");
            db.execSQL("ALTER TABLE tasks ADD COLUMN location_lng REAL DEFAULT 0");
            db.execSQL("ALTER TABLE tasks ADD COLUMN location_reminder INTEGER DEFAULT 0");
        }
    }

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
        return getWritableDatabase().insert("tasks", null, cv);
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
        return getWritableDatabase().update("tasks", cv, "id=?", new String[]{String.valueOf(task.getId())});
    }

    public void toggleTaskComplete(int taskId, boolean completed) {
        ContentValues cv = new ContentValues();
        cv.put("completed", completed ? 1 : 0);
        getWritableDatabase().update("tasks", cv, "id=?", new String[]{String.valueOf(taskId)});
    }

    public int deleteTask(int taskId) {
        return getWritableDatabase().delete("tasks", "id=?", new String[]{String.valueOf(taskId)});
    }

    public int deleteCompletedTasks(String userEmail) {
        return getWritableDatabase().delete("tasks", "user_email=? AND completed=?", new String[]{userEmail, "1"});
    }

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
}