package com.example.smartattendance;

public class StudentModel {
    private String roll;
    private String name;
    private String studentId;
    private String className;
    private boolean isPresent;

    // Firebase sathi compulsory empty constructor:
    public StudentModel() {
    }

    public StudentModel(String roll, String name) {
        this.roll = roll;
        this.name = name;
        this.studentId = generateStudentId(roll, name);
        this.className = "TY BCA";
        this.isPresent = false;
    }

    public StudentModel(String roll, String name, String studentId, String className) {
        this.roll = roll;
        this.name = name;
        this.studentId = studentId != null && !studentId.isEmpty() ? studentId : generateStudentId(roll, name);
        this.className = className != null && !className.isEmpty() ? className : "TY BCA";
        this.isPresent = false;
    }

    public static String generateStudentId(String roll, String name) {
        int seed = Math.abs((roll + name).hashCode());
        return String.valueOf(39000000 + (seed % 9000000));
    }

    public String getRoll() { return roll != null ? roll : ""; }
    public String getRollNo() { return roll != null ? roll : ""; }
    public String getName() { return name != null ? name : ""; }
    public String getStudentId() { return studentId != null ? studentId : generateStudentId(roll, name); }
    public String getClassName() { return className != null ? className : "TY BCA"; }
    public boolean isPresent() { return isPresent; }
    public void setPresent(boolean present) { isPresent = present; }
}