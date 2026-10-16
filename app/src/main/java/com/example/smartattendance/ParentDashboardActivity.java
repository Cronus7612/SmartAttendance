package com.example.smartattendance;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class ParentDashboardActivity extends AppCompatActivity {

    private TextView tvParentOverallPercent, tvParentAcademicStatus, tvParentNoticeInfo;
    private ProgressBar pbParentOverall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashboard);

        TextView tvParentHeaderInfo = findViewById(R.id.tvParentHeaderInfo);
        MaterialButton btnLogout = findViewById(R.id.btnLogout);
        tvParentOverallPercent = findViewById(R.id.tvParentOverallPercent);
        tvParentAcademicStatus = findViewById(R.id.tvParentAcademicStatus);
        tvParentNoticeInfo = findViewById(R.id.tvParentNoticeInfo);
        pbParentOverall = findViewById(R.id.pbParentOverall);

        String childName = getIntent().getStringExtra("STUDENT_NAME");
        String childRoll = getIntent().getStringExtra("STUDENT_ROLL");

        if (childName == null || childName.trim().isEmpty()) childName = "Aditya Thete";
        if (childRoll == null || childRoll.trim().isEmpty()) childRoll = "1";

        if (tvParentHeaderInfo != null) {
            tvParentHeaderInfo.setText("Monitoring Child: " + childName + " (#" + childRoll + ")");
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> confirmLogout());
        }

        loadChildAttendance(childName);
        listenCampusNotices();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.logout_confirm_title)
                .setMessage(R.string.logout_confirm_msg)
                .setPositiveButton("Logout", (dialog, which) -> {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Exit App / Return to Home?")
                .setMessage("Do you want to minimize the app or log out?")
                .setPositiveButton("Minimize", (dialog, which) -> moveTaskToBack(true))
                .setNegativeButton("Logout", (dialog, which) -> {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void loadChildAttendance(String childName) {
        AttendanceData.listenStudentAttendance(childName, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int totalAttended = 0;
                int totalLectures = 0;

                if (snapshot.exists()) {
                    for (DataSnapshot sub : snapshot.getChildren()) {
                        Long a = sub.child("attended").getValue(Long.class);
                        Long t = sub.child("total").getValue(Long.class);
                        if (a != null) totalAttended += a;
                        if (t != null) totalLectures += t;
                    }
                }

                int percent = totalLectures > 0 ? (int) Math.round(((double) totalAttended / totalLectures) * 100) : 0;

                if (tvParentOverallPercent != null) {
                    tvParentOverallPercent.setText(getString(R.string.percent_format, percent));
                }
                if (pbParentOverall != null) {
                    pbParentOverall.setProgress(percent);
                }

                if (tvParentAcademicStatus != null) {
                    if (totalLectures == 0) {
                        tvParentAcademicStatus.setText(R.string.status_no_data);
                        tvParentAcademicStatus.setTextColor(0xFFBBF7D0);
                    } else if (percent >= 75) {
                        tvParentAcademicStatus.setText("✅ EXAM ELIGIBLE: Child's attendance is safe (" + percent + "%). Eligible for Term End Exams.");
                        tvParentAcademicStatus.setTextColor(0xFFBBF7D0);
                    } else {
                        tvParentAcademicStatus.setText("🚨 DEFAULTER WARNING: Attendance is below 75% (" + percent + "%)! Exam Hall Ticket may be withheld.");
                        tvParentAcademicStatus.setTextColor(0xFFFF3333);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void listenCampusNotices() {
        CloudRepository.announcementsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && tvParentNoticeInfo != null) {
                    StringBuilder sb = new StringBuilder();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        String title = child.child("title").getValue(String.class);
                        String msg = child.child("message").getValue(String.class);
                        Long ts = child.child("timestamp").getValue(Long.class);

                        long timestamp = ts != null ? ts : 0L;
                        String timeStr = CloudRepository.formatTimestamp(timestamp);

                        if (title != null && msg != null) {
                            sb.insert(0, "📌 " + title + (timeStr.isEmpty() ? "" : " [" + timeStr + "]") + "\n   " + msg + "\n\n");
                        }
                    }
                    if (sb.length() > 0) {
                        tvParentNoticeInfo.setText(sb.toString().trim());
                    } else {
                        tvParentNoticeInfo.setText(R.string.no_announcements);
                    }
                } else if (tvParentNoticeInfo != null) {
                    tvParentNoticeInfo.setText(R.string.no_announcements);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}