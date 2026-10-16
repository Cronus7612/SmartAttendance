package com.example.smartattendance;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.card.MaterialCardView;

public class SettingsActivity extends AppCompatActivity {

    private String userRole = "STUDENT";
    private String userKey = "Student";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        SessionManager sessionManager = new SessionManager(this);

        String r = getIntent().getStringExtra("USER_ROLE");
        String k = getIntent().getStringExtra("USER_KEY");
        if (r != null && !r.isEmpty()) userRole = r;
        if (k != null && !k.isEmpty()) userKey = k;

        TextView btnBack = findViewById(R.id.btnBack);
        MaterialCardView cardChangePassword = findViewById(R.id.cardChangePassword);
        MaterialCardView cardThemeToggle = findViewById(R.id.cardThemeToggle);
        MaterialCardView cardNotificationPref = findViewById(R.id.cardNotificationPref);
        MaterialCardView cardAboutApp = findViewById(R.id.cardAboutApp);
        MaterialCardView cardPrivacy = findViewById(R.id.cardPrivacy);
        MaterialCardView cardHelpSupport = findViewById(R.id.cardHelpSupport);

        TextView tvThemeStatus = findViewById(R.id.tvThemeStatus);
        TextView tvThemeIcon = findViewById(R.id.tvThemeIcon);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Theme Status Display
        boolean isDark = sessionManager.getThemeMode() == AppCompatDelegate.MODE_NIGHT_YES;
        if (tvThemeStatus != null) {
            tvThemeStatus.setText("Current Mode: " + (isDark ? "Dark Theme 🌙" : "Light Theme ☀️"));
        }
        if (tvThemeIcon != null) {
            tvThemeIcon.setText(isDark ? "🌙" : "☀️");
        }

        if (cardChangePassword != null) {
            cardChangePassword.setOnClickListener(v -> ChangePasswordUtil.showChangePasswordDialog(this, userRole, userKey));
        }

        if (cardThemeToggle != null) {
            cardThemeToggle.setOnClickListener(v -> {
                int newMode = isDark ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES;
                sessionManager.setThemeMode(newMode);
                AppCompatDelegate.setDefaultNightMode(newMode);
            });
        }

        if (cardNotificationPref != null) {
            cardNotificationPref.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("🔔 Notification Preferences")
                        .setMessage("In-App Alerts: ENABLED 🟢\nLeave Request Status Alerts: ENABLED 🟢\nCampus Announcements: ENABLED 🟢")
                        .setPositiveButton("OK", null)
                        .show();
            });
        }

        if (cardAboutApp != null) {
            cardAboutApp.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("ℹ️ About SmartAttendance")
                        .setMessage("SmartAttendance App v2.4.0\n\nFeatures:\n• Firebase Cloud Synchronization\n• Real-Time Attendance Logging\n• Telegram-Style Navigation Drawer\n• Dynamic Light & Dark Mode\n\nDesigned & Developed by Siddharth Bhambare.")
                        .setPositiveButton("Close", null)
                        .show();
            });
        }

        if (cardPrivacy != null) {
            cardPrivacy.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("🛡️ Privacy & Security")
                        .setMessage("Data Encryption: Enabled\nStorage: Firebase Realtime Database (Google Cloud)\nSession Token: Encrypted SharedPreferences")
                        .setPositiveButton("Close", null)
                        .show();
            });
        }

        if (cardHelpSupport != null) {
            cardHelpSupport.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("❓ Help & Support")
                        .setMessage("For ERP support, timetable corrections, or account issues, contact your Department Head (HOD) or Academic Coordinator.")
                        .setPositiveButton("Close", null)
                        .show();
            });
        }
    }
}