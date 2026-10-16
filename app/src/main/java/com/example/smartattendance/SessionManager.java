package com.example.smartattendance;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class SessionManager {

    private static final String PREF_NAME = "SmartAttendanceSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_ROLE = "role"; // ADMIN, TEACHER, STUDENT, PARENT
    private static final String KEY_NAME = "name";
    private static final String KEY_ID_OR_ROLL = "idOrRoll";
    private static final String KEY_CLASS_NAME = "className";
    private static final String KEY_STUDENT_ID = "studentId";
    private static final String KEY_THEME_MODE = "themeMode";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void createSession(String role, String name, String idOrRoll, String className, String studentId) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_ROLE, role);
        editor.putString(KEY_NAME, name);
        editor.putString(KEY_ID_OR_ROLL, idOrRoll);
        editor.putString(KEY_CLASS_NAME, className != null ? className : "TY BCA");
        editor.putString(KEY_STUDENT_ID, studentId != null ? studentId : "");
        editor.apply();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getRole() {
        return pref.getString(KEY_ROLE, "");
    }

    public String getName() {
        return pref.getString(KEY_NAME, "");
    }

    public String getIdOrRoll() {
        return pref.getString(KEY_ID_OR_ROLL, "");
    }

    public String getClassName() {
        return pref.getString(KEY_CLASS_NAME, "TY BCA");
    }

    public String getStudentId() {
        return pref.getString(KEY_STUDENT_ID, "");
    }

    public int getThemeMode() {
        return pref.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_NO);
    }

    public void setThemeMode(int mode) {
        editor.putInt(KEY_THEME_MODE, mode);
        editor.apply();
    }

    public void logoutUser() {
        int savedTheme = getThemeMode();
        editor.clear();
        editor.putInt(KEY_THEME_MODE, savedTheme);
        editor.apply();
    }
}