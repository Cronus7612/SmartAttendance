package com.example.smartattendance;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class TeacherDashboardActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private String teacherAssignedClasses = "TY BCA, SY BCA, FY BCA";
    private String teacherAssignedSubjects = "Core Java, Cloud Computing, UI & UX Design";

    private ProgressBar pbOverallRing;
    private TextView tvOverallPercentage, tvProgressTitle, tvPendingLeaveCountText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_dashboard);

        SessionManager sessionManager = new SessionManager(this);

        // Bind all Views (using View type for btnMenuDrawer to prevent ClassCastException)
        drawerLayout = findViewById(R.id.drawerLayout);
        View btnMenuDrawer = findViewById(R.id.btnMenuDrawer);

        TextView tvFacultyInfo = findViewById(R.id.tvFacultyInfo);
        MaterialCardView btnFacultyAvatar = findViewById(R.id.btnFacultyAvatar);
        TextView tvFacultyAvatarInitials = findViewById(R.id.tvFacultyAvatarInitials);
        TextView btnNotificationBell = findViewById(R.id.btnNotificationBell);

        TextView tvDrawerFacultyInitials = findViewById(R.id.tvDrawerFacultyInitials);
        TextView tvDrawerFacultyName = findViewById(R.id.tvDrawerFacultyName);
        TextView tvDrawerFacultyDetails = findViewById(R.id.tvDrawerFacultyDetails);
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

        TextView tvDrawerFacultyProfile = findViewById(R.id.tvDrawerFacultyProfile);
        TextView tvDrawerFacultyLeaves = findViewById(R.id.tvDrawerFacultyLeaves);
        TextView tvDrawerFacultyNotice = findViewById(R.id.tvDrawerFacultyNotice);
        TextView tvDrawerFacultySettings = findViewById(R.id.tvDrawerFacultySettings);
        MaterialButton btnDrawerLogout = findViewById(R.id.btnDrawerLogout);

        pbOverallRing = findViewById(R.id.pbOverallRing);
        tvOverallPercentage = findViewById(R.id.tvOverallPercentage);
        tvProgressTitle = findViewById(R.id.tvProgressTitle);
        tvPendingLeaveCountText = findViewById(R.id.tvPendingLeaveCountText);
        MaterialCardView cardPendingLeaveCard = findViewById(R.id.cardPendingLeaveCard);

        MaterialCardView cardOptionMarkAttendance = findViewById(R.id.cardOptionMarkAttendance);
        MaterialCardView cardOptionAssignment = findViewById(R.id.cardOptionAssignment);
        MaterialCardView cardOptionPostNotice = findViewById(R.id.cardOptionPostNotice);
        MaterialCardView cardOptionManageLeaves = findViewById(R.id.cardOptionManageLeaves);
        MaterialCardView cardOptionAdjustRoster = findViewById(R.id.cardOptionAdjustRoster);
        MaterialCardView cardOptionExportReport = findViewById(R.id.cardOptionExportReport);
        MaterialCardView cardOptionUploadNotes = findViewById(R.id.cardOptionUploadNotes);

        LinearLayout btnBottomHome = findViewById(R.id.btnBottomHome);
        LinearLayout btnBottomSchedule = findViewById(R.id.btnBottomSchedule);
        LinearLayout btnBottomRoster = findViewById(R.id.btnBottomRoster);
        LinearLayout btnBottomProfile = findViewById(R.id.btnBottomProfile);

        // Safe Profile / Teacher Info display with Firebase path sanitization
        String rawTeacherName = getIntent().getStringExtra("TEACHER_NAME");
        String rawTeacherId = getIntent().getStringExtra("TEACHER_ID");

        String teacherName = (rawTeacherName == null || rawTeacherName.trim().isEmpty())
                ? "Prof. S. R. Patil" : rawTeacherName;

        String teacherId = "20481923";
        if (rawTeacherId != null && !rawTeacherId.trim().isEmpty()) {
            String clean = rawTeacherId.replaceAll("[.#$\\[\\]]", "").replaceAll("^#", "").trim();
            if (!clean.isEmpty()) {
                teacherId = clean;
            }
        }

        final String finalTeacherName = teacherName;
        final String finalTeacherId = teacherId;

        if (tvFacultyInfo != null) {
            tvFacultyInfo.setText(teacherName + " (#" + teacherId + ")");
        }
        if (tvDrawerFacultyName != null) {
            tvDrawerFacultyName.setText(teacherName);
        }
        if (tvDrawerFacultyDetails != null) {
            tvDrawerFacultyDetails.setText("Faculty ID: #" + teacherId);
        }

        if (!teacherName.isEmpty()) {
            String initials = "S";
            String[] parts = teacherName.trim().split("\\s+");
            if (parts.length >= 2) {
                initials = ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
            } else if (!parts[0].isEmpty()) {
                initials = ("" + parts[0].charAt(0)).toUpperCase();
            }
            if (tvFacultyAvatarInitials != null) tvFacultyAvatarInitials.setText(initials);
            if (tvDrawerFacultyInitials != null) tvDrawerFacultyInitials.setText(initials);
        }

        // Fetch Assigned Classes & Subjects for Teacher from Cloud Firebase (TYPE SAFE)
        CloudRepository.teacherRef.child(finalTeacherId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String classes = CloudRepository.getSafeString(snapshot, "assignedClasses");
                    String subjects = CloudRepository.getSafeString(snapshot, "assignedSubjects");
                    if (!classes.isEmpty()) {
                        teacherAssignedClasses = classes;
                    }
                    if (!subjects.isEmpty()) {
                        teacherAssignedSubjects = subjects;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Open Telegram Navigation Drawer
        if (btnMenuDrawer != null) {
            btnMenuDrawer.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.openDrawer(GravityCompat.START);
            });
        }

        if (btnFacultyAvatar != null) {
            btnFacultyAvatar.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.openDrawer(GravityCompat.START);
            });
        }

        if (btnNotificationBell != null) {
            btnNotificationBell.setOnClickListener(v -> showTeacherNotificationsDialog());
        }

        // Quick Info Pending Leave Card Click -> Direct Launch Leave Approvals
        if (cardPendingLeaveCard != null) {
            cardPendingLeaveCard.setOnClickListener(v -> {
                Intent intent = new Intent(this, LeaveApprovalActivity.class);
                intent.putExtra("TEACHER_NAME", finalTeacherName);
                startActivity(intent);
            });
        }

        // Drawer Menu Click Handlers
        if (tvDrawerFacultyProfile != null) {
            tvDrawerFacultyProfile.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
                showFacultyProfileDialog(finalTeacherName, finalTeacherId);
            });
        }

        if (tvDrawerFacultyLeaves != null) {
            tvDrawerFacultyLeaves.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
                Intent intent = new Intent(this, LeaveApprovalActivity.class);
                intent.putExtra("TEACHER_NAME", finalTeacherName);
                startActivity(intent);
            });
        }

        if (tvDrawerFacultyNotice != null) {
            tvDrawerFacultyNotice.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
                Intent intent = new Intent(this, PostNoticeActivity.class);
                intent.putExtra("TEACHER_NAME", finalTeacherName);
                startActivity(intent);
            });
        }

        if (tvDrawerFacultySettings != null) {
            tvDrawerFacultySettings.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
                Intent intent = new Intent(this, SettingsActivity.class);
                intent.putExtra("USER_ROLE", "TEACHER");
                intent.putExtra("USER_KEY", finalTeacherId);
                startActivity(intent);
            });
        }

        if (btnDrawerLogout != null) {
            btnDrawerLogout.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
                confirmLogout();
            });
        }

        // Option Hub Handlers
        if (cardOptionMarkAttendance != null) {
            cardOptionMarkAttendance.setOnClickListener(v -> {
                Intent intent = new Intent(this, AttendanceHubActivity.class);
                intent.putExtra("ASSIGNED_CLASSES", teacherAssignedClasses);
                intent.putExtra("ASSIGNED_SUBJECTS", teacherAssignedSubjects);
                startActivity(intent);
            });
        }

        if (cardOptionAssignment != null) {
            cardOptionAssignment.setOnClickListener(v -> {
                Intent intent = new Intent(this, FacultyAssignmentActivity.class);
                intent.putExtra("TEACHER_NAME", finalTeacherName);
                intent.putExtra("ASSIGNED_CLASSES", teacherAssignedClasses);
                startActivity(intent);
            });
        }

        if (cardOptionPostNotice != null) {
            cardOptionPostNotice.setOnClickListener(v -> {
                Intent intent = new Intent(this, PostNoticeActivity.class);
                intent.putExtra("TEACHER_NAME", finalTeacherName);
                startActivity(intent);
            });
        }

        if (cardOptionManageLeaves != null) {
            cardOptionManageLeaves.setOnClickListener(v -> {
                Intent intent = new Intent(this, LeaveApprovalActivity.class);
                intent.putExtra("TEACHER_NAME", finalTeacherName);
                startActivity(intent);
            });
        }

        if (cardOptionAdjustRoster != null) {
            cardOptionAdjustRoster.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdjustRosterActivity.class);
                intent.putExtra("ASSIGNED_CLASSES", teacherAssignedClasses);
                startActivity(intent);
            });
        }

        if (cardOptionExportReport != null) {
            cardOptionExportReport.setOnClickListener(v -> {
                Intent intent = new Intent(this, ExportReportActivity.class);
                intent.putExtra("ASSIGNED_CLASSES", teacherAssignedClasses);
                startActivity(intent);
            });
        }

        if (cardOptionUploadNotes != null) {
            cardOptionUploadNotes.setOnClickListener(v -> {
                Intent intent = new Intent(this, ResourcesActivity.class);
                intent.putExtra("USER_ROLE", "TEACHER");
                intent.putExtra("TEACHER_NAME", finalTeacherName);
                startActivity(intent);
            });
        }

        // Bottom Navigation Bar
        if (btnBottomHome != null) {
            btnBottomHome.setOnClickListener(v -> {});
        }
        if (btnBottomSchedule != null) {
            btnBottomSchedule.setOnClickListener(v -> {
                Intent intent = new Intent(this, TimetableActivity.class);
                intent.putExtra("USER_ROLE", "TEACHER");
                intent.putExtra("ASSIGNED_CLASSES", teacherAssignedClasses);
                startActivity(intent);
            });
        }
        if (btnBottomRoster != null) {
            btnBottomRoster.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdjustRosterActivity.class);
                intent.putExtra("ASSIGNED_CLASSES", teacherAssignedClasses);
                startActivity(intent);
            });
        }
        if (btnBottomProfile != null) {
            btnBottomProfile.setOnClickListener(v -> showFacultyProfileDialog(finalTeacherName, finalTeacherId));
        }

        // Listeners for Live Conducted Classes & Pending Leave Requests
        listenDailyClassesConducted();
        listenPendingLeaveRequests();
    }

    private void listenDailyClassesConducted() {
        SimpleDateFormat sdfToday = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDateStr = sdfToday.format(Calendar.getInstance().getTime());

        CloudRepository.attendanceLogsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int todayConductedCount = 0;
                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String logDate = CloudRepository.getSafeString(ds, "date");
                        if (!logDate.isEmpty() && logDate.equalsIgnoreCase(todayDateStr)) {
                            todayConductedCount++;
                        }
                    }
                }

                int dailyPercent = Math.min(100, todayConductedCount * 25);
                if (pbOverallRing != null) pbOverallRing.setProgress(dailyPercent);
                if (tvOverallPercentage != null) tvOverallPercentage.setText("");
                if (tvProgressTitle != null) {
                    tvProgressTitle.setText("Classes Conducted Today: " + todayConductedCount);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void listenPendingLeaveRequests() {
        CloudRepository.leaveRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int pendingCount = 0;
                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String status = CloudRepository.getSafeString(ds, "status");
                        if ("PENDING".equalsIgnoreCase(status)) {
                            pendingCount++;
                        }
                    }
                }
                if (tvPendingLeaveCountText != null) {
                    tvPendingLeaveCountText.setText(pendingCount + " Pending Requests");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showTeacherNotificationsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🔔 Campus Broadcasts & Notifications");

        CloudRepository.announcementsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        String title = CloudRepository.getSafeString(child, "title");
                        String msg = CloudRepository.getSafeString(child, "message");
                        String postedBy = CloudRepository.getSafeString(child, "postedBy");
                        Long ts = child.child("timestamp").getValue(Long.class);

                        long timestamp = ts != null ? ts : 0L;
                        String timeStr = CloudRepository.formatTimestamp(timestamp);

                        if (!title.isEmpty() && !msg.isEmpty()) {
                            String meta = (timeStr.isEmpty() ? "" : " [" + timeStr + "]") + (postedBy.isEmpty() ? "" : " - " + postedBy);
                            sb.insert(0, "📌 " + title + meta + "\n   " + msg + "\n\n");
                        }
                    }
                    builder.setMessage(sb.toString().trim());
                } else {
                    builder.setMessage("No unread campus notifications at this time.");
                }

                builder.setNeutralButton("🗑️ Clear All Notifications", (dialog, which) -> {
                    CloudRepository.clearAllAnnouncements();
                    Toast.makeText(TeacherDashboardActivity.this, "Notifications Cleared!", Toast.LENGTH_SHORT).show();
                });

                builder.setPositiveButton("Close", null);
                builder.show();
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

    private void showFacultyProfileDialog(String name, String id) {
        new AlertDialog.Builder(this)
                .setTitle("👨‍🏫 Faculty Profile")
                .setMessage("Name: " + name + "\nFaculty ID: #" + id + "\nAssigned Department/Classes: " + teacherAssignedClasses + "\nAssigned Lectures/Subjects: " + teacherAssignedSubjects + "\nAccount Status: Active & Cloud-Synced")
                .setPositiveButton("Close", null)
                .show();
    }
}