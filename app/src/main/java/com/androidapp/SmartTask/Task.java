package com.androidapp.SmartTask;

public class Task {
    private int id;
    private String title;
    private String description;
    private String time;
    private String date;
    private boolean completed;
    private String userEmail;

    // Constructor đầy đủ
    public Task(int id, String title, String description, String time, String date,
                boolean completed, String userEmail) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.time = time;
        this.date = date;
        this.completed = completed;
        this.userEmail = userEmail;
    }

    // Constructor không có ID (dùng khi thêm mới)
    public Task(String title, String description, String time, String date, String userEmail) {
        this.title = title;
        this.description = description;
        this.time = time;
        this.date = date;
        this.userEmail = userEmail;
        this.completed = false;
    }

    // Constructor đơn giản (giữ lại để tương thích)
    public Task(String title, String time, boolean completed) {
        this.title = title;
        this.time = time;
        this.completed = completed;
        this.description = "";
        this.date = "";
        this.userEmail = "";
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
}