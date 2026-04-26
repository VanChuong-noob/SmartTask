package com.androidapp.SmartTask;

public class Task {
    private String title;
    private String time;
    private boolean completed;

    public Task(String title, String time, boolean completed) {
        this.title = title;
        this.time = time;
        this.completed = completed;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}