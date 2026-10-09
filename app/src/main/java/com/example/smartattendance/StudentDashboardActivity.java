package com.example.smartattendance;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class StudentDashboardActivity extends AppCompatActivity {

    private TextView tvOverallPercentage, tvExamStatus, tvTargetCalculatorInfo, tvAnnouncementInfo, tvMonthwiseInfo, tvStudentHeaderInfo;
    private ProgressBar pbOverall;
    private LinearLayout layoutStudentAttendancePanel, layoutSubjectCards;
    private MaterialCardView cardTargetSection, cardMonthWiseSummary;
    private MaterialButton btnSubjectWiseFilter, btnMonthWiseFilter;

    private String studentName = "Aditya Thete";
    private String studentRoll = "1";
    private String studentClass = "TY BCA";
    private String student8DigitId = "39108421";

    // 6 Subject Card Views & Titles
    private MaterialCardView[] cardViews;
    private TextView[] titleViews;
    private TextView[] countViews;
    private TextView[] percentViews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        // 1. Bind Views
        tvStudentHeaderInfo = findViewById(R.id.tvStudentHeaderInfo);
        TextView btnNotificationBell = findViewById(R.id.btnNotificationBell);
        MaterialCardView btnStudentAvatar = findViewById(R.id.btnStudentAvatar);
        TextView tvAvatarInitials = findViewById(R.id.tvAvatarInitials);

        tvOverallPercentage = findViewById(R.id.tvOverallPercentage);
        tvExamStatus = findViewById(R.id.tvExamStatus);
        tvTargetCalculatorInfo = findViewById(R.id.tvTargetCalculatorInfo);
        tvAnnouncementInfo = findViewById(R.id.tvAnnouncementInfo);
        tvMonthwiseInfo = findViewById(R.id.tvMonthwiseInfo);
        pbOverall = findViewById(R.id.pbOverall);

        layoutStudentAttendancePanel = findViewById(R.id.layoutStudentAttendancePanel);
        layoutSubjectCards = findViewById(R.id.layoutSubjectCards);
        cardMonthWiseSummary = findViewById(R.id.cardMonthWiseSummary);
        cardTargetSection = findViewById(R.id.cardTargetSection);

        btnSubjectWiseFilter = findViewById(R.id.btnSubjectWiseFilter);
        btnMonthWiseFilter = findViewById(R.id.btnMonthWiseFilter);

        MaterialCardView cardStudentAttendance = findViewById(R.id.cardStudentAttendance);
        MaterialCardView cardStudentNotices = findViewById(R.id.cardStudentNotices);
        MaterialCardView cardStudentLeave = findViewById(R.id.cardStudentLeave);
        MaterialCardView cardStudentTarget = findViewById(R.id.cardStudentTarget);
        MaterialCardView cardStudentTimetable = findViewById(R.id.cardStudentTimetable);

        cardViews = new MaterialCardView[]{
                findViewById(R.id.cardSub1),
                findViewById(R.id.cardSub2),
                findViewById(R.id.cardSub3),
                findViewById(R.id.cardSub4),
                findViewById(R.id.cardSub5),
                findViewById(R.id.cardSub6)
        };

        titleViews = new TextView[]{
                findViewById(R.id.s1),
                findViewById(R.id.s2),
                findViewById(R.id.s3),
                findViewById(R.id.s4),
                findViewById(R.id.s5),
                findViewById(R.id.s6)
        };

        countViews = new TextView[]{
                findViewById(R.id.tvSub1Count),
                findViewById(R.id.tvSub2Count),
                findViewById(R.id.tvSub3Count),
                findViewById(R.id.tvSub4Count),
                findViewById(R.id.tvSub5Count),
                findViewById(R.id.tvSub6Count)
        };

        percentViews = new TextView[]{
                findViewById(R.id.tvSub1Percent),
                findViewById(R.id.tvSub2Percent),
                findViewById(R.id.tvSub3Percent),
                findViewById(R.id.tvSub4Percent),
                findViewById(R.id.tvSub5Percent),
                findViewById(R.id.tvSub6Percent)
        };

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

        updateStudentHeaderUI();

        // Setup Initials for Telegram Profile Avatar
        if (tvAvatarInitials != null && !studentName.isEmpty()) {
            String[] parts = studentName.trim().split("\\s+");
            if (parts.length >= 2) {
                tvAvatarInitials.setText(("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase());
            } else if (!parts[0].isEmpty()) {
                tvAvatarInitials.setText(("" + parts[0].charAt(0)).toUpperCase());
            }
        }

        // Verify Student Class and 8-Digit ID from Firebase
        fetchStudentDetailsFromCloud();

        // Toggle Filter Listeners
        if (btnSubjectWiseFilter != null) {
            btnSubjectWiseFilter.setOnClickListener(v -> showSubjectWiseView());
        }

        if (btnMonthWiseFilter != null) {
            btnMonthWiseFilter.setOnClickListener(v -> showMonthWiseView());
        }

        // Telegram Avatar Profile Click
        if (btnStudentAvatar != null) {
            btnStudentAvatar.setOnClickListener(v -> showStudentProfileDialog());
        }

        if (btnNotificationBell != null) {
            btnNotificationBell.setOnClickListener(v -> showNotificationsDialog());
        }

        // Option Hub Click Listeners
        if (cardStudentAttendance != null) {
            cardStudentAttendance.setOnClickListener(v -> {
                if (layoutStudentAttendancePanel != null) {
                    int vis = (layoutStudentAttendancePanel.getVisibility() == View.VISIBLE) ? View.GONE : View.VISIBLE;
                    layoutStudentAttendancePanel.setVisibility(vis);
                }
            });
        }

        if (cardStudentNotices != null) {
            cardStudentNotices.setOnClickListener(v -> showNotificationsDialog());
        }

        if (cardStudentLeave != null) {
            cardStudentLeave.setOnClickListener(v -> showLeaveTrackerDialog());
        }

        if (cardStudentTarget != null) {
            cardStudentTarget.setOnClickListener(v -> {
                if (cardTargetSection != null) {
                    int vis = (cardTargetSection.getVisibility() == View.VISIBLE) ? View.GONE : View.VISIBLE;
                    cardTargetSection.setVisibility(vis);
                }
            });
        }

        if (cardStudentTimetable != null) {
            cardStudentTimetable.setOnClickListener(v -> showTimetableDialog());
        }

        // 5. Live Announcements Observer
        listenCampusAnnouncements();

        // 6. Live Attendance Load
        loadLiveAttendance(studentName);
    }

    private void showSubjectWiseView() {
        if (layoutSubjectCards != null) layoutSubjectCards.setVisibility(View.VISIBLE);
        if (cardMonthWiseSummary != null) cardMonthWiseSummary.setVisibility(View.GONE);

        if (btnSubjectWiseFilter != null) {
            btnSubjectWiseFilter.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2563EB")));
        }
        if (btnMonthWiseFilter != null) {
            btnMonthWiseFilter.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#64748B")));
        }
    }

    private void showMonthWiseView() {
        if (layoutSubjectCards != null) layoutSubjectCards.setVisibility(View.GONE);
        if (cardMonthWiseSummary != null) cardMonthWiseSummary.setVisibility(View.VISIBLE);

        if (btnMonthWiseFilter != null) {
            btnMonthWiseFilter.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2563EB")));
        }
        if (btnSubjectWiseFilter != null) {
            btnSubjectWiseFilter.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#64748B")));
        }
    }

    private void updateStudentHeaderUI() {
        if (tvStudentHeaderInfo != null) {
            tvStudentHeaderInfo.setText(studentName + " | " + studentClass + " (#" + studentRoll + ")");
        }
    }

    private void fetchStudentDetailsFromCloud() {
        CloudRepository.studentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String name = ds.child("name").getValue(String.class);
                        String cName = ds.child("className").getValue(String.class);
                        String sId = ds.child("studentId").getValue(String.class);

                        if (name != null && name.equalsIgnoreCase(studentName)) {
                            if (cName != null && !cName.isEmpty()) studentClass = cName;
                            if (sId != null && !sId.isEmpty()) student8DigitId = sId;
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

    private void showNotificationsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🔔 Campus Notifications & History Log");

        if (tvAnnouncementInfo != null && !tvAnnouncementInfo.getText().toString().isEmpty()) {
            builder.setMessage(tvAnnouncementInfo.getText().toString());
        } else {
            builder.setMessage("No unread notifications at this time.");
        }

        builder.setNeutralButton("🗑️ Clear History", (dialog, which) -> {
            CloudRepository.clearAllAnnouncements();
            Toast.makeText(this, "Notice History Cleared!", Toast.LENGTH_SHORT).show();
        });

        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showTimetableDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📅 Daily Class Timetable (" + studentClass + ")");
        builder.setMessage("Monday - Friday Schedule (" + studentClass + "):\n\n" +
                "• 09:00 AM - 10:00 AM : Lecture 1\n" +
                "• 10:00 AM - 11:00 AM : Lecture 2\n" +
                "• 11:15 AM - 12:15 PM : Lecture 3\n" +
                "• 12:15 PM - 01:15 PM : Lecture 4\n" +
                "• 01:45 PM - 03:45 PM : Practical / Lab Session");
        builder.setPositiveButton("Close", null);
        builder.show();
    }

    private void showLeaveTrackerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📝 Digital Leave & Status Tracker");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 20);

        final TextView tvStatusHeader = new TextView(this);
        tvStatusHeader.setText("Your Submitted Leave Applications:\nFetching live status from Cloud...");
        tvStatusHeader.setTextSize(13);
        layout.addView(tvStatusHeader);

        final EditText etReason = new EditText(this);
        etReason.setHint("Reason for New Leave (e.g. Medical / NSS)");
        layout.addView(etReason);

        builder.setView(layout);

        // Fetch past leave applications live
        CloudRepository.leaveRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    StringBuilder sb = new StringBuilder("Your Submitted Leave Applications:\n\n");
                    int count = 0;
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String applicantName = ds.child("applicantName").getValue(String.class);
                        String reason = ds.child("reason").getValue(String.class);
                        String status = ds.child("status").getValue(String.class);
                        Long actionTs = ds.child("actionTimestamp").getValue(Long.class);

                        if (applicantName != null && applicantName.equalsIgnoreCase(studentName)) {
                            count++;
                            String badge = "PENDING 🟡";
                            String timeStr = "";

                            if (actionTs != null && actionTs > 0) {
                                timeStr = " (" + CloudRepository.formatTimestamp(actionTs) + ")";
                            }

                            if ("APPROVED".equalsIgnoreCase(status)) badge = "APPROVED 🟢" + timeStr;
                            else if ("REJECTED".equalsIgnoreCase(status)) badge = "REJECTED 🔴" + timeStr;

                            sb.append("• Reason: ").append(Objects.requireNonNullElse(reason, "N/A"))
                                    .append("\n  Status: ").append(badge).append("\n\n");
                        }
                    }
                    if (count > 0) {
                        tvStatusHeader.setText(sb.toString().trim());
                    } else {
                        tvStatusHeader.setText("No previous leave applications found.");
                    }
                } else {
                    tvStatusHeader.setText("No previous leave applications found.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        builder.setPositiveButton("Submit New Leave", (dialog, which) -> {
            String reason = etReason.getText().toString().trim();
            if (!reason.isEmpty()) {
                CloudRepository.submitLeaveApplication(studentName, studentRoll, "Medical/Duty", reason);
                Toast.makeText(this, "Leave application submitted to HOD/Principal!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Please enter a reason for leave", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Close", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void listenCampusAnnouncements() {
        CloudRepository.announcementsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && tvAnnouncementInfo != null) {
                    StringBuilder sb = new StringBuilder();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        String title = child.child("title").getValue(String.class);
                        String msg = child.child("message").getValue(String.class);
                        String postedBy = child.child("postedBy").getValue(String.class);
                        Long ts = child.child("timestamp").getValue(Long.class);

                        long timestamp = Objects.requireNonNullElse(ts, 0L);
                        String timeStr = CloudRepository.formatTimestamp(timestamp);

                        if (title != null && msg != null) {
                            String meta = (timeStr.isEmpty() ? "" : " [" + timeStr + "]") + (postedBy == null ? "" : " - " + postedBy);
                            sb.insert(0, "📌 " + title + meta + "\n   " + msg + "\n\n");
                        }
                    }
                    if (sb.length() > 0) {
                        tvAnnouncementInfo.setText(sb.toString().trim());
                    } else {
                        tvAnnouncementInfo.setText(R.string.no_announcements);
                    }
                } else if (tvAnnouncementInfo != null) {
                    tvAnnouncementInfo.setText(R.string.no_announcements);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadLiveAttendance(String studentName) {
        AttendanceData.listenStudentAttendance(studentName, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int totalAttendedAll = 0;
                int totalLecturesAll = 0;

                // Collect all dynamic subjects recorded in Firebase for this student
                List<DataSnapshot> subjectSnapshots = new ArrayList<>();
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        subjectSnapshots.add(child);
                    }
                }

                int maxCards = cardViews.length;

                if (!subjectSnapshots.isEmpty()) {
                    for (int i = 0; i < maxCards; i++) {
                        if (i < subjectSnapshots.size()) {
                            DataSnapshot subDs = subjectSnapshots.get(i);
                            String subName = subDs.getKey();
                            Long a = subDs.child("attended").getValue(Long.class);
                            Long t = subDs.child("total").getValue(Long.class);

                            long attended = Objects.requireNonNullElse(a, 0L);
                            long total = Objects.requireNonNullElse(t, 0L);

                            totalAttendedAll += (int) attended;
                            totalLecturesAll += (int) total;

                            int p = total > 0 ? (int) Math.round(((double) attended / total) * 100) : 0;

                            if (cardViews[i] != null) cardViews[i].setVisibility(View.VISIBLE);
                            if (titleViews[i] != null && subName != null) titleViews[i].setText(subName);
                            if (countViews[i] != null) countViews[i].setText(getString(R.string.attended_format, attended, total));
                            if (percentViews[i] != null) {
                                percentViews[i].setText(getString(R.string.percent_format, p));
                                percentViews[i].setTextColor(p >= 75 ? 0xFF16A34A : 0xFFDC2626);
                            }
                        } else {
                            if (cardViews[i] != null) cardViews[i].setVisibility(View.GONE);
                        }
                    }
                } else {
                    // Fallback default TY BCA subjects if no attendance marked yet
                    String[] defaultSubs = {"Core Java", "Entrepreneurship", "Software Engineering", "Cloud Computing", "UI & UX Design", "Project"};
                    for (int i = 0; i < maxCards; i++) {
                        if (cardViews[i] != null) cardViews[i].setVisibility(View.VISIBLE);
                        if (titleViews[i] != null) titleViews[i].setText(defaultSubs[i]);
                        if (countViews[i] != null) countViews[i].setText(getString(R.string.attended_format, 0L, 0L));
                        if (percentViews[i] != null) {
                            percentViews[i].setText(getString(R.string.percent_format, 0));
                            percentViews[i].setTextColor(0xFFDC2626);
                        }
                    }
                }

                int overall = totalLecturesAll > 0 ? (int) Math.round(((double) totalAttendedAll / totalLecturesAll) * 100) : 0;
                if (tvOverallPercentage != null) {
                    tvOverallPercentage.setText(getString(R.string.percent_format, overall));
                }
                if (pbOverall != null) {
                    pbOverall.setProgress(overall);
                }

                if (tvMonthwiseInfo != null) {
                    SimpleDateFormat sdfMonth = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
                    String currentMonthStr = sdfMonth.format(Calendar.getInstance().getTime());
                    tvMonthwiseInfo.setText(getString(R.string.monthwise_summary_format, currentMonthStr, overall));
                }

                // Calculate Target / Safe-Bunk Analytics
                updateTargetCalculator(totalAttendedAll, totalLecturesAll, overall);

                if (tvExamStatus != null) {
                    if (totalLecturesAll == 0) {
                        tvExamStatus.setText(R.string.status_no_data);
                        tvExamStatus.setTextColor(0xFFBBF7D0);
                    } else if (overall >= 75) {
                        tvExamStatus.setText("✅ EXAM ELIGIBLE: Attendance is safe (" + overall + "%). Eligible for Term End Exams.");
                        tvExamStatus.setTextColor(0xFFBBF7D0);
                    } else {
                        tvExamStatus.setText("🚨 DEFAULTER WARNING: Attendance is below 75% (" + overall + "%)! Exam Hall Ticket may be withheld.");
                        tvExamStatus.setTextColor(0xFFFF3333);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void updateTargetCalculator(int attended, int total, int percentage) {
        if (tvTargetCalculatorInfo == null) return;

        if (total == 0) {
            tvTargetCalculatorInfo.setText(R.string.target_calculator_no_history);
            return;
        }

        if (percentage < 75) {
            int needed = (int) Math.ceil((0.75 * total - attended) / 0.25);
            if (needed <= 0) needed = 1;
            tvTargetCalculatorInfo.setText("🚨 DEFAULTER WARNING: Attendance is below 75%!\nAttended: " + attended + "/" + total + " Lectures.\nYou need to attend next " + needed + " consecutive lectures to achieve 75% exam eligibility!");
        } else if (percentage > 75) {
            int safeBunk = (int) Math.floor((attended - 0.75 * total) / 0.75);
            if (safeBunk > 0) {
                tvTargetCalculatorInfo.setText("📊 Academic Exam Status: Eligible for Term End Exams! 🟢\nAttended: " + attended + "/" + total + " Lectures.\nYou can safely miss next " + safeBunk + " lectures and remain above 75%!");
            } else {
                tvTargetCalculatorInfo.setText("📊 Academic Exam Status: On Target (Exactly 75%)! 🟢\nAttended: " + attended + "/" + total + " Lectures.\nAttend all upcoming lectures to maintain eligibility.");
            }
        } else {
            tvTargetCalculatorInfo.setText("📊 Academic Exam Status: On Target (Exactly 75%)! 🟢\nAttended: " + attended + "/" + total + " Lectures.");
        }
    }
}