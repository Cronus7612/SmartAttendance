package com.example.smartattendance;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.Objects;

public class StudentLeaveTrackerActivity extends AppCompatActivity {

    private LinearLayout containerLeaveHistory;
    private String studentName = "Aditya Thete";
    private String studentRoll = "1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_leave_tracker);

        TextView btnBack = findViewById(R.id.btnBack);
        Spinner spinnerLeaveType = findViewById(R.id.spinnerLeaveType);
        EditText etLeaveReason = findViewById(R.id.etLeaveReason);
        MaterialButton btnSubmitLeave = findViewById(R.id.btnSubmitLeave);
        MaterialButton btnClearLeaveHistory = findViewById(R.id.btnClearLeaveHistory);
        containerLeaveHistory = findViewById(R.id.containerLeaveHistory);

        String n = getIntent().getStringExtra("STUDENT_NAME");
        String r = getIntent().getStringExtra("STUDENT_ROLL");
        if (n != null && !n.isEmpty()) studentName = n;
        if (r != null && !r.isEmpty()) studentRoll = r;

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        String[] leaveTypes = {
                "Medical Leave",
                "Personal Leave",
                "NSS / NCC Duty",
                "Sports Event",
                "Family Function",
                "Other"
        };
        if (spinnerLeaveType != null) {
            spinnerLeaveType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, leaveTypes));
        }

        if (btnSubmitLeave != null) {
            btnSubmitLeave.setOnClickListener(v -> {
                String type = spinnerLeaveType != null ? spinnerLeaveType.getSelectedItem().toString() : "Medical Leave";
                String reason = etLeaveReason != null ? etLeaveReason.getText().toString().trim() : "";
                String finalReason = "[" + type + "] " + (reason.isEmpty() ? "No additional notes" : reason);

                CloudRepository.submitLeaveApplication(studentName, studentRoll, type, finalReason);
                Toast.makeText(this, "Leave application submitted to HOD/Principal!", Toast.LENGTH_LONG).show();
                if (etLeaveReason != null) etLeaveReason.setText("");
            });
        }

        if (btnClearLeaveHistory != null) {
            btnClearLeaveHistory.setOnClickListener(v -> clearPersonalLeaveHistory());
        }

        loadLeaveHistory();
    }

    private void clearPersonalLeaveHistory() {
        CloudRepository.leaveRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String applicantName = ds.child("applicantName").getValue(String.class);
                        String applicantRoll = ds.child("applicantRollOrId").getValue(String.class);

                        boolean isNameMatch = applicantName != null && applicantName.equalsIgnoreCase(studentName);
                        boolean isRollMatch = applicantRoll != null && applicantRoll.equalsIgnoreCase(studentRoll);

                        if (isNameMatch || isRollMatch) {
                            ds.getRef().removeValue();
                        }
                    }
                    Toast.makeText(StudentLeaveTrackerActivity.this, "Your Leave History Cleared!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadLeaveHistory() {
        CloudRepository.leaveRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (containerLeaveHistory == null) return;
                containerLeaveHistory.removeAllViews();

                if (!snapshot.exists()) {
                    TextView tvEmpty = new TextView(StudentLeaveTrackerActivity.this);
                    tvEmpty.setText("No previous leave applications found.");
                    tvEmpty.setTextColor(0xFF64748B);
                    tvEmpty.setPadding(10, 20, 10, 20);
                    containerLeaveHistory.addView(tvEmpty);
                } else {
                    int count = 0;
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String applicantName = ds.child("applicantName").getValue(String.class);
                        String applicantRoll = ds.child("applicantRollOrId").getValue(String.class);
                        String reason = ds.child("reason").getValue(String.class);
                        String status = ds.child("status").getValue(String.class);
                        Long actionTs = ds.child("actionTimestamp").getValue(Long.class);

                        boolean isNameMatch = applicantName != null && applicantName.equalsIgnoreCase(studentName);
                        boolean isRollMatch = applicantRoll != null && applicantRoll.equalsIgnoreCase(studentRoll);

                        if (isNameMatch || isRollMatch) {
                            count++;
                            String badge = "PENDING 🟡";
                            String timeStr = "";

                            if (actionTs != null && actionTs > 0) {
                                timeStr = " (" + CloudRepository.formatTimestamp(actionTs) + ")";
                            }

                            if ("APPROVED".equalsIgnoreCase(status)) badge = "APPROVED 🟢" + timeStr;
                            else if ("REJECTED".equalsIgnoreCase(status)) badge = "REJECTED 🔴" + timeStr;

                            MaterialCardView card = new MaterialCardView(StudentLeaveTrackerActivity.this);
                            card.setRadius(12);
                            card.setCardElevation(2);
                            card.setCardBackgroundColor(0xFFFFFFFF);
                            card.setStrokeColor(0xFFE2E8F0);
                            card.setStrokeWidth(1);

                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            lp.setMargins(0, 0, 0, 16);
                            card.setLayoutParams(lp);

                            LinearLayout inner = new LinearLayout(StudentLeaveTrackerActivity.this);
                            inner.setOrientation(LinearLayout.VERTICAL);
                            inner.setPadding(20, 16, 20, 16);

                            TextView tvReason = new TextView(StudentLeaveTrackerActivity.this);
                            tvReason.setText("• Reason: " + Objects.requireNonNullElse(reason, "N/A"));
                            tvReason.setTextSize(14);
                            tvReason.setTypeface(null, android.graphics.Typeface.BOLD);
                            tvReason.setTextColor(0xFF1E293B);

                            TextView tvBadge = new TextView(StudentLeaveTrackerActivity.this);
                            tvBadge.setText("  Status: " + badge);
                            tvBadge.setTextSize(13);
                            tvBadge.setTextColor(0xFF334155);
                            tvBadge.setPadding(0, 4, 0, 0);

                            inner.addView(tvReason);
                            inner.addView(tvBadge);
                            card.addView(inner);

                            containerLeaveHistory.addView(card, 0);
                        }
                    }

                    if (count == 0) {
                        TextView tvEmpty = new TextView(StudentLeaveTrackerActivity.this);
                        tvEmpty.setText("No previous leave applications found.");
                        tvEmpty.setTextColor(0xFF64748B);
                        tvEmpty.setPadding(10, 20, 10, 20);
                        containerLeaveHistory.addView(tvEmpty);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}