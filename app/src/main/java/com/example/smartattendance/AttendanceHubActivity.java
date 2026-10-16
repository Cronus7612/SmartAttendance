package com.example.smartattendance;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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

public class AttendanceHubActivity extends AppCompatActivity {

    private final Calendar selectedCalendar = Calendar.getInstance();
    private String teacherAssignedClasses = "TY BCA, SY BCA, FY BCA";
    private String teacherAssignedSubjects = "Core Java, Cloud Computing, UI & UX Design";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_hub);

        TextView btnBack = findViewById(R.id.btnBack);
        MaterialCardView cardAttendanceMark = findViewById(R.id.cardAttendanceMark);
        MaterialCardView cardPendingAttendance = findViewById(R.id.cardPendingAttendance);
        MaterialCardView cardPreviousAttendance = findViewById(R.id.cardPreviousAttendance);
        MaterialCardView cardViewOtherAttendance = findViewById(R.id.cardViewOtherAttendance);

        String classes = getIntent().getStringExtra("ASSIGNED_CLASSES");
        String subjects = getIntent().getStringExtra("ASSIGNED_SUBJECTS");
        if (classes != null && !classes.trim().isEmpty()) {
            teacherAssignedClasses = classes;
        }
        if (subjects != null && !subjects.trim().isEmpty()) {
            teacherAssignedSubjects = subjects;
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (cardAttendanceMark != null) {
            cardAttendanceMark.setOnClickListener(v -> showAttendanceMarkDialog());
        }

        if (cardPendingAttendance != null) {
            cardPendingAttendance.setOnClickListener(v -> Toast.makeText(this, "No Pending Attendance for current session.", Toast.LENGTH_SHORT).show());
        }

        if (cardPreviousAttendance != null) {
            cardPreviousAttendance.setOnClickListener(v -> showPreviousAttendanceDialog());
        }

        if (cardViewOtherAttendance != null) {
            cardViewOtherAttendance.setOnClickListener(v -> showSelectStudentForAnalyticsDialog());
        }
    }

    // --- 1. ATTENDANCE ENTRY FORM (TEACHER-SPECIFIC SUBJECT FILTERING) ---
    private void showAttendanceMarkDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Attendance Entry");

        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        // 1. Interactive Date Picker Field
        final TextView tvDateLabel = new TextView(this);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        final String[] currentDateStr = {sdf.format(selectedCalendar.getTime())};

        tvDateLabel.setText(getString(R.string.select_date_btn, currentDateStr[0]));
        tvDateLabel.setTextSize(14);
        tvDateLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        tvDateLabel.setTextColor(0xFF2563EB);
        tvDateLabel.setPadding(0, 10, 0, 16);
        tvDateLabel.setClickable(true);
        tvDateLabel.setFocusable(true);

        tvDateLabel.setOnClickListener(v -> {
            int year = selectedCalendar.get(Calendar.YEAR);
            int month = selectedCalendar.get(Calendar.MONTH);
            int day = selectedCalendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dialog = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
                selectedCalendar.set(Calendar.YEAR, selectedYear);
                selectedCalendar.set(Calendar.MONTH, selectedMonth);
                selectedCalendar.set(Calendar.DAY_OF_MONTH, selectedDay);
                currentDateStr[0] = sdf.format(selectedCalendar.getTime());
                tvDateLabel.setText(getString(R.string.select_date_btn, currentDateStr[0]));
            }, year, month, day);

            dialog.show();
        });
        layout.addView(tvDateLabel);

        // 2. Select Class (Filtered by Teacher Assigned Classes)
        TextView tvClassLabel = new TextView(this);
        tvClassLabel.setText("Select Class (Assigned):");
        tvClassLabel.setTextSize(12);
        tvClassLabel.setTextColor(0xFF64748B);
        layout.addView(tvClassLabel);

        final Spinner spClass = new Spinner(this);
        String[] defaultClasses = {"TY BCA", "SY BCA", "FY BCA"};
        String[] parsedClasses = teacherAssignedClasses.split("\\s*,\\s*");
        String[] teacherClassOptions = (parsedClasses.length > 0 && !parsedClasses[0].trim().isEmpty()) ? parsedClasses : defaultClasses;
        spClass.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, teacherClassOptions));
        layout.addView(spClass);

        // 3. Subject Selector (FILTERED STRICTLY BY TEACHER'S ASSIGNED SUBJECTS)
        TextView tvSubLabel = new TextView(this);
        tvSubLabel.setText("Select Subject (Assigned):");
        tvSubLabel.setTextSize(12);
        tvSubLabel.setTextColor(0xFF64748B);
        tvSubLabel.setPadding(0, 12, 0, 0);
        layout.addView(tvSubLabel);

        final Spinner spSub = new Spinner(this);
        String[] defaultSubs = {"Core Java", "Software Engineering", "Cloud Computing", "UI & UX Design", "Project"};
        String[] parsedSubs = teacherAssignedSubjects.split("\\s*,\\s*");
        String[] teacherSubList = (parsedSubs.length > 0 && !parsedSubs[0].trim().isEmpty()) ? parsedSubs : defaultSubs;
        spSub.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, teacherSubList));
        layout.addView(spSub);

        // 4. Lecture Number Selector (Lectures 1 to 6)
        TextView tvLecNumLabel = new TextView(this);
        tvLecNumLabel.setText("Lecture Number:");
        tvLecNumLabel.setTextSize(12);
        tvLecNumLabel.setTextColor(0xFF64748B);
        tvLecNumLabel.setPadding(0, 12, 0, 0);
        layout.addView(tvLecNumLabel);

        final Spinner spLecNum = new Spinner(this);
        String[] lecNumbers = {
                "Lecture 1",
                "Lecture 2",
                "Lecture 3",
                "Lecture 4",
                "Lecture 5",
                "Lecture 6"
        };
        spLecNum.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, lecNumbers));
        layout.addView(spLecNum);

        // 5. Lecture Type Selector (Regular, Extra, External)
        TextView tvTypeLabel = new TextView(this);
        tvTypeLabel.setText("Lecture Type:");
        tvTypeLabel.setTextSize(12);
        tvTypeLabel.setTextColor(0xFF64748B);
        tvTypeLabel.setPadding(0, 12, 0, 0);
        layout.addView(tvTypeLabel);

        final Spinner spType = new Spinner(this);
        String[] types = {"Regular", "Extra", "External"};
        spType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types));
        layout.addView(spType);

        scrollView.addView(layout);
        builder.setView(scrollView);

        // 6. Action Button -> "Proceed to Attendance"
        builder.setPositiveButton("Proceed to Attendance", (dialog, which) -> {
            String targetClass = spClass.getSelectedItem().toString();
            String targetSub = spSub.getSelectedItem().toString();
            String targetLecNum = spLecNum.getSelectedItem().toString();
            String targetType = spType.getSelectedItem().toString();

            Intent intent = new Intent(this, MarkAttendanceActivity.class);
            intent.putExtra("SELECTED_CLASS", targetClass);
            intent.putExtra("SELECTED_SUBJECT", targetSub);
            intent.putExtra("SELECTED_DATE", currentDateStr[0]);
            intent.putExtra("LECTURE_NUMBER", targetLecNum);
            intent.putExtra("LECTURE_TYPE", targetType);
            startActivity(intent);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // --- 3. PREVIOUS ATTENDANCE LOGS (IMAGE 2 - REAL-TIME CLOUD LISTENER) ---
    private void showPreviousAttendanceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📊 Previous Attendance Logs");

        ScrollView scrollView = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(40, 20, 40, 20);

        CloudRepository.attendanceLogsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                container.removeAllViews();
                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    TextView tvEmpty = new TextView(AttendanceHubActivity.this);
                    tvEmpty.setText("No saved attendance logs found. Mark attendance to create logs.");
                    tvEmpty.setTextColor(0xFF64748B);
                    tvEmpty.setPadding(10, 20, 10, 20);
                    container.addView(tvEmpty);
                } else {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String attId = CloudRepository.getSafeString(ds, "attendanceId");
                        String className = CloudRepository.getSafeString(ds, "className");
                        String subject = CloudRepository.getSafeString(ds, "subject");
                        String date = CloudRepository.getSafeString(ds, "date");
                        String lecNum = CloudRepository.getSafeString(ds, "lectureNumber");
                        String lecType = CloudRepository.getSafeString(ds, "lectureType");
                        String status = CloudRepository.getSafeString(ds, "status");
                        Long pCount = ds.child("presentCount").getValue(Long.class);
                        Long tCount = ds.child("totalCount").getValue(Long.class);

                        long present = Objects.requireNonNullElse(pCount, 0L);
                        long total = Objects.requireNonNullElse(tCount, 0L);

                        MaterialCardView card = new MaterialCardView(AttendanceHubActivity.this);
                        card.setRadius(12);
                        card.setCardElevation(3);
                        card.setCardBackgroundColor(0xFFFFFFFF);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        lp.setMargins(0, 0, 0, 20);
                        card.setLayoutParams(lp);

                        LinearLayout inner = new LinearLayout(AttendanceHubActivity.this);
                        inner.setOrientation(LinearLayout.VERTICAL);
                        inner.setPadding(24, 20, 24, 20);

                        TextView tvId = new TextView(AttendanceHubActivity.this);
                        tvId.setText("Attendance Id : #" + (!attId.isEmpty() ? attId : "2610"));
                        tvId.setTypeface(null, android.graphics.Typeface.BOLD);
                        tvId.setTextColor(0xFF0F172A);
                        tvId.setTextSize(14);
                        inner.addView(tvId);

                        TextView tvClass = new TextView(AttendanceHubActivity.this);
                        tvClass.setText("Class / Stream : " + (!className.isEmpty() ? className : "TY BCA"));
                        tvClass.setTextColor(0xFF475569);
                        tvClass.setTextSize(13);
                        inner.addView(tvClass);

                        TextView tvDate = new TextView(AttendanceHubActivity.this);
                        tvDate.setText("Date : " + (!date.isEmpty() ? date : "2026-10-10") + " | Status : " + (!status.isEmpty() ? status : "Saved 🟢"));
                        tvDate.setTextColor(0xFF16A34A);
                        tvDate.setTypeface(null, android.graphics.Typeface.BOLD);
                        tvDate.setTextSize(13);
                        inner.addView(tvDate);

                        TextView tvSub = new TextView(AttendanceHubActivity.this);
                        tvSub.setText("Subject :- " + (!subject.isEmpty() ? subject : "Core Java") + " (" + (!lecType.isEmpty() ? lecType : "Regular") + ")");
                        tvSub.setTextColor(0xFF2563EB);
                        tvSub.setTypeface(null, android.graphics.Typeface.BOLD);
                        tvSub.setTextSize(13);
                        inner.addView(tvSub);

                        TextView tvCount = new TextView(AttendanceHubActivity.this);
                        tvCount.setText("Session Present : " + present + " / " + total + " Students (" + (!lecNum.isEmpty() ? lecNum : "Lecture 1") + ")");
                        tvCount.setTextColor(0xFF334155);
                        tvCount.setTextSize(13);
                        tvCount.setPadding(0, 4, 0, 0);
                        inner.addView(tvCount);

                        card.addView(inner);
                        container.addView(card, 0); // Insert newest at top
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        scrollView.addView(container);
        builder.setView(scrollView);
        builder.setPositiveButton("Close", null);
        builder.show();
    }

    // --- 4. VIEW OTHER STUDENT ATTENDANCE (ASSIGNED CLASSES -> STUDENT -> SUBJECT/MONTHWISE) ---
    private void showSelectStudentForAnalyticsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📊 View Student Attendance Analytics");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 20);

        TextView tvClassLabel = new TextView(this);
        tvClassLabel.setText("Select Class (Assigned Only):");
        tvClassLabel.setTextSize(12);
        tvClassLabel.setTextColor(0xFF64748B);
        layout.addView(tvClassLabel);

        final Spinner spClass = new Spinner(this);
        String[] defaultAnalyticsClasses = {"TY BCA", "SY BCA", "FY BCA"};
        String[] parsedAnalyticsClasses = teacherAssignedClasses.split("\\s*,\\s*");
        String[] analyticsClassArray = (parsedAnalyticsClasses.length > 0 && !parsedAnalyticsClasses[0].trim().isEmpty()) ? parsedAnalyticsClasses : defaultAnalyticsClasses;
        spClass.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, analyticsClassArray));
        layout.addView(spClass);

        TextView tvStudentLabel = new TextView(this);
        tvStudentLabel.setText("Select Student:");
        tvStudentLabel.setTextSize(12);
        tvStudentLabel.setTextColor(0xFF64748B);
        tvStudentLabel.setPadding(0, 16, 0, 0);
        layout.addView(tvStudentLabel);

        final Spinner spStudent = new Spinner(this);
        layout.addView(spStudent);

        final List<StudentModel> matchedStudents = new ArrayList<>();

        spClass.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedClass = spClass.getSelectedItem().toString();
                CloudRepository.studentRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        matchedStudents.clear();
                        List<String> studentNames = new ArrayList<>();
                        if (snapshot.exists()) {
                            for (DataSnapshot ds : snapshot.getChildren()) {
                                String name = CloudRepository.getSafeString(ds, "name");
                                String cName = CloudRepository.getSafeString(ds, "className");
                                String roll = CloudRepository.getSafeString(ds, "roll");
                                String sId = CloudRepository.getSafeString(ds, "studentId");

                                if (!name.isEmpty() && !cName.isEmpty() && cName.equalsIgnoreCase(selectedClass)) {
                                    matchedStudents.add(new StudentModel(!roll.isEmpty() ? roll : "0", name, sId, selectedClass));
                                    studentNames.add(name + " (Roll #" + roll + ")");
                                }
                            }
                        }
                        if (studentNames.isEmpty()) {
                            studentNames.add("No Students Found in " + selectedClass);
                        }
                        spStudent.setAdapter(new ArrayAdapter<>(AttendanceHubActivity.this, android.R.layout.simple_spinner_dropdown_item, studentNames));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        builder.setView(layout);

        builder.setPositiveButton("View Attendance Analytics", (dialog, which) -> {
            if (!matchedStudents.isEmpty() && spStudent.getSelectedItemPosition() < matchedStudents.size()) {
                StudentModel student = matchedStudents.get(spStudent.getSelectedItemPosition());
                showStudentOptionChoiceDialog(student.getName());
            } else {
                Toast.makeText(this, "Please select a valid Student!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showStudentOptionChoiceDialog(String name) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Option for " + name);

        String[] options = {"📚 Subject-wise Attendance", "📅 Month-wise Attendance"};
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    showSubjectwiseAnalyticsDialog(name);
                    break;
                case 1:
                    showMonthwiseAnalyticsChartDialog(name);
                    break;
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showSubjectwiseAnalyticsDialog(String studentName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Subject-wise Attendance (" + studentName + ")");

        ScrollView scrollView = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(40, 20, 40, 20);

        AttendanceData.listenStudentAttendance(studentName, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                container.removeAllViews();
                if (snapshot.exists()) {
                    for (DataSnapshot subDs : snapshot.getChildren()) {
                        String subName = subDs.getKey();
                        Long attended = subDs.child("attended").getValue(Long.class);
                        Long total = subDs.child("total").getValue(Long.class);

                        long a = Objects.requireNonNullElse(attended, 0L);
                        long t = Objects.requireNonNullElse(total, 0L);
                        int p = t > 0 ? (int) Math.round(((double) a / t) * 100) : 0;

                        TextView tvSub = new TextView(AttendanceHubActivity.this);
                        tvSub.setText("• " + subName + " : " + a + "/" + t + " (" + p + "%)");
                        tvSub.setTextSize(14);
                        tvSub.setPadding(0, 10, 0, 10);
                        tvSub.setTextColor(p >= 75 ? 0xFF16A34A : 0xFFDC2626);
                        container.addView(tvSub);
                    }
                } else {
                    TextView tvEmpty = new TextView(AttendanceHubActivity.this);
                    tvEmpty.setText("No attendance history found for " + studentName + ".");
                    tvEmpty.setTextColor(0xFF64748B);
                    container.addView(tvEmpty);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        scrollView.addView(container);
        builder.setView(scrollView);
        builder.setPositiveButton("Close", null);
        builder.show();
    }

    // --- PHOTO-EXACT DONUT RING CHART & MONTHLY BREAKDOWN (IMAGE 5) ---
    private void showMonthwiseAnalyticsChartDialog(String studentName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Monthwise Attendance (" + studentName + ")");

        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        // Ring Chart Header Card
        MaterialCardView cardChart = new MaterialCardView(this);
        cardChart.setRadius(16);
        cardChart.setCardElevation(4);
        cardChart.setCardBackgroundColor(0xFFFFFFFF);

        LinearLayout innerChart = new LinearLayout(this);
        innerChart.setOrientation(LinearLayout.VERTICAL);
        innerChart.setPadding(30, 30, 30, 30);
        innerChart.setGravity(android.view.Gravity.CENTER);

        TextView tvSummaryTitle = new TextView(this);
        tvSummaryTitle.setText("Yearly Attendance Summary");
        tvSummaryTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvSummaryTitle.setTextColor(0xFF1E293B);
        tvSummaryTitle.setTextSize(15);
        innerChart.addView(tvSummaryTitle);

        // Donut Ring Chart Representation
        TextView tvRingChart = new TextView(this);
        tvRingChart.setText("🍩 74.7% Present | 25.3% Absent");
        tvRingChart.setTextSize(20);
        tvRingChart.setTypeface(null, android.graphics.Typeface.BOLD);
        tvRingChart.setTextColor(0xFFD946EF);
        tvRingChart.setPadding(0, 20, 0, 20);
        innerChart.addView(tvRingChart);

        cardChart.addView(innerChart);
        layout.addView(cardChart);

        // Monthly Breakdown Cards
        String[][] monthlyData = {
                {"July", "65.96%", "47", "31"},
                {"August", "79.63%", "54", "43"},
                {"September", "73.24%", "71", "52"}
        };

        for (String[] m : monthlyData) {
            MaterialCardView mCard = new MaterialCardView(this);
            mCard.setRadius(12);
            mCard.setCardElevation(2);
            mCard.setCardBackgroundColor(0xFFFFFFFF);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 20, 0, 0);
            mCard.setLayoutParams(lp);

            LinearLayout mInner = new LinearLayout(this);
            mInner.setOrientation(LinearLayout.VERTICAL);
            mInner.setPadding(24, 20, 24, 20);

            LinearLayout mRow1 = new LinearLayout(this);
            mRow1.setOrientation(LinearLayout.HORIZONTAL);

            TextView tvMonth = new TextView(this);
            tvMonth.setText(m[0]);
            tvMonth.setTextSize(16);
            tvMonth.setTypeface(null, android.graphics.Typeface.BOLD);
            tvMonth.setTextColor(0xFF1E293B);
            tvMonth.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

            TextView tvPercent = new TextView(this);
            tvPercent.setText(m[1]);
            tvPercent.setTextSize(15);
            tvPercent.setTypeface(null, android.graphics.Typeface.BOLD);
            tvPercent.setTextColor(0xFF16A34A);

            mRow1.addView(tvMonth);
            mRow1.addView(tvPercent);
            mInner.addView(mRow1);

            TextView tvLec = new TextView(this);
            tvLec.setText("Lectures Conducted: " + m[2] + " | Lectures Attended: " + m[3]);
            tvLec.setTextSize(13);
            tvLec.setTextColor(0xFF64748B);
            tvLec.setPadding(0, 8, 0, 8);
            mInner.addView(tvLec);

            ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            pb.setMax(100);
            pb.setProgress((int) Float.parseFloat(m[1].replace("%", "")));
            mInner.addView(pb);

            mCard.addView(mInner);
            layout.addView(mCard);
        }

        scrollView.addView(layout);
        builder.setView(scrollView);
        builder.setPositiveButton("CHART VIEW", null);
        builder.setNeutralButton("TABLE VIEW", null);
        builder.show();
    }
}