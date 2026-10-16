package com.example.smartattendance;

import java.util.Objects;

public class StudentModel {
    private String roll;
    private String name;
    private String studentId;
    private String className;
    private String email;
    private String phone;
    private boolean isPresent;

    @SuppressWarnings("unused")
    public StudentModel() {
        // Required for Firebase deserialization
    }

    public StudentModel(String roll, String name, String studentId, String className, String email, String phone) {
        this.roll = roll;
        this.name = name;
        this.studentId = studentId != null && !studentId.isEmpty() ? studentId : generateStudentId(roll, name);
        this.className = className != null && !className.isEmpty() ? className : "TY BCA";
        this.email = Objects.requireNonNullElse(email, "");
        this.phone = Objects.requireNonNullElse(phone, "");
        this.isPresent = false;
    }

    public StudentModel(String roll, String name, String studentId, String className) {
        this(roll, name, studentId, className, "", "");
    }

    public StudentModel(String roll, String name) {
        this(roll, name, generateStudentId(roll, name), "TY BCA", "", "");
    }

    public static String generateStudentId(String roll, String name) {
        int seed = Math.abs((roll + name).hashCode());
        return "" + (39000000 + (seed % 9000000));
    }

    public String getRoll() { return Objects.requireNonNullElse(roll, ""); }
    public String getRollNo() { return Objects.requireNonNullElse(roll, ""); }
    public String getName() { return Objects.requireNonNullElse(name, ""); }
    public String getStudentId() { return Objects.requireNonNullElseGet(studentId, () -> generateStudentId(roll, name)); }
    public String getClassName() { return Objects.requireNonNullElse(className, "TY BCA"); }
    public String getEmail() { return Objects.requireNonNullElse(email, ""); }
    public String getPhone() { return Objects.requireNonNullElse(phone, ""); }
    public boolean isPresent() { return isPresent; }
    public void setPresent(boolean present) { isPresent = present; }
}