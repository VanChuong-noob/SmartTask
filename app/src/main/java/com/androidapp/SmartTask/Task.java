package com.androidapp.SmartTask;

public class Task {
    private int id;
    private String title;
    private String description;
    private String time;
    private String date;
    private boolean completed;
    private String userEmail;
    private String locationName;
    private double locationLat;
    private double locationLng;
    private boolean locationReminder;

    public Task() {
        this.completed = false;
        this.locationReminder = false;
        this.locationLat = 0;
        this.locationLng = 0;
        this.locationName = "";
    }

    public Task(String title, String description, String time, String date, String userEmail) {
        this.title = title;
        this.description = description;
        this.time = time;
        this.date = date;
        this.userEmail = userEmail;
        this.completed = false;
        this.locationReminder = false;
        this.locationLat = 0;
        this.locationLng = 0;
        this.locationName = "";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title != null ? title : ""; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description != null ? description : ""; }
    public void setDescription(String description) { this.description = description; }
    public String getTime() { return time != null ? time : ""; }
    public void setTime(String time) { this.time = time; }
    public String getDate() { return date != null ? date : ""; }
    public void setDate(String date) { this.date = date; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public String getUserEmail() { return userEmail != null ? userEmail : ""; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getLocationName() { return locationName != null ? locationName : ""; }
    public void setLocationName(String locationName) { this.locationName = locationName; }
    public double getLocationLat() { return locationLat; }
    public void setLocationLat(double locationLat) { this.locationLat = locationLat; }
    public double getLocationLng() { return locationLng; }
    public void setLocationLng(double locationLng) { this.locationLng = locationLng; }
    public boolean isLocationReminder() { return locationReminder; }
    public void setLocationReminder(boolean locationReminder) { this.locationReminder = locationReminder; }
}