package com.example.smartattendance;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.Objects;

public class StudentDashboardActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private TextView tvOverallPercentage, tvExamStatus, tvStudentHeaderInfo, tvProgressTitle, tvAssignmentAlertText;
    private TextView tvDrawerStudentName, tvDrawerStudentDetails, tvDrawerInitials;
    private ProgressBar pbOverallRing;

    private String studentName = "Aditya Thete";
    private String studentRoll = "1";
    private String studentClass = "TY BCA";
    private String student8DigitId = "39108421";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        SessionManager sessionManager = new SessionManager(this);

        // 1. Bind Views & Telegram Drawer
        drawerLayout = findViewById(R.id.drawerLayout);
        TextView btnMenuDrawer = findViewById(R.id.btnMenuDrawer);

        tvStudentHeaderInfo = findViewById(R.id.tvStudentHeaderInfo);
        TextView btnNotificationBell = findViewById(R.id.btnNotificationBell);
        MaterialCardView btnStudentAvatar = findViewById(R.id.btnStudentAvatar);
        TextView tvAvatarInitials = findViewById(R.id.tvAvatarInitials);

        tvDrawerInitials = findViewById(R.id.tvDrawerInitials);
        tvDrawerStudentName = findViewById(R.id.tvDrawerStudentName);
        tvDrawerStudentDetails = findViewById(R.id.tvDrawerStudentDetails);
        TextView btnThemeToggle = findViewById(R.id.btnThemeToggle);

        if (btnThemeToggle != null) {
            btnThemeToggle.setText(sessionManager.getThemeMode() == AppCompatDelegate.MODE_NIGHT_YES ? "🌙" : "☀️");
            btnThemeToggle.setOnClickListener(v -> {
                int newMode = sessionManager.getThemeMode() == AppCompatDelegate.MODE_NIGHT_YES
                        ? AppCompatDelegate.MODE_NIGHT_NO
                        : AppCompatDelegate.MODE_NIGHT_YES;
                sessionManager.setThemeMode(newMode);
                AppCompatDelegate.setDefaultNightMode(newMode);
            });
        }

        TextView tvDrawerMyProfile = findViewById(R.id.tvDrawerMyProfile);
        TextView tvDrawerNotices = findViewById(R.id.tvDrawerNotices);
        TextView tvDrawerLeave = findViewById(R.id.tvDrawerLeave);
        TextView tvDrawerSettings = findViewById(R.id.tvDrawerSettings);
        MaterialButton btnDrawerLogout = findViewById(R.id.btnDrawerLogout);

        tvProgressTitle = findViewById(R.id.tvProgressTitle);
        tvOverallPercentage = findViewById(R.id.tvOverallPercentage);
        tvExamStatus = findViewById(R.id.tvExamStatus);
        pbOverallRing = findViewById(R.id.pbOverallRing);
        tvAssignmentAlertText = findViewById(R.id.tvAssignmentAlertText);
        MaterialCardView cardAssignmentAlertCard = findViewById(R.id.cardAssignmentAlertCard);

        MaterialCardView cardStudentAttendance = findViewById(R.id.cardStudentAttendance);
        MaterialCardView cardStudentAssignment = findViewById(R.id.cardStudentAssignment);
        MaterialCardView cardStudentNotices = findViewById(R.id.cardStudentNotices);
        MaterialCardView cardStudentLeave = findViewById(R.id.cardStudentLeave);
        MaterialCardView cardStudentTarget = findViewById(R.id.cardStudentTarget);
        MaterialCardView cardStudentTimetable = findViewById(R.id.cardStudentTimetable);

        LinearLayout btnBottomHome = findViewById(R.id.btnBottomHome);
        LinearLayout btnBottomGrades = findViewById(R.id.btnBottomGrades);
        LinearLayout btnBottomResources = findViewById(R.id.btnBottomResources);
        LinearLayout btnBottomProfile = findViewById(R.id.btnBottomProfile);

        // 2. Fetch Intent details
        String rawName = getIntent().getStringExtra("STUDENT_NAME");
        String rawRoll = getIntent().getStringExtra("STUDENT_ROLL");
        String rawClass = getIntent().getStringExtra("STUDENT_CLASS");
        String rawId = getIntent().getStringExtra("STUDENT_ID");

        if (rawName != null && !rawName.trim().isEmpty()) studentName = rawName;
        if (rawRoll != null && !rawRoll.trim().isEmpty()) studentRoll = rawRoll;
        if (rawClass != null && !rawClass.trim().isEmpty()) studentClass = rawClass;
        if (rawId != null && !rawId.trim().isEmpty()) student8DigitId = rawId;
        else student8DigitId = CloudRepository.generate8DigitStudentId(studentRoll, studentName);

        final String finalStudentName = studentName;
        final String finalStudentRoll = studentRoll;
        final String finalStudentClass = studentClass;

        updateStudentHeaderUI();

        // Setup Initials for Avatar
        if (!studentName.isEmpty()) {
            String initials = "AT";
            String[] parts = studentName.trim().split("\\s+");
            if (parts.length >= 2) {
                initials = ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
            } else if (!parts[0].isEmpty()) {
                initials = ("" + parts[0].charAt(0)).toUpperCase();
            }
            if (tvAvatarInitials != null) tvAvatarInitials.setText(initials);
            if (tvDrawerInitials != null) tvDrawerInitials.setText(initials);
        }

        // Verify Student Details from Firebase
        fetchStudentDetailsFromCloud();

        // Open Telegram Navigation Drawer
        if (btnMenuDrawer != null) {
            btnMenuDrawer.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.openDrawer(GravityCompat.START);
            });
        }

        if (btnStudentAvatar != null) {
            btnStudentAvatar.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.openDrawer(GravityCompat.START);
            });
        }

        // Quick Info Assignment Card Click -> Launches Student Assignments
        if (cardAssignmentAlertCard != null) {
            cardAssignmentAlertCard.setOnClickListener(v -> {
                Intent intent = new Intent(this, StudentAssignmentActivity.class);
                intent.putExtra("STUDENT_CLASS", finalStudentClass);
                startActivity(intent);
            });
        }

        // Telegram Drawer Actions
        if (tvDrawerMyProfile != null) {
            tvDrawerMyProfile.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
                showStudentProfileDialog();
            });
        }

        if (tvDrawerNotices != null) {
            tvDrawerNotices.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(this, CampusNoticesActivity.class));
            });
        }

        if (tvDrawerLeave != null) {
            tvDrawerLeave.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
                Intent intent = new Intent(this, StudentLeaveTrackerActivity.class);
                intent.putExtra("STUDENT_NAME", finalStudentName);
                intent.putExtra("STUDENT_ROLL", finalStudentRoll);
                startActivity(intent);
            });
        }

        if (tvDrawerSettings != null) {
            tvDrawerSettings.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
                Intent intent = new Intent(this, SettingsActivity.class);
                intent.putExtra("USER_ROLE", "STUDENT");
                intent.putExtra("USER_KEY", finalStudentName);
                startActivity(intent);
            });
        }

        if (btnDrawerLogout != null) {
            btnDrawerLogout.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
                confirmLogout();
            });
        }

        if (btnNotificationBell != null) {
            btnNotificationBell.setOnClickListener(v -> showPersonalNotificationsDialog());
        }

        // Option Hub Click Listeners -> Launches Dedicated Fullscreen Activities!
        if (cardStudentAttendance != null) {
            cardStudentAttendance.setOnClickListener(v -> {
                Intent intent = new Intent(this, StudentAttendanceActivity.class);
                intent.putExtra("STUDENT_NAME", finalStudentName);
                startActivity(intent);
            });
        }

        if (cardStudentAssignment != null) {
            cardStudentAssignment.setOnClickListener(v -> {
                Intent intent = new Intent(this, StudentAssignmentActivity.class);
                intent.putExtra("STUDENT_CLASS", finalStudentClass);
                startActivity(intent);
            });
        }

        if (cardStudentNotices != null) {
            cardStudentNotices.setOnClickListener(v -> startActivity(new Intent(this, CampusNoticesActivity.class)));
        }

        if (cardStudentLeave != null) {
            cardStudentLeave.setOnClickListener(v -> {
                Intent intent = new Intent(this, StudentLeaveTrackerActivity.class);
                intent.putExtra("STUDENT_NAME", finalStudentName);
                intent.putExtra("STUDENT_ROLL", finalStudentRoll);
                startActivity(intent);
            });
        }

        if (cardStudentTarget != null) {
            cardStudentTarget.setOnClickListener(v -> {
                Intent intent = new Intent(this, TargetCalculatorActivity.class);
                intent.putExtra("STUDENT_NAME", finalStudentName);
                startActivity(intent);
            });
        }

        if (cardStudentTimetable != null) {
            cardStudentTimetable.setOnClickListener(v -> {
                Intent intent = new Intent(this, TimetableActivity.class);
                intent.putExtra("STUDENT_CLASS", finalStudentClass);
                intent.putExtra("USER_ROLE", "STUDENT");
                startActivity(intent);
            });
        }

        // Bottom Navigation Bar Actions
        if (btnBottomHome != null) {
            btnBottomHome.setOnClickListener(v -> {});
        }

        if (btnBottomGrades != null) {
            btnBottomGrades.setOnClickListener(v -> Toast.makeText(this, "🎓 Grades Portal: This feature is coming in the next update!", Toast.LENGTH_LONG).show());
        }

        if (btnBottomResources != null) {
            btnBottomResources.setOnClickListener(v -> {
                Intent intent = new Intent(this, ResourcesActivity.class);
                intent.putExtra("USER_ROLE", "STUDENT");
                intent.putExtra("STUDENT_CLASS", finalStudentClass);
                startActivity(intent);
            });
        }

        if (btnBottomProfile != null) {
            btnBottomProfile.setOnClickListener(v -> showStudentProfileDialog());
        }

        // Live Listeners for Overview & Real-Time Assignment Alert
        loadLiveAttendanceOverview(studentName);
        listenLiveClassAssignments();
    }

    private void listenLiveClassAssignments() {
        CloudRepository.assignmentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (tvAssignmentAlertText == null) return;
                boolean foundNew = false;
                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String title = CloudRepository.getSafeString(ds, "title");
                        String cName = CloudRepository.getSafeString(ds, "className");
                        String dueDate = CloudRepository.getSafeString(ds, "dueDate");

                        boolean isClassMatch = cName.isEmpty()
                                || cName.equalsIgnoreCase(studentClass)
                                || studentClass.toLowerCase().contains(cName.toLowerCase());

                        if (isClassMatch && !title.isEmpty()) {
                            foundNew = true;
                            tvAssignmentAlertText.setText("🆕 " + title + "\n(Due: " + (!dueDate.isEmpty() ? dueDate : "N/A") + ")");
                            break;
                        }
                    }
                }
                if (!foundNew) {
                    tvAssignmentAlertText.setText("No Pending Assignments");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.logout_confirm_title)
                .setMessage(R.string.logout_confirm_msg)
                .setPositiveButton("Logout", (dialog, which) -> {
                    new SessionManager(this).logoutUser();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void updateStudentHeaderUI() {
        if (tvStudentHeaderInfo != null) {
            tvStudentHeaderInfo.setText(studentName + " | " + studentClass + " (#" + studentRoll + ")");
        }
        if (tvDrawerStudentName != null) {
            tvDrawerStudentName.setText(studentName);
        }
        if (tvDrawerStudentDetails != null) {
            tvDrawerStudentDetails.setText(studentClass + " | ID: #" + student8DigitId);
        }
    }

    private void fetchStudentDetailsFromCloud() {
        CloudRepository.studentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String name = CloudRepository.getSafeString(ds, "name");
                        String cName = CloudRepository.getSafeString(ds, "className");
                        String sId = CloudRepository.getSafeString(ds, "studentId");

                        if (!name.isEmpty() && name.equalsIgnoreCase(studentName)) {
                            if (!cName.isEmpty()) studentClass = cName;
                            if (!sId.isEmpty()) student8DigitId = sId;
                            updateStudentHeaderUI();
                            break;
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showStudentProfileDialog() {
        new AlertDialog.Builder(this)
                .setTitle("🎓 Student Profile")
                .setMessage("Name: " + studentName + "\nStudent Unique ID: #" + student8DigitId + "\nRoll Number: #" + studentRoll + "\nAcademic Class: " + studentClass + "\nStatus: Active\nPortal: Cloud Attendance Synchronized")
                .setPositiveButton("Close", null)
                .show();
    }

    // Decoupled Personal Notifications Dialog (With Clear Button)
    private void showPersonalNotificationsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🔔 Personal Alerts & Notifications");

        CloudRepository.leaveRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    StringBuilder sb = new StringBuilder("Personal Leave & Status Alerts:\n\n");
                    int count = 0;
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String applicantName = CloudRepository.getSafeString(ds, "applicantName");
                        String reason = CloudRepository.getSafeString(ds, "reason");
                        String status = CloudRepository.getSafeString(ds, "status");
                        Long actionTs = ds.child("actionTimestamp").getValue(Long.class);

                        if (!applicantName.isEmpty() && applicantName.equalsIgnoreCase(studentName) && !"PENDING".equalsIgnoreCase(status)) {
                            count++;
                            String badge = "APPROVED 🟢";
                            String timeStr = "";

                            if (actionTs != null && actionTs > 0) {
                                timeStr = " (" + CloudRepository.formatTimestamp(actionTs) + ")";
                            }

                            if ("REJECTED".equalsIgnoreCase(status)) badge = "REJECTED 🔴";

                            sb.append("• Leave Request (").append(!reason.isEmpty() ? reason : "N/A")
                                    .append(") was ").append(badge).append(timeStr).append("\n\n");
                        }
                    }
                    if (count > 0) {
                        builder.setMessage(sb.toString().trim());
                    } else {
                        builder.setMessage("No unread personal alerts at this time.");
                    }
                } else {
                    builder.setMessage("No unread personal alerts at this time.");
                }

                builder.setNeutralButton("🗑️ Clear All Alerts", (dialog, which) -> Toast.makeText(StudentDashboardActivity.this, "Personal Notifications Cleared!", Toast.LENGTH_SHORT).show());

                builder.setPositiveButton("Close", null);
                builder.show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadLiveAttendanceOverview(String studentName) {
        AttendanceData.listenStudentAttendance(studentName, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int totalAttendedAll = 0;
                int totalLecturesAll = 0;

                if (snapshot.exists()) {
                    for (DataSnapshot subDs : snapshot.getChildren()) {
                        Long a = subDs.child("attended").getValue(Long.class);
                        Long t = subDs.child("total").getValue(Long.class);
                        if (a != null) totalAttendedAll += a;
                        if (t != null) totalLecturesAll += t;
                    }
                }

                int overall = totalLecturesAll > 0 ? (int) Math.round(((double) totalAttendedAll / totalLecturesAll) * 100) : 0;
                if (tvOverallPercentage != null) {
                    tvOverallPercentage.setText(overall > 0 ? overall + "%" : "");
                }
                if (pbOverallRing != null) {
                    pbOverallRing.setProgress(overall);
                }

                if (tvExamStatus != null) {
                    if (totalLecturesAll == 0 || overall >= 75) {
                        tvExamStatus.setText("On Track!");
                    } else {
                        tvExamStatus.setText("Defaulter Warning!");
                    }
                }

                if (tvProgressTitle != null) {
                    tvProgressTitle.setText(overall > 0 ? "My Attendance: " + overall + "%" : "My Attendance");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}