package com.androidapp.SmartTask;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "SmartTaskDB";
    private static final int DATABASE_VERSION = 1;

    // Table Users
    private static final String TABLE_USERS = "users";
    private static final String COL_USER_ID = "id";
    private static final String COL_USER_EMAIL = "email";
    private static final String COL_USER_PASSWORD = "password";
    private static final String COL_USER_NAME = "name";

    // Table Tasks
    private static final String TABLE_TASKS = "tasks";
    private static final String COL_TASK_ID = "id";
    private static final String COL_TASK_TITLE = "title";
    private static final String COL_TASK_DESCRIPTION = "description";
    private static final String COL_TASK_TIME = "time";
    private static final String COL_TASK_DATE = "date";
    private static final String COL_TASK_COMPLETED = "completed";
    private static final String COL_TASK_USER_EMAIL = "user_email";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tạo bảng Users
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + "("
                + COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_USER_EMAIL + " TEXT UNIQUE,"
                + COL_USER_PASSWORD + " TEXT,"
                + COL_USER_NAME + " TEXT"
                + ")";
        db.execSQL(createUsersTable);

        // Tạo bảng Tasks
        String createTasksTable = "CREATE TABLE " + TABLE_TASKS + "("
                + COL_TASK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_TASK_TITLE + " TEXT,"
                + COL_TASK_DESCRIPTION + " TEXT,"
                + COL_TASK_TIME + " TEXT,"
                + COL_TASK_DATE + " TEXT,"
                + COL_TASK_COMPLETED + " INTEGER DEFAULT 0,"
                + COL_TASK_USER_EMAIL + " TEXT"
                + ")";
        db.execSQL(createTasksTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    // ============ USER METHODS ============

    public boolean registerUser(String email, String password, String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USER_EMAIL, email);
        values.put(COL_USER_PASSWORD, password);
        values.put(COL_USER_NAME, name);

        long result = db.insert(TABLE_USERS, null, values);
        db.close();
        return result != -1;
    }

    public boolean checkUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_USERS
                + " WHERE " + COL_USER_EMAIL + "=? AND " + COL_USER_PASSWORD + "=?";
        Cursor cursor = db.rawQuery(query, new String[]{email, password});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    public boolean checkEmailExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_USERS + " WHERE " + COL_USER_EMAIL + "=?";
        Cursor cursor = db.rawQuery(query, new String[]{email});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    public String getUserName(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COL_USER_NAME + " FROM " + TABLE_USERS
                + " WHERE " + COL_USER_EMAIL + "=?";
        Cursor cursor = db.rawQuery(query, new String[]{email});
        String name = "";
        if (cursor.moveToFirst()) {
            name = cursor.getString(0);
        }
        cursor.close();
        db.close();
        return name;
    }

    // ============ TASK CRUD METHODS ============

    // CREATE - Thêm task mới
    public long addTask(String title, String description, String time, String date, String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TASK_TITLE, title);
        values.put(COL_TASK_DESCRIPTION, description);
        values.put(COL_TASK_TIME, time);
        values.put(COL_TASK_DATE, date);
        values.put(COL_TASK_COMPLETED, 0);
        values.put(COL_TASK_USER_EMAIL, userEmail);

        long id = db.insert(TABLE_TASKS, null, values);
        db.close();
        return id;
    }

    // READ - Lấy tất cả task của user
    public List<Task> getAllTasks(String userEmail) {
        List<Task> taskList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_TASKS
                + " WHERE " + COL_TASK_USER_EMAIL + "=? ORDER BY " + COL_TASK_ID + " DESC";
        Cursor cursor = db.rawQuery(query, new String[]{userEmail});

        if (cursor.moveToFirst()) {
            do {
                Task task = new Task(
                        cursor.getInt(0),    // id
                        cursor.getString(1), // title
                        cursor.getString(2), // description
                        cursor.getString(3), // time
                        cursor.getString(4), // date
                        cursor.getInt(5) == 1, // completed
                        cursor.getString(6)  // userEmail
                );
                taskList.add(task);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return taskList;
    }

    // READ - Lấy task theo ID
    public Task getTaskById(int taskId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_TASKS + " WHERE " + COL_TASK_ID + "=?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(taskId)});

        Task task = null;
        if (cursor.moveToFirst()) {
            task = new Task(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getInt(5) == 1,
                    cursor.getString(6)
            );
        }

        cursor.close();
        db.close();
        return task;
    }

    // UPDATE - Cập nhật task
    public int updateTask(int taskId, String title, String description, String time, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TASK_TITLE, title);
        values.put(COL_TASK_DESCRIPTION, description);
        values.put(COL_TASK_TIME, time);
        values.put(COL_TASK_DATE, date);

        int rowsAffected = db.update(TABLE_TASKS, values, COL_TASK_ID + "=?",
                new String[]{String.valueOf(taskId)});
        db.close();
        return rowsAffected;
    }

    // UPDATE - Đánh dấu hoàn thành/chưa hoàn thành
    public void toggleTaskComplete(int taskId, boolean completed) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TASK_COMPLETED, completed ? 1 : 0);

        db.update(TABLE_TASKS, values, COL_TASK_ID + "=?",
                new String[]{String.valueOf(taskId)});
        db.close();
    }

    // DELETE - Xoá task
    public void deleteTask(int taskId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TASKS, COL_TASK_ID + "=?",
                new String[]{String.valueOf(taskId)});
        db.close();
    }

    // DELETE - Xoá tất cả task đã hoàn thành
    public void deleteCompletedTasks(String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TASKS, COL_TASK_USER_EMAIL + "=? AND " + COL_TASK_COMPLETED + "=?",
                new String[]{userEmail, "1"});
        db.close();
    }

    // Thống kê
    public int getCompletedCount(String userEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + TABLE_TASKS
                + " WHERE " + COL_TASK_USER_EMAIL + "=? AND " + COL_TASK_COMPLETED + "=?";
        Cursor cursor = db.rawQuery(query, new String[]{userEmail, "1"});
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }

    public int getPendingCount(String userEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + TABLE_TASKS
                + " WHERE " + COL_TASK_USER_EMAIL + "=? AND " + COL_TASK_COMPLETED + "=?";
        Cursor cursor = db.rawQuery(query, new String[]{userEmail, "0"});
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }

    public int getTotalCount(String userEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + TABLE_TASKS
                + " WHERE " + COL_TASK_USER_EMAIL + "=?";
        Cursor cursor = db.rawQuery(query, new String[]{userEmail});
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }
}