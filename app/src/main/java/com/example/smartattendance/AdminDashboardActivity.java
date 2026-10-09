package com.example.smartattendance;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AdminDashboardActivity extends AppCompatActivity {

    private final List<TeacherModel> facultyList = new ArrayList<>();
    private final List<StudentModel> studentList = new ArrayList<>();

    private String adminName = "Siddharth Bhambare";
    private String adminId = "10842931";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Bind Views
        TextView tvAdminProfileInfo = findViewById(R.id.tvAdminProfileInfo);
        MaterialCardView btnAdminAvatar = findViewById(R.id.btnAdminAvatar);
        TextView tvAdminAvatarInitials = findViewById(R.id.tvAdminAvatarInitials);

        MaterialButton btnAdminManageTeachers = findViewById(R.id.btnAdminManageTeachers);
        MaterialButton btnAdminManageStudents = findViewById(R.id.btnAdminManageStudents);
        MaterialButton btnExportAttendanceReport = findViewById(R.id.btnExportAttendanceReport);
        MaterialButton btnPostBroadcastNotice = findViewById(R.id.btnPostBroadcastNotice);
        MaterialButton btnManageLeaves = findViewById(R.id.btnManageLeaves);
        MaterialButton btnAddAdmin = findViewById(R.id.btnAddAdmin);

        String n = getIntent().getStringExtra("ADMIN_NAME");
        String id = getIntent().getStringExtra("ADMIN_ID");
        if (n != null && !n.trim().isEmpty()) adminName = n;
        if (id != null && !id.trim().isEmpty()) adminId = id;

        if (tvAdminProfileInfo != null) {
            tvAdminProfileInfo.setText(adminName + " (#" + adminId + ")");
        }

        if (tvAdminAvatarInitials != null && !adminName.isEmpty()) {
            String[] parts = adminName.trim().split("\\s+");
            if (parts.length >= 2) {
                tvAdminAvatarInitials.setText(("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase());
            } else if (!parts[0].isEmpty()) {
                tvAdminAvatarInitials.setText(("" + parts[0].charAt(0)).toUpperCase());
            }
        }

        if (btnAdminAvatar != null) {
            btnAdminAvatar.setOnClickListener(v -> showProfileDialog(adminName, adminId));
        }

        // Live Listeners for Cloud DB
        loadTeachersFromFirebase();
        loadStudentsFromFirebase();

        // 1. Manage Teachers
        if (btnAdminManageTeachers != null) {
            btnAdminManageTeachers.setOnClickListener(v -> showTeacherManagementDialog());
        }

        // 2. Manage Students
        if (btnAdminManageStudents != null) {
            btnAdminManageStudents.setOnClickListener(v -> showStudentManagementDialog());
        }

        // 3. Export Attendance Report
        if (btnExportAttendanceReport != null) {
            btnExportAttendanceReport.setOnClickListener(v -> showExportReportDialog());
        }

        // 4. Post Broadcast Notice
        if (btnPostBroadcastNotice != null) {
            btnPostBroadcastNotice.setOnClickListener(v -> showBroadcastNoticeDialog());
        }

        // 5. Manage Leave Applications
        if (btnManageLeaves != null) {
            btnManageLeaves.setOnClickListener(v -> showLeaveApprovalCenterDialog());
        }

        // 6. Add New Admin
        if (btnAddAdmin != null) {
            btnAddAdmin.setOnClickListener(v -> showAddNewAdminDialog());
        }
    }

    private void showExportReportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📄 Export Attendance Sheet & Report");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 20);

        final Spinner spinnerClass = new Spinner(this);
        String[] classesList = {"TY BCA", "SY BCA", "FY BCA", "TY B.Sc (CS)", "SY B.Sc (CS)", "FY B.Sc (CS)", "12th (HSC)"};
        ArrayAdapter<String> classAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, classesList);
        spinnerClass.setAdapter(classAdapter);
        layout.addView(spinnerClass);

        final Spinner spinnerSubject = new Spinner(this);
        String[] subList = {"Core Java", "Software Engineering", "Cloud Computing", "UI & UX Design", "Project"};
        ArrayAdapter<String> subAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, subList);
        spinnerSubject.setAdapter(subAdapter);
        layout.addView(spinnerSubject);

        builder.setView(layout);

        builder.setPositiveButton("Generate & Export CSV", (dialog, which) -> {
            String selectedClass = spinnerClass.getSelectedItem().toString();
            String selectedSub = spinnerSubject.getSelectedItem().toString();

            AttendanceReportUtil.exportAndShareReport(this, selectedClass, selectedSub, studentList);
            Toast.makeText(this, "Exporting Attendance Report for " + selectedClass + "...", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showBroadcastNoticeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📢 Campus Broadcast Announcement");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 20);

        final EditText etTitle = new EditText(this);
        etTitle.setHint("Notice Title (e.g. Holiday Announcement)");
        layout.addView(etTitle);

        final EditText etMessage = new EditText(this);
        etMessage.setHint("Notice Message / Details");
        layout.addView(etMessage);

        builder.setView(layout);

        builder.setPositiveButton("Post Broadcast", (dialog, which) -> {
            String title = etTitle.getText().toString().trim();
            String msg = etMessage.getText().toString().trim();

            if (!title.isEmpty() && !msg.isEmpty()) {
                CloudRepository.postAnnouncement(title, msg, "Notice", adminName);
                Toast.makeText(this, "Campus notice broadcasted successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Please enter both Title and Message", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNeutralButton("🗑️ Clear All Notices", (dialog, which) -> {
            CloudRepository.clearAllAnnouncements();
            Toast.makeText(this, "Campus Notice History Cleared!", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showLeaveApprovalCenterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📝 Leave Application Approval Center");

        ScrollView scrollView = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(40, 20, 40, 20);

        CloudRepository.leaveRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    TextView tvEmpty = new TextView(AdminDashboardActivity.this);
                    tvEmpty.setText("No leave applications found.");
                    tvEmpty.setPadding(10, 20, 10, 20);
                    tvEmpty.setTextColor(0xFF64748B);
                    container.addView(tvEmpty);
                } else {
                    TextView tvPendingHeader = new TextView(AdminDashboardActivity.this);
                    tvPendingHeader.setText("📌 PENDING APPLICATIONS:");
                    tvPendingHeader.setTypeface(null, android.graphics.Typeface.BOLD);
                    tvPendingHeader.setTextColor(0xFFD97706);
                    tvPendingHeader.setPadding(0, 10, 0, 10);
                    container.addView(tvPendingHeader);

                    int pendingCount = 0;
                    List<DataSnapshot> historyList = new ArrayList<>();

                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String status = ds.child("status").getValue(String.class);

                        if ("PENDING".equalsIgnoreCase(status)) {
                            pendingCount++;
                            String leaveId = ds.getKey();
                            String name = ds.child("applicantName").getValue(String.class);
                            String roll = ds.child("applicantRollOrId").getValue(String.class);
                            String reason = ds.child("reason").getValue(String.class);

                            LinearLayout row = new LinearLayout(AdminDashboardActivity.this);
                            row.setOrientation(LinearLayout.VERTICAL);
                            row.setPadding(0, 12, 0, 16);

                            TextView tvInfo = new TextView(AdminDashboardActivity.this);
                            tvInfo.setText("Applicant: " + name + " (#" + roll + ")\nReason: " + reason + "\nStatus: PENDING 🟡");
                            tvInfo.setTextSize(14);
                            tvInfo.setTextColor(0xFF1E293B);

                            LinearLayout btnRow = new LinearLayout(AdminDashboardActivity.this);
                            btnRow.setOrientation(LinearLayout.HORIZONTAL);

                            TextView btnApprove = new TextView(AdminDashboardActivity.this);
                            btnApprove.setText("✓ Approve");
                            btnApprove.setTextColor(0xFF16A34A);
                            btnApprove.setPadding(0, 8, 30, 8);
                            btnApprove.setTypeface(null, android.graphics.Typeface.BOLD);
                            btnApprove.setOnClickListener(v -> {
                                if (leaveId != null) CloudRepository.updateLeaveStatus(leaveId, "APPROVED");
                                Toast.makeText(AdminDashboardActivity.this, "Leave Approved & Saved to History!", Toast.LENGTH_SHORT).show();
                                showLeaveApprovalCenterDialog();
                            });

                            TextView btnReject = new TextView(AdminDashboardActivity.this);
                            btnReject.setText("✕ Reject");
                            btnReject.setTextColor(0xFFDC2626);
                            btnReject.setPadding(0, 8, 0, 8);
                            btnReject.setTypeface(null, android.graphics.Typeface.BOLD);
                            btnReject.setOnClickListener(v -> {
                                if (leaveId != null) CloudRepository.updateLeaveStatus(leaveId, "REJECTED");
                                Toast.makeText(AdminDashboardActivity.this, "Leave Rejected & Saved to History!", Toast.LENGTH_SHORT).show();
                                showLeaveApprovalCenterDialog();
                            });

                            btnRow.addView(btnApprove);
                            btnRow.addView(btnReject);

                            row.addView(tvInfo);
                            row.addView(btnRow);
                            container.addView(row);
                        } else {
                            historyList.add(ds);
                        }
                    }

                    if (pendingCount == 0) {
                        TextView tvNoPending = new TextView(AdminDashboardActivity.this);
                        tvNoPending.setText("No pending leave applications.\n");
                        tvNoPending.setTextColor(0xFF64748B);
                        container.addView(tvNoPending);
                    }

                    // Leave History Log
                    TextView tvHistoryHeader = new TextView(AdminDashboardActivity.this);
                    tvHistoryHeader.setText("📜 PROCESSED LEAVE HISTORY LOG:");
                    tvHistoryHeader.setTypeface(null, android.graphics.Typeface.BOLD);
                    tvHistoryHeader.setTextColor(0xFF2563EB);
                    tvHistoryHeader.setPadding(0, 20, 0, 10);
                    container.addView(tvHistoryHeader);

                    if (historyList.isEmpty()) {
                        TextView tvNoHistory = new TextView(AdminDashboardActivity.this);
                        tvNoHistory.setText("No processed leave history.");
                        tvNoHistory.setTextColor(0xFF64748B);
                        container.addView(tvNoHistory);
                    } else {
                        for (DataSnapshot ds : historyList) {
                            String name = ds.child("applicantName").getValue(String.class);
                            String roll = ds.child("applicantRollOrId").getValue(String.class);
                            String reason = ds.child("reason").getValue(String.class);
                            String status = ds.child("status").getValue(String.class);

                            String badge = "APPROVED 🟢";
                            if ("REJECTED".equalsIgnoreCase(status)) badge = "REJECTED 🔴";

                            TextView tvHistInfo = new TextView(AdminDashboardActivity.this);
                            tvHistInfo.setText("• " + name + " (#" + roll + ") - " + reason + "\n  Decision: " + badge);
                            tvHistInfo.setTextSize(13);
                            tvHistInfo.setPadding(0, 6, 0, 12);
                            tvHistInfo.setTextColor(0xFF334155);
                            container.addView(tvHistInfo);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        scrollView.addView(container);
        builder.setView(scrollView);
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void loadTeachersFromFirebase() {
        CloudRepository.teacherRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                facultyList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String id = ds.child("id").getValue(String.class);
                    String name = ds.child("name").getValue(String.class);
                    if (id != null && name != null) {
                        facultyList.add(new TeacherModel(id, name));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadStudentsFromFirebase() {
        CloudRepository.studentRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                studentList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String roll = ds.child("roll").getValue(String.class);
                    String name = ds.child("name").getValue(String.class);
                    String sId = ds.child("studentId").getValue(String.class);
                    String cName = ds.child("className").getValue(String.class);
                    if (roll != null && name != null) {
                        studentList.add(new StudentModel(roll, name, sId, cName));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // --- Student Management Dialog (With Real-Time Search) ---
    private void showStudentManagementDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Student Management (" + studentList.size() + " Registered)");

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(30, 20, 30, 10);

        final EditText etSearchStudent = new EditText(this);
        etSearchStudent.setHint("🔍 Search Name, Class, Roll # or ID...");
        etSearchStudent.setPadding(20, 16, 20, 16);
        mainLayout.addView(etSearchStudent);

        ScrollView scrollView = new ScrollView(this);
        final LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(10, 10, 10, 10);

        populateStudentRows(container, studentList, "");

        etSearchStudent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                populateStudentRows(container, studentList, s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        scrollView.addView(container);
        mainLayout.addView(scrollView);
        builder.setView(mainLayout);

        builder.setPositiveButton("+ Add Student", (dialog, which) -> showAddNewStudentDialog());
        builder.setNegativeButton("Close", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void populateStudentRows(LinearLayout container, List<StudentModel> list, String query) {
        container.removeAllViews();

        List<StudentModel> filtered = new ArrayList<>();
        for (StudentModel s : list) {
            if (query.isEmpty()
                    || s.getName().toLowerCase().contains(query.toLowerCase())
                    || s.getClassName().toLowerCase().contains(query.toLowerCase())
                    || s.getRoll().equalsIgnoreCase(query)
                    || s.getStudentId().equalsIgnoreCase(query)) {
                filtered.add(s);
            }
        }

        if (filtered.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No matching students found.");
            tvEmpty.setPadding(10, 20, 10, 20);
            tvEmpty.setTextColor(0xFF64748B);
            container.addView(tvEmpty);
        } else {
            for (StudentModel student : filtered) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 12, 0, 12);

                TextView tvInfo = new TextView(this);
                tvInfo.setText(student.getName() + " | " + student.getClassName() + "\nRoll #" + student.getRoll() + " | ID: #" + student.getStudentId());
                tvInfo.setTextSize(14);
                tvInfo.setTextColor(0xFF1E293B);
                tvInfo.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

                TextView btnDel = createDeleteButton();
                btnDel.setOnClickListener(v -> showDeleteStudentConfirmation(student));

                row.addView(tvInfo);
                row.addView(btnDel);
                container.addView(row);
            }
        }
    }

    private void showDeleteStudentConfirmation(StudentModel student) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Student?")
                .setMessage("Are you sure you want to remove " + student.getName() + " (Roll #" + student.getRoll() + ") permanently?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    CloudRepository.removeStudentAndReorder(student.getRoll(), student.getClassName());
                    Toast.makeText(AdminDashboardActivity.this, student.getName() + " removed permanently!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private TextView createDeleteButton() {
        TextView btnDel = new TextView(this);
        btnDel.setText(R.string.remove_action);
        btnDel.setTextColor(0xFFDC2626);
        btnDel.setTextSize(13);
        btnDel.setPadding(16, 8, 16, 8);
        return btnDel;
    }

    private void showAddNewStudentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Register New Student");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 20);

        // 1. Enter Student Name
        final EditText etName = new EditText(this);
        etName.setHint("Enter Student Full Name");
        layout.addView(etName);

        // 2. Set Password
        final EditText etPass = new EditText(this);
        etPass.setHint("Set Login Password");
        layout.addView(etPass);

        builder.setView(layout);

        builder.setPositiveButton("Add & Auto-Sort", (dialog, which) -> {
            String rawName = etName.getText().toString().trim();
            String pass = etPass.getText().toString().trim();

            if (!rawName.isEmpty() && !pass.isEmpty()) {
                // Cloud write + auto-capitalization + auto-sort + auto-roll assignment
                CloudRepository.addStudentAndReorder(rawName, pass);

                String formatted = CloudRepository.formatToTitleCase(rawName);
                Toast.makeText(AdminDashboardActivity.this, formatted + " added & Roll numbers recalculated!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(AdminDashboardActivity.this, "Please enter both Student Name and Password", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    // --- Teacher Management Dialog ---
    private void showTeacherManagementDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Faculty Directory (" + facultyList.size() + " Teachers)");

        ScrollView scrollView = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(40, 20, 40, 20);

        if (facultyList.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText(R.string.no_faculty_found);
            tvEmpty.setPadding(10, 20, 10, 20);
            tvEmpty.setTextColor(0xFF64748B);
            container.addView(tvEmpty);
        } else {
            for (TeacherModel teacher : facultyList) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 16, 0, 16);

                TextView tvInfo = new TextView(this);
                tvInfo.setText(getString(R.string.teacher_item_format, teacher.getName(), teacher.getId()));
                tvInfo.setTextSize(14);
                tvInfo.setTextColor(0xFF1E293B);
                tvInfo.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

                TextView btnDel = createDeleteButton();
                btnDel.setOnClickListener(v -> showDeleteTeacherConfirmation(teacher));

                row.addView(tvInfo);
                row.addView(btnDel);
                container.addView(row);
            }
        }

        scrollView.addView(container);
        builder.setView(scrollView);
        builder.setPositiveButton("+ Add New Teacher", (dialog, which) -> showAddNewTeacherDialog());
        builder.setNegativeButton("Close", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showDeleteTeacherConfirmation(TeacherModel teacher) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Teacher?")
                .setMessage("Are you sure you want to remove " + teacher.getName() + " (ID #" + teacher.getId() + ") permanently?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    CloudRepository.removeTeacher(teacher.getId());
                    Toast.makeText(AdminDashboardActivity.this, teacher.getName() + " removed!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showAddNewTeacherDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Register New Faculty");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 20);

        final EditText etName = new EditText(this);
        etName.setHint("Faculty Full Name (e.g. Prof. Patil)");
        layout.addView(etName);

        final EditText etPass = new EditText(this);
        etPass.setHint("Set Login Password");
        layout.addView(etPass);

        builder.setView(layout);

        builder.setPositiveButton("Generate ID & Save", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String pass = etPass.getText().toString().trim();

            if (!name.isEmpty() && !pass.isEmpty()) {
                Random random = new Random();
                int eightDigitId = 10000000 + random.nextInt(90000000);
                String idStr = "" + eightDigitId;

                // Save to Firebase Cloud
                CloudRepository.saveTeacher(idStr, name, pass);

                new AlertDialog.Builder(AdminDashboardActivity.this)
                        .setTitle("Faculty Registered! 👨‍🏫")
                        .setMessage("Faculty Name: " + name + "\nAssigned 8-Digit Unique ID: #" + idStr + "\nSet Password: " + pass + "\n\n(Saved to Cloud permanently)")
                        .setPositiveButton("OK", null)
                        .show();
            } else {
                Toast.makeText(AdminDashboardActivity.this, "Please enter both Name and Password", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showAddNewAdminDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New Administrator");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 20);

        final EditText etName = new EditText(this);
        etName.setHint("Admin Full Name");
        layout.addView(etName);

        final EditText etPass = new EditText(this);
        etPass.setHint("Set Login Password");
        layout.addView(etPass);

        builder.setView(layout);

        builder.setPositiveButton("Generate ID & Authorize", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String pass = etPass.getText().toString().trim();

            if (!name.isEmpty() && !pass.isEmpty()) {
                Random random = new Random();
                int eightDigitId = 10000000 + random.nextInt(90000000);
                String idStr = "" + eightDigitId;

                // Save to Firebase Cloud
                CloudRepository.saveAdmin(idStr, name, pass);

                new AlertDialog.Builder(AdminDashboardActivity.this)
                        .setTitle("Admin Created Successfully! 🛡️")
                        .setMessage("Admin Name: " + name + "\nAssigned 8-Digit Unique ID: #" + idStr + "\nSet Password: " + pass + "\n\n(Saved to Cloud permanently)")
                        .setPositiveButton("OK", null)
                        .show();
            } else {
                Toast.makeText(AdminDashboardActivity.this, "Please enter both Name and Password", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showProfileDialog(String name, String id) {
        new AlertDialog.Builder(this)
                .setTitle("👤 User Profile")
                .setMessage("Role: Administrator\nName: " + name + "\nUnique ID: #" + id + "\nAccount Status: Active & Cloud-Synced\nAccess Level: Full Administrative Access")
                .setPositiveButton("Close", null)
                .show();
    }
}