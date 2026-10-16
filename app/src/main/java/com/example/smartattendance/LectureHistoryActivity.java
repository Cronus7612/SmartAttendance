package com.example.smartattendance;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class LectureHistoryActivity extends AppCompatActivity {

    private LinearLayout containerLectureHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecture_history);

        TextView btnBack = findViewById(R.id.btnBack);
        containerLectureHistory = findViewById(R.id.containerLectureHistory);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        loadLectureHistory();
    }

    private void loadLectureHistory() {
        CloudRepository.attendanceLogsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (containerLectureHistory == null) return;
                containerLectureHistory.removeAllViews();

                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    TextView tvEmpty = new TextView(LectureHistoryActivity.this);
                    tvEmpty.setText("No saved lecture attendance history found.");
                    tvEmpty.setTextColor(0xFF64748B);
                    tvEmpty.setPadding(10, 20, 10, 20);
                    containerLectureHistory.addView(tvEmpty);
                } else {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String attId = ds.child("attendanceId").getValue(String.class);
                        String className = ds.child("className").getValue(String.class);
                        String subject = ds.child("subject").getValue(String.class);
                        String date = ds.child("date").getValue(String.class);
                        String lecNum = ds.child("lectureNumber").getValue(String.class);
                        String lecType = ds.child("lectureType").getValue(String.class);
                        Long pCount = ds.child("presentCount").getValue(Long.class);
                        Long tCount = ds.child("totalCount").getValue(Long.class);

                        long present = pCount != null ? pCount : 0;
                        long total = tCount != null ? tCount : 0;

                        TextView tvLog = new TextView(LectureHistoryActivity.this);
                        tvLog.setText("• Log #" + (attId != null ? attId : "2610") + " | " + (className != null ? className : "TY BCA") + " - " + (subject != null ? subject : "Core Java") + " (" + (lecType != null ? lecType : "Regular") + ")\n  Date: " + (date != null ? date : "2026-10-10") + " | " + (lecNum != null ? lecNum : "Lecture 1") + " : " + present + "/" + total + " Present");
                        tvLog.setTextSize(14);
                        tvLog.setPadding(0, 10, 0, 10);
                        tvLog.setTextColor(0xFF334155);
                        containerLectureHistory.addView(tvLog, 0); // Newest at top
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}