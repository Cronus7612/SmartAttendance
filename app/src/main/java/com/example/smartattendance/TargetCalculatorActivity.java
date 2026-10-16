package com.example.smartattendance;

import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class TargetCalculatorActivity extends AppCompatActivity {

    private TextView tvTargetCalculatorInfo;
    private String studentName = "Aditya Thete";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_target_calculator);

        TextView btnBack = findViewById(R.id.btnBack);
        tvTargetCalculatorInfo = findViewById(R.id.tvTargetCalculatorInfo);

        String name = getIntent().getStringExtra("STUDENT_NAME");
        if (name != null && !name.isEmpty()) studentName = name;

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        loadTargetAnalytics(studentName);
    }

    private void loadTargetAnalytics(String studentName) {
        AttendanceData.listenStudentAttendance(studentName, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int totalAttendedAll = 0;
                int totalLecturesAll = 0;

                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Long a = child.child("attended").getValue(Long.class);
                        Long t = child.child("total").getValue(Long.class);
                        if (a != null) totalAttendedAll += a;
                        if (t != null) totalLecturesAll += t;
                    }
                }

                int overall = totalLecturesAll > 0 ? (int) Math.round(((double) totalAttendedAll / totalLecturesAll) * 100) : 0;
                updateTargetCalculator(totalAttendedAll, totalLecturesAll, overall);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateTargetCalculator(int attended, int total, int percentage) {
        if (tvTargetCalculatorInfo == null) return;

        if (total == 0) {
            tvTargetCalculatorInfo.setText("No attendance history found yet. Attend upcoming lectures to build attendance history.");
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