package com.androidapp.SmartTask;

public class Achievement {
    private String icon;
    private String title;
    private String description;
    private boolean unlocked;
    private String type;

    public Achievement() {
    }

    public Achievement(String icon, String title, String description, String type) {
        this.icon = icon;
        this.title = title;
        this.description = description;
        this.type = type;
        this.unlocked = false;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
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

    public boolean isUnlocked() {
        return unlocked;
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}