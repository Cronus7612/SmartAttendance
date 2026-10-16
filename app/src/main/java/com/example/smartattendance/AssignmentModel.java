package com.example.smartattendance;

@SuppressWarnings("unused")
public class AssignmentModel {
    private String assignmentId;
    private String title;
    private String description;
    private String subject;
    private String className;
    private String dueDate;
    private String postedBy;
    private long timestamp;

    public AssignmentModel() {
        // Required for Firebase deserialization
    }

    public AssignmentModel(String assignmentId, String title, String description, String subject, String className, String dueDate, String postedBy, long timestamp) {
        this.assignmentId = assignmentId;
        this.title = title;
        this.description = description;
        this.subject = subject;
        this.className = className;
        this.dueDate = dueDate;
        this.postedBy = postedBy;
        this.timestamp = timestamp;
    }

    public String getAssignmentId() { return assignmentId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getSubject() { return subject; }
    public String getClassName() { return className; }
    public String getDueDate() { return dueDate; }
    public String getPostedBy() { return postedBy; }
    public long getTimestamp() { return timestamp; }
}