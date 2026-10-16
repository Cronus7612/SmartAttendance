package com.example.smartattendance;

import java.util.Objects;

public class TeacherModel {
    private String id;
    private String name;
    private String assignedClasses;
    private String assignedSubjects;
    private String email;
    private String phone;

    public TeacherModel() {
        // Required for Firebase
    }

    public TeacherModel(String id, String name, String assignedClasses, String assignedSubjects, String email, String phone) {
        this.id = id;
        this.name = name;
        this.assignedClasses = Objects.requireNonNullElse(assignedClasses, "TY BCA, SY BCA");
        this.assignedSubjects = Objects.requireNonNullElse(assignedSubjects, "Java, Cloud Computing, UI-UX Design");
        this.email = Objects.requireNonNullElse(email, "");
        this.phone = Objects.requireNonNullElse(phone, "");
    }

    public TeacherModel(String id, String name, String assignedClasses, String assignedSubjects) {
        this(id, name, assignedClasses, assignedSubjects, "", "");
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getAssignedClasses() { return assignedClasses; }
    public String getAssignedSubjects() { return assignedSubjects; }
    public String getEmail() { return Objects.requireNonNullElse(email, ""); }
    public String getPhone() { return Objects.requireNonNullElse(phone, ""); }
}