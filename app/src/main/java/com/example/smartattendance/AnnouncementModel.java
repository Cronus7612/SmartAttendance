package com.example.smartattendance;

public class AnnouncementModel {
    private String id;
    private String title;
    private String message;
    private String category;
    private String postedBy;
    private long timestamp;

    public AnnouncementModel() {
        // Required for Firebase
    }

    public AnnouncementModel(String id, String title, String message, String category, String postedBy, long timestamp) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.category = category;
        this.postedBy = postedBy;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPostedBy() { return postedBy; }
    public void setPostedBy(String postedBy) { this.postedBy = postedBy; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}