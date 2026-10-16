package com.example.smartattendance;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class LeaveApprovalActivity extends AppCompatActivity {

    private LinearLayout containerLeaves;
    private String teacherName = "Faculty";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leave_approval);

        TextView btnBack = findViewById(R.id.btnBack);
        containerLeaves = findViewById(R.id.containerLeaves);

        String name = getIntent().getStringExtra("TEACHER_NAME");
        if (name != null && !name.isEmpty()) teacherName = name;

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        loadLeavesFromFirebase();
    }

    private void loadLeavesFromFirebase() {
        CloudRepository.leaveRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (containerLeaves == null) return;
                containerLeaves.removeAllViews();

                if (!snapshot.exists()) {
                    TextView tvEmpty = new TextView(LeaveApprovalActivity.this);
                    tvEmpty.setText("No leave applications found.");
                    tvEmpty.setPadding(10, 20, 10, 20);
                    tvEmpty.setTextColor(0xFF64748B);
                    containerLeaves.addView(tvEmpty);
                } else {
                    TextView tvPendingHeader = new TextView(LeaveApprovalActivity.this);
                    tvPendingHeader.setText("📌 PENDING LEAVE APPLICATIONS:");
                    tvPendingHeader.setTypeface(null, android.graphics.Typeface.BOLD);
                    tvPendingHeader.setTextColor(0xFFD97706);
                    tvPendingHeader.setPadding(0, 10, 0, 10);
                    containerLeaves.addView(tvPendingHeader);

                    int pendingCount = 0;
                    List<DataSnapshot> historyList = new ArrayList<>();

                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String status = ds.child("status").getValue(String.class);

                        if ("PENDING".equalsIgnoreCase(status)) {
                            pendingCount++;
                            String leaveId = ds.getKey();
                            String applicantName = ds.child("applicantName").getValue(String.class);
                            String roll = ds.child("applicantRollOrId").getValue(String.class);
                            String reason = ds.child("reason").getValue(String.class);

                            LinearLayout row = new LinearLayout(LeaveApprovalActivity.this);
                            row.setOrientation(LinearLayout.VERTICAL);
                            row.setPadding(0, 12, 0, 16);

                            TextView tvInfo = new TextView(LeaveApprovalActivity.this);
                            tvInfo.setText("Applicant: " + applicantName + " (#" + roll + ")\nReason: " + reason + "\nStatus: PENDING 🟡");
                            tvInfo.setTextSize(14);
                            tvInfo.setTextColor(0xFF1E293B);

                            LinearLayout btnRow = new LinearLayout(LeaveApprovalActivity.this);
                            btnRow.setOrientation(LinearLayout.HORIZONTAL);

                            TextView btnApprove = new TextView(LeaveApprovalActivity.this);
                            btnApprove.setText("✓ Approve");
                            btnApprove.setTextColor(0xFF16A34A);
                            btnApprove.setPadding(0, 8, 30, 8);
                            btnApprove.setTypeface(null, android.graphics.Typeface.BOLD);
                            btnApprove.setOnClickListener(v -> {
                                if (leaveId != null) CloudRepository.updateLeaveStatus(leaveId, "APPROVED", applicantName, reason);
                                Toast.makeText(LeaveApprovalActivity.this, "Leave Approved by " + teacherName + "!", Toast.LENGTH_SHORT).show();
                            });

                            TextView btnReject = new TextView(LeaveApprovalActivity.this);
                            btnReject.setText("✕ Reject");
                            btnReject.setTextColor(0xFFDC2626);
                            btnReject.setPadding(0, 8, 0, 8);
                            btnReject.setTypeface(null, android.graphics.Typeface.BOLD);
                            btnReject.setOnClickListener(v -> {
                                if (leaveId != null) CloudRepository.updateLeaveStatus(leaveId, "REJECTED", applicantName, reason);
                                Toast.makeText(LeaveApprovalActivity.this, "Leave Rejected by " + teacherName + "!", Toast.LENGTH_SHORT).show();
                            });

                            btnRow.addView(btnApprove);
                            btnRow.addView(btnReject);

                            row.addView(tvInfo);
                            row.addView(btnRow);
                            containerLeaves.addView(row);
                        } else {
                            historyList.add(ds);
                        }
                    }

                    if (pendingCount == 0) {
                        TextView tvNoPending = new TextView(LeaveApprovalActivity.this);
                        tvNoPending.setText("No pending leave applications.\n");
                        tvNoPending.setTextColor(0xFF64748B);
                        containerLeaves.addView(tvNoPending);
                    }

                    // Leave History Log
                    TextView tvHistoryHeader = new TextView(LeaveApprovalActivity.this);
                    tvHistoryHeader.setText("📜 PROCESSED LEAVE HISTORY LOG:");
                    tvHistoryHeader.setTypeface(null, android.graphics.Typeface.BOLD);
                    tvHistoryHeader.setTextColor(0xFF2563EB);
                    tvHistoryHeader.setPadding(0, 20, 0, 10);
                    containerLeaves.addView(tvHistoryHeader);

                    if (historyList.isEmpty()) {
                        TextView tvNoHistory = new TextView(LeaveApprovalActivity.this);
                        tvNoHistory.setText("No processed leave history.");
                        tvNoHistory.setTextColor(0xFF64748B);
                        containerLeaves.addView(tvNoHistory);
                    } else {
                        for (DataSnapshot ds : historyList) {
                            String applicantName = ds.child("applicantName").getValue(String.class);
                            String roll = ds.child("applicantRollOrId").getValue(String.class);
                            String reason = ds.child("reason").getValue(String.class);
                            String status = ds.child("status").getValue(String.class);

                            String badge = "APPROVED 🟢";
                            if ("REJECTED".equalsIgnoreCase(status)) badge = "REJECTED 🔴";

                            TextView tvHistInfo = new TextView(LeaveApprovalActivity.this);
                            tvHistInfo.setText("• " + applicantName + " (#" + roll + ") - " + reason + "\n  Decision: " + badge);
                            tvHistInfo.setTextSize(13);
                            tvHistInfo.setPadding(0, 6, 0, 12);
                            tvHistInfo.setTextColor(0xFF334155);
                            containerLeaves.addView(tvHistInfo);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}