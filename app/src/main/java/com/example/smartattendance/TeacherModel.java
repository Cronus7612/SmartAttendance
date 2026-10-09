package com.example.smartattendance;

public class TeacherModel {
    private String id;
    private String name;

    public TeacherModel(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }
}