package com.example.it_project2;

public class Aktivitas {
    private String title;
    private String description;
    private long timestamp;
    private boolean active;

    public Aktivitas() {
        // Required for Firebase
    }

    public Aktivitas(String title, String description, long timestamp, boolean active) {
        this.title = title;
        this.description = description;
        this.timestamp = timestamp;
        this.active = active;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
