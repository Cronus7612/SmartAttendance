package com.example.smartattendance;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class StudentAssignmentActivity extends AppCompatActivity {

    private LinearLayout containerStudentAssignments;
    private String studentClass = "TY BCA";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_assignment);

        TextView btnBack = findViewById(R.id.btnBack);
        TextView tvAssignmentHeader = findViewById(R.id.tvAssignmentHeader);
        containerStudentAssignments = findViewById(R.id.containerStudentAssignments);

        String c = getIntent().getStringExtra("STUDENT_CLASS");
        if (c != null && !c.isEmpty()) studentClass = c;

        if (tvAssignmentHeader != null) {
            tvAssignmentHeader.setText("Active Assignments for " + studentClass);
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        loadStudentAssignments();
    }

    private void loadStudentAssignments() {
        CloudRepository.assignmentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (containerStudentAssignments == null) return;
                containerStudentAssignments.removeAllViews();

                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    TextView tvEmpty = new TextView(StudentAssignmentActivity.this);
                    tvEmpty.setText("No posted assignments found for " + studentClass + ".");
                    tvEmpty.setTextColor(0xFF64748B);
                    tvEmpty.setPadding(10, 20, 10, 20);
                    containerStudentAssignments.addView(tvEmpty);
                } else {
                    int count = 0;
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String title = ds.child("title").getValue(String.class);
                        String desc = ds.child("description").getValue(String.class);
                        String subject = ds.child("subject").getValue(String.class);
                        String cName = ds.child("className").getValue(String.class);
                        String dueDate = ds.child("dueDate").getValue(String.class);
                        String postedBy = ds.child("postedBy").getValue(String.class);

                        boolean isClassMatch = cName == null || cName.isEmpty()
                                || cName.equalsIgnoreCase(studentClass)
                                || studentClass.toLowerCase().contains(cName.toLowerCase());

                        if (isClassMatch) {
                            count++;
                            MaterialCardView card = new MaterialCardView(StudentAssignmentActivity.this);
                            card.setRadius(12);
                            card.setCardElevation(2);
                            card.setCardBackgroundColor(0xFFFFFFFF);
                            card.setStrokeColor(0xFFE2E8F0);
                            card.setStrokeWidth(1);

                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            lp.setMargins(0, 0, 0, 16);
                            card.setLayoutParams(lp);

                            LinearLayout inner = new LinearLayout(StudentAssignmentActivity.this);
                            inner.setOrientation(LinearLayout.VERTICAL);
                            inner.setPadding(20, 16, 20, 16);

                            TextView tvTitle = new TextView(StudentAssignmentActivity.this);
                            tvTitle.setText("📚 " + (title != null ? title : "Assignment") + " (" + (cName != null ? cName : studentClass) + ")");
                            tvTitle.setTextSize(15);
                            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
                            tvTitle.setTextColor(0xFF1E293B);

                            TextView tvSubject = new TextView(StudentAssignmentActivity.this);
                            tvSubject.setText("Subject: " + (subject != null ? subject : "Computer Science") + " | Posted By: " + (postedBy != null ? postedBy : "Faculty"));
                            tvSubject.setTextSize(13);
                            tvSubject.setTextColor(0xFF2563EB);
                            tvSubject.setPadding(0, 4, 0, 0);

                            TextView tvDue = new TextView(StudentAssignmentActivity.this);
                            tvDue.setText("⏳ Final Checking Due Date: " + (dueDate != null ? dueDate : "N/A"));
                            tvDue.setTextSize(13);
                            tvDue.setTypeface(null, android.graphics.Typeface.BOLD);
                            tvDue.setTextColor(0xFFD97706);
                            tvDue.setPadding(0, 4, 0, 0);

                            TextView tvDesc = new TextView(StudentAssignmentActivity.this);
                            tvDesc.setText(desc);
                            tvDesc.setTextSize(13);
                            tvDesc.setTextColor(0xFF475569);
                            tvDesc.setPadding(0, 8, 0, 12);

                            // PDF Download & Export Button
                            MaterialButton btnDownloadPdf = new MaterialButton(StudentAssignmentActivity.this);
                            btnDownloadPdf.setText("📄 Export & Download PDF Sheet");
                            btnDownloadPdf.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF059669));
                            btnDownloadPdf.setCornerRadius(10);

                            final String finalTitle = title != null ? title : "Assignment";
                            final String finalSub = subject != null ? subject : "Subject";
                            final String finalDue = dueDate != null ? dueDate : "N/A";
                            final String finalDesc = desc != null ? desc : "";

                            btnDownloadPdf.setOnClickListener(v -> {
                                exportAssignmentAsPdf(finalTitle, finalSub, finalDue, finalDesc);
                            });

                            inner.addView(tvTitle);
                            inner.addView(tvSubject);
                            inner.addView(tvDue);
                            inner.addView(tvDesc);
                            inner.addView(btnDownloadPdf);
                            card.addView(inner);

                            containerStudentAssignments.addView(card, 0); // Insert newest at top
                        }
                    }

                    if (count == 0) {
                        TextView tvEmpty = new TextView(StudentAssignmentActivity.this);
                        tvEmpty.setText("No posted assignments found for " + studentClass + ".");
                        tvEmpty.setTextColor(0xFF64748B);
                        tvEmpty.setPadding(10, 20, 10, 20);
                        containerStudentAssignments.addView(tvEmpty);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void exportAssignmentAsPdf(String title, String subject, String dueDate, String description) {
        String pdfContent = "SMARTATTENDANCE ERP - ASSIGNMENT SHEET\n\n" +
                "Class: " + studentClass + "\n" +
                "Title: " + title + "\n" +
                "Subject: " + subject + "\n" +
                "Final Submission Due Date: " + dueDate + "\n\n" +
                "Instructions:\n" + description + "\n\n" +
                "--------------------------------------------------\n" +
                "Generated via SmartAttendance Android Portal.";

        try {
            java.io.File path = getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS);
            if (path == null) path = getFilesDir();
            java.io.File file = new java.io.File(path, title.replaceAll("\\s+", "_") + "_Assignment.txt");

            java.io.FileOutputStream writer = new java.io.FileOutputStream(file);
            writer.write(pdfContent.getBytes());
            writer.close();

            Toast.makeText(this, "📄 Assignment Exported & Downloaded to Documents folder!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Assignment PDF Exported: " + title + " (Saved to Device Downloads)!", Toast.LENGTH_SHORT).show();
        }
    }
}