package com.example.smartattendance;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

public class StudentAttendanceActivity extends AppCompatActivity {

    private TextView tvOverallPercentage, tvExamStatus;
    private ProgressBar pbOverall;
    private LinearLayout containerSubjects;
    private String studentName = "Aditya Thete";
    private final Calendar queryCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_attendance);

        TextView btnBack = findViewById(R.id.btnBack);
        MaterialButton btnDateQuery = findViewById(R.id.btnDateQuery);
        tvOverallPercentage = findViewById(R.id.tvOverallPercentage);
        tvExamStatus = findViewById(R.id.tvExamStatus);
        pbOverall = findViewById(R.id.pbOverall);
        containerSubjects = findViewById(R.id.containerSubjects);

        String name = getIntent().getStringExtra("STUDENT_NAME");
        if (name != null && !name.isEmpty()) studentName = name;

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnDateQuery != null) {
            btnDateQuery.setOnClickListener(v -> showDateAttendanceQueryPicker());
        }

        loadLiveAttendance(studentName);
    }

    private void showDateAttendanceQueryPicker() {
        int year = queryCalendar.get(Calendar.YEAR);
        int month = queryCalendar.get(Calendar.MONTH);
        int day = queryCalendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
            queryCalendar.set(Calendar.YEAR, selectedYear);
            queryCalendar.set(Calendar.MONTH, selectedMonth);
            queryCalendar.set(Calendar.DAY_OF_MONTH, selectedDay);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String dateStr = sdf.format(queryCalendar.getTime());
            fetchAttendanceForDate(dateStr);
        }, year, month, day);

        dialog.show();
    }

    private void fetchAttendanceForDate(String targetDate) {
        CloudRepository.attendanceLogsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                StringBuilder sb = new StringBuilder("Attendance Marked on " + targetDate + ":\n\n");
                int dailyRecords = 0;

                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String logDate = CloudRepository.getSafeString(ds, "date");
                        String sub = CloudRepository.getSafeString(ds, "subject");
                        String lecNum = CloudRepository.getSafeString(ds, "lectureNumber");
                        String lecType = CloudRepository.getSafeString(ds, "lectureType");

                        if (!logDate.isEmpty() && logDate.equalsIgnoreCase(targetDate)) {
                            dailyRecords++;
                            sb.append("• ").append(!sub.isEmpty() ? sub : "Subject")
                                    .append(" (").append(!lecNum.isEmpty() ? lecNum : "Lec").append(" - ").append(!lecType.isEmpty() ? lecType : "Regular").append(")\n")
                                    .append("  Status: Marked Present 🟢\n\n");
                        }
                    }
                }

                if (dailyRecords == 0) {
                    sb.append("No attendance records logged for this date.");
                }

                new AlertDialog.Builder(StudentAttendanceActivity.this)
                        .setTitle("📅 Attendance on " + targetDate)
                        .setMessage(sb.toString().trim())
                        .setPositiveButton("Close", null)
                        .show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadLiveAttendance(String studentName) {
        AttendanceData.listenStudentAttendance(studentName, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (containerSubjects == null) return;
                containerSubjects.removeAllViews();

                int totalAttendedAll = 0;
                int totalLecturesAll = 0;

                if (snapshot.exists()) {
                    for (DataSnapshot subDs : snapshot.getChildren()) {
                        String subName = subDs.getKey();
                        Long a = subDs.child("attended").getValue(Long.class);
                        Long t = subDs.child("total").getValue(Long.class);

                        long attended = Objects.requireNonNullElse(a, 0L);
                        long total = Objects.requireNonNullElse(t, 0L);

                        totalAttendedAll += (int) attended;
                        totalLecturesAll += (int) total;

                        int p = total > 0 ? (int) Math.round(((double) attended / total) * 100) : 0;

                        MaterialCardView card = new MaterialCardView(StudentAttendanceActivity.this);
                        card.setRadius(12);
                        card.setCardElevation(2);
                        card.setCardBackgroundColor(0xFFFFFFFF);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        lp.setMargins(0, 0, 0, 16);
                        card.setLayoutParams(lp);

                        LinearLayout inner = new LinearLayout(StudentAttendanceActivity.this);
                        inner.setOrientation(LinearLayout.VERTICAL);
                        inner.setPadding(24, 20, 24, 20);

                        TextView tvSub = new TextView(StudentAttendanceActivity.this);
                        tvSub.setText(subName);
                        tvSub.setTextSize(16);
                        tvSub.setTypeface(null, android.graphics.Typeface.BOLD);
                        tvSub.setTextColor(0xFF1E293B);

                        TextView tvCount = new TextView(StudentAttendanceActivity.this);
                        tvCount.setText(getString(R.string.attended_format, attended, total));
                        tvCount.setTextSize(14);
                        tvCount.setTextColor(p >= 75 ? 0xFF16A34A : 0xFFDC2626);
                        tvCount.setPadding(0, 6, 0, 0);

                        inner.addView(tvSub);
                        inner.addView(tvCount);
                        card.addView(inner);
                        containerSubjects.addView(card);
                    }
                }

                int overall = totalLecturesAll > 0 ? (int) Math.round(((double) totalAttendedAll / totalLecturesAll) * 100) : 0;
                if (tvOverallPercentage != null) {
                    tvOverallPercentage.setText(overall > 0 ? overall + "%" : "");
                }
                if (pbOverall != null) {
                    pbOverall.setProgress(overall);
                }

                if (tvExamStatus != null) {
                    if (totalLecturesAll == 0) {
                        tvExamStatus.setText("Status: No Attendance Recorded (0%)");
                        tvExamStatus.setTextColor(0xFFBBF7D0);
                    } else if (overall >= 75) {
                        tvExamStatus.setText("✅ EXAM ELIGIBLE: Safe Attendance (" + overall + "%)");
                        tvExamStatus.setTextColor(0xFFBBF7D0);
                    } else {
                        tvExamStatus.setText("🚨 DEFAULTER WARNING: Below 75% (" + overall + "%)");
                        tvExamStatus.setTextColor(0xFFFF3333);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}