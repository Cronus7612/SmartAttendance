package com.example.smartattendance;

public class UserModel {
    private String id;
    private String name;
    private String password;
    private String role;
    private String department;
    private String deviceId;

    public UserModel() {
        // Required for Firebase
    }

    public UserModel(String id, String name, String password, String role, String department, String deviceId) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.role = role;
        this.department = department;
        this.deviceId = deviceId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}