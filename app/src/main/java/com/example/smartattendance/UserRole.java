package com.example.smartattendance;

public enum UserRole {
    SUPER_ADMIN("Super Admin / Management"),
    PRINCIPAL("Principal / Vice Principal"),
    HOD("Head of Department (HOD)"),
    FACULTY("Faculty / Teacher"),
    STUDENT("Student"),
    PARENT("Parent / Guardian"),
    STAFF("Non-Teaching Staff");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static UserRole fromString(String roleStr) {
        if (roleStr == null) return STUDENT;
        for (UserRole r : values()) {
            if (r.name().equalsIgnoreCase(roleStr) || r.displayName.equalsIgnoreCase(roleStr)) {
                return r;
            }
        }
        return STUDENT;
    }
}