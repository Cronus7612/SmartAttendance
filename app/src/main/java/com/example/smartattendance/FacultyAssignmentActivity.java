package com.example.smartattendance;

import android.app.DatePickerDialog;
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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class FacultyAssignmentActivity extends AppCompatActivity {

    private final Calendar dueCalendar = Calendar.getInstance();
    private String teacherName = "Prof. S. R. Patil";
    private String teacherAssignedClasses = "TY BCA, SY BCA, FY BCA";
    private LinearLayout containerFacultyAssignments;
    private MaterialButton btnSelectDueDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_assignment);

        TextView btnBack = findViewById(R.id.btnBack);
        EditText etAssignmentTitle = findViewById(R.id.etAssignmentTitle);
        Spinner spinnerAssignmentClass = findViewById(R.id.spinnerAssignmentClass);
        EditText etAssignmentSubject = findViewById(R.id.etAssignmentSubject);
        btnSelectDueDate = findViewById(R.id.btnSelectDueDate);
        EditText etAssignmentDesc = findViewById(R.id.etAssignmentDesc);

        MaterialButton btnPostAssignment = findViewById(R.id.btnPostAssignment);
        MaterialButton btnClearAssignments = findViewById(R.id.btnClearAssignments);
        containerFacultyAssignments = findViewById(R.id.containerFacultyAssignments);

        String name = getIntent().getStringExtra("TEACHER_NAME");
        String classes = getIntent().getStringExtra("ASSIGNED_CLASSES");
        if (name != null && !name.isEmpty()) teacherName = name;
        if (classes != null && !classes.isEmpty()) teacherAssignedClasses = classes;

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Setup Class Dropdown
        String[] assignedClassList = teacherAssignedClasses.split("\\s*,\\s*");
        if (assignedClassList.length == 0 || assignedClassList[0].trim().isEmpty()) {
            assignedClassList = new String[]{"TY BCA", "SY BCA", "FY BCA"};
        }
        if (spinnerAssignmentClass != null) {
            spinnerAssignmentClass.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, assignedClassList));
        }

        // Default Due Date (+ 7 Days)
        dueCalendar.add(Calendar.DAY_OF_MONTH, 7);
        updateDueDateButton();

        if (btnSelectDueDate != null) {
            btnSelectDueDate.setOnClickListener(v -> showDueDatePicker());
        }

        if (btnPostAssignment != null) {
            btnPostAssignment.setOnClickListener(v -> {
                String title = etAssignmentTitle.getText().toString().trim();
                String targetClass = spinnerAssignmentClass != null ? spinnerAssignmentClass.getSelectedItem().toString() : "TY BCA";
                String subject = etAssignmentSubject.getText().toString().trim();
                String desc = etAssignmentDesc.getText().toString().trim();

                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                String dueDateStr = sdf.format(dueCalendar.getTime());

                if (!title.isEmpty() && !subject.isEmpty()) {
                    CloudRepository.postAssignment(title, desc.isEmpty() ? "Complete all assigned questions." : desc, subject, targetClass, dueDateStr, teacherName);
                    Toast.makeText(this, "Assignment posted for " + targetClass + " (Due: " + dueDateStr + ")!", Toast.LENGTH_LONG).show();

                    etAssignmentTitle.setText("");
                    etAssignmentSubject.setText("");
                    etAssignmentDesc.setText("");
                } else {
                    Toast.makeText(this, "Please enter Assignment Title and Subject Name!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnClearAssignments != null) {
            btnClearAssignments.setOnClickListener(v -> {
                CloudRepository.clearAllAssignments();
                Toast.makeText(this, "All Posted Assignments Cleared!", Toast.LENGTH_SHORT).show();
            });
        }

        loadPostedAssignments();
    }

    private void updateDueDateButton() {
        if (btnSelectDueDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            String formattedDate = sdf.format(dueCalendar.getTime());
            btnSelectDueDate.setText("📅 Select Submission Due Date: " + formattedDate);
        }
    }

    private void showDueDatePicker() {
        int year = dueCalendar.get(Calendar.YEAR);
        int month = dueCalendar.get(Calendar.MONTH);
        int day = dueCalendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
            dueCalendar.set(Calendar.YEAR, selectedYear);
            dueCalendar.set(Calendar.MONTH, selectedMonth);
            dueCalendar.set(Calendar.DAY_OF_MONTH, selectedDay);
            updateDueDateButton();
        }, year, month, day);

        dialog.show();
    }

    private void loadPostedAssignments() {
        CloudRepository.assignmentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (containerFacultyAssignments == null) return;
                containerFacultyAssignments.removeAllViews();

                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    TextView tvEmpty = new TextView(FacultyAssignmentActivity.this);
                    tvEmpty.setText("No active posted assignments found.");
                    tvEmpty.setTextColor(0xFF64748B);
                    tvEmpty.setPadding(10, 20, 10, 20);
                    containerFacultyAssignments.addView(tvEmpty);
                } else {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String title = ds.child("title").getValue(String.class);
                        String desc = ds.child("description").getValue(String.class);
                        String subject = ds.child("subject").getValue(String.class);
                        String cName = ds.child("className").getValue(String.class);
                        String dueDate = ds.child("dueDate").getValue(String.class);
                        String postedBy = ds.child("postedBy").getValue(String.class);

                        MaterialCardView card = new MaterialCardView(FacultyAssignmentActivity.this);
                        card.setRadius(12);
                        card.setCardElevation(2);
                        card.setCardBackgroundColor(0xFFFFFFFF);
                        card.setStrokeColor(0xFFE2E8F0);
                        card.setStrokeWidth(1);

                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        lp.setMargins(0, 0, 0, 16);
                        card.setLayoutParams(lp);

                        LinearLayout inner = new LinearLayout(FacultyAssignmentActivity.this);
                        inner.setOrientation(LinearLayout.VERTICAL);
                        inner.setPadding(20, 16, 20, 16);

                        TextView tvTitle = new TextView(FacultyAssignmentActivity.this);
                        tvTitle.setText("📚 " + (title != null ? title : "Assignment") + " (" + (cName != null ? cName : "TY BCA") + ")");
                        tvTitle.setTextSize(15);
                        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
                        tvTitle.setTextColor(0xFF1E293B);

                        TextView tvSubject = new TextView(FacultyAssignmentActivity.this);
                        tvSubject.setText("Subject: " + (subject != null ? subject : "Computer Science") + " | Posted By: " + (postedBy != null ? postedBy : "Faculty"));
                        tvSubject.setTextSize(13);
                        tvSubject.setTextColor(0xFF2563EB);
                        tvSubject.setPadding(0, 4, 0, 0);

                        TextView tvDue = new TextView(FacultyAssignmentActivity.this);
                        tvDue.setText("⏳ Final Submission / Checking Due Date: " + (dueDate != null ? dueDate : "N/A"));
                        tvDue.setTextSize(13);
                        tvDue.setTypeface(null, android.graphics.Typeface.BOLD);
                        tvDue.setTextColor(0xFFD97706);
                        tvDue.setPadding(0, 4, 0, 0);

                        TextView tvDesc = new TextView(FacultyAssignmentActivity.this);
                        tvDesc.setText(desc);
                        tvDesc.setTextSize(13);
                        tvDesc.setTextColor(0xFF475569);
                        tvDesc.setPadding(0, 8, 0, 0);

                        inner.addView(tvTitle);
                        inner.addView(tvSubject);
                        inner.addView(tvDue);
                        inner.addView(tvDesc);
                        card.addView(inner);

                        containerFacultyAssignments.addView(card, 0); // Insert newest at top
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}