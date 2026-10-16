package com.example.smartattendance;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AdminDashboardActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private final List<TeacherModel> facultyList = new ArrayList<>();
    private final List<StudentModel> studentList = new ArrayList<>();

    private String adminName = "Siddharth Bhambare";
    private String adminId = "10842931";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        SessionManager sessionManager = new SessionManager(this);

        // Bind Views & Telegram Drawer
        drawerLayout = findViewById(R.id.drawerLayout);
        LinearLayout btnMenuDrawer = findViewById(R.id.btnMenuDrawer);

        TextView tvAdminProfileInfo = findViewById(R.id.tvAdminProfileInfo);
        MaterialCardView btnAdminAvatar = findViewById(R.id.btnAdminAvatar);
        TextView tvAdminAvatarInitials = findViewById(R.id.tvAdminAvatarInitials);

        TextView tvDrawerAdminInitials = findViewById(R.id.tvDrawerAdminInitials);
        TextView tvDrawerAdminName = findViewById(R.id.tvDrawerAdminName);
        TextView tvDrawerAdminDetails = findViewById(R.id.tvDrawerAdminDetails);

        TextView tvDrawerAdminProfile = findViewById(R.id.tvDrawerAdminProfile);
        TextView tvDrawerChangePassword = findViewById(R.id.tvDrawerChangePassword);
        TextView tvDrawerAdminNotice = findViewById(R.id.tvDrawerAdminNotice);
        TextView tvDrawerAdminSettings = findViewById(R.id.tvDrawerAdminSettings);
        MaterialButton btnDrawerLogout = findViewById(R.id.btnDrawerLogout);
        TextView btnThemeToggle = findViewById(R.id.btnThemeToggle);

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

        final String finalAdminId = adminId;

        if (tvAdminProfileInfo != null) {
            tvAdminProfileInfo.setText(adminName + " (#" + adminId + ")");
        }
        if (tvDrawerAdminName != null) {
            tvDrawerAdminName.setText(adminName);
        }
        if (tvDrawerAdminDetails != null) {
            tvDrawerAdminDetails.setText("Administrator | ID: #" + adminId);
        }

        if (!adminName.isEmpty()) {
            String initials = "👤";
            String[] parts = adminName.trim().split("\\s+");
            if (parts.length >= 2) {
                initials = ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
            } else if (!parts[0].isEmpty()) {
                initials = ("" + parts[0].charAt(0)).toUpperCase();
            }
            if (tvAdminAvatarInitials != null) tvAdminAvatarInitials.setText(initials);
            if (tvDrawerAdminInitials != null) tvDrawerAdminInitials.setText(initials);
        }

        // Theme Toggle Handler
        if (btnThemeToggle != null) {
            btnThemeToggle.setText(sessionManager.getThemeMode() == AppCompatDelegate.MODE_NIGHT_YES ? "🌙" : "☀️");
            btnThemeToggle.setOnClickListener(v -> {
                int newMode = sessionManager.getThemeMode() == AppCompatDelegate.MODE_NIGHT_YES
                        ? AppCompatDelegate.MODE_NIGHT_NO
                        : AppCompatDelegate.MODE_NIGHT_YES;
                sessionManager.setThemeMode(newMode);
                AppCompatDelegate.setDefaultNightMode(newMode);
            });
        }

        // Open Telegram Navigation Drawer
        if (btnMenuDrawer != null) {
            btnMenuDrawer.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.openDrawer(GravityCompat.START);
            });
        }

        if (btnAdminAvatar != null) {
            btnAdminAvatar.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.openDrawer(GravityCompat.START);
            });
        }

        // Telegram Drawer Actions
        if (tvDrawerAdminProfile != null) {
            tvDrawerAdminProfile.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
                showProfileDialog(adminName, adminId);
            });
        }

        if (tvDrawerChangePassword != null) {
            tvDrawerChangePassword.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
                Intent intent = new Intent(this, SettingsActivity.class);
                intent.putExtra("USER_ROLE", "ADMIN");
                intent.putExtra("USER_KEY", finalAdminId);
                startActivity(intent);
            });
        }

        if (tvDrawerAdminNotice != null) {
            tvDrawerAdminNotice.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
                showBroadcastNoticeDialog();
            });
        }

        if (tvDrawerAdminSettings != null) {
            tvDrawerAdminSettings.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
                Intent intent = new Intent(this, SettingsActivity.class);
                intent.putExtra("USER_ROLE", "ADMIN");
                intent.putExtra("USER_KEY", finalAdminId);
                startActivity(intent);
            });
        }

        if (btnDrawerLogout != null) {
            btnDrawerLogout.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
                confirmLogout();
            });
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

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.logout_confirm_title)
                .setMessage(R.string.logout_confirm_msg)
                .setPositiveButton("Logout", (dialog, which) -> {
                    new SessionManager(this).logoutUser();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
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
                        String status = CloudRepository.getSafeString(ds, "status");

                        if ("PENDING".equalsIgnoreCase(status)) {
                            pendingCount++;
                            String leaveId = ds.getKey();
                            String name = CloudRepository.getSafeString(ds, "applicantName");
                            String roll = CloudRepository.getSafeString(ds, "applicantRollOrId");
                            String reason = CloudRepository.getSafeString(ds, "reason");

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
                                if (leaveId != null) CloudRepository.updateLeaveStatus(leaveId, "APPROVED", name, reason);
                                Toast.makeText(AdminDashboardActivity.this, "Leave Approved & Saved to History!", Toast.LENGTH_SHORT).show();
                                showLeaveApprovalCenterDialog();
                            });

                            TextView btnReject = new TextView(AdminDashboardActivity.this);
                            btnReject.setText("✕ Reject");
                            btnReject.setTextColor(0xFFDC2626);
                            btnReject.setPadding(0, 8, 0, 8);
                            btnReject.setTypeface(null, android.graphics.Typeface.BOLD);
                            btnReject.setOnClickListener(v -> {
                                if (leaveId != null) CloudRepository.updateLeaveStatus(leaveId, "REJECTED", name, reason);
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
                            String name = CloudRepository.getSafeString(ds, "applicantName");
                            String roll = CloudRepository.getSafeString(ds, "applicantRollOrId");
                            String reason = CloudRepository.getSafeString(ds, "reason");
                            String status = CloudRepository.getSafeString(ds, "status");

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
                    String id = CloudRepository.getSafeString(ds, "id");
                    if (id.isEmpty()) id = ds.getKey();
                    String name = CloudRepository.getSafeString(ds, "name");
                    String classes = CloudRepository.getSafeString(ds, "assignedClasses");
                    String subjects = CloudRepository.getSafeString(ds, "assignedSubjects");
                    String email = CloudRepository.getSafeString(ds, "email");
                    String phone = CloudRepository.getSafeString(ds, "phone");
                    if (id != null && !id.isEmpty() && name != null && !name.isEmpty()) {
                        facultyList.add(new TeacherModel(id, name, classes, subjects, email, phone));
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
                    String roll = CloudRepository.getSafeString(ds, "roll");
                    String name = CloudRepository.getSafeString(ds, "name");
                    String sId = CloudRepository.getSafeString(ds, "studentId");
                    String cName = CloudRepository.getSafeString(ds, "className");
                    String email = CloudRepository.getSafeString(ds, "email");
                    String phone = CloudRepository.getSafeString(ds, "phone");
                    if (!roll.isEmpty() && !name.isEmpty()) {
                        studentList.add(new StudentModel(roll, name, sId, cName, email, phone));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // --- Student Management Dialog (With Class Filter, Search & Edit Student) ---
    private void showStudentManagementDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Student Management (" + studentList.size() + " Registered)");

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(30, 20, 30, 10);

        // 1. Class Filter Dropdown
        final Spinner spinnerClassFilter = new Spinner(this);
        String[] classFilterOptions = {"All Classes", "TY BCA", "SY BCA", "FY BCA", "TY B.Sc (CS)", "SY B.Sc (CS)", "FY B.Sc (CS)", "TY BBA", "SY BBA", "FY BBA", "12th (HSC)"};
        spinnerClassFilter.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, classFilterOptions));
        mainLayout.addView(spinnerClassFilter);

        // 2. Real-Time Search Bar
        final EditText etSearchStudent = new EditText(this);
        etSearchStudent.setHint("🔍 Search Name, Roll #, ID, Phone...");
        etSearchStudent.setPadding(20, 16, 20, 16);
        mainLayout.addView(etSearchStudent);

        ScrollView scrollView = new ScrollView(this);
        final LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(10, 10, 10, 10);

        populateStudentRows(container, studentList, "", "All Classes");

        spinnerClassFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedClass = classFilterOptions[position];
                String q = etSearchStudent.getText().toString().trim();
                populateStudentRows(container, studentList, q, selectedClass);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        etSearchStudent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String selectedClass = classFilterOptions[spinnerClassFilter.getSelectedItemPosition()];
                populateStudentRows(container, studentList, s.toString().trim(), selectedClass);
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

    private void populateStudentRows(LinearLayout container, List<StudentModel> list, String query, String classFilter) {
        container.removeAllViews();

        List<StudentModel> filtered = new ArrayList<>();
        for (StudentModel s : list) {
            boolean classMatches = "All Classes".equalsIgnoreCase(classFilter) || s.getClassName().equalsIgnoreCase(classFilter);
            boolean queryMatches = query.isEmpty()
                    || s.getName().toLowerCase().contains(query.toLowerCase())
                    || s.getClassName().toLowerCase().contains(query.toLowerCase())
                    || s.getRoll().equalsIgnoreCase(query)
                    || s.getStudentId().equalsIgnoreCase(query)
                    || s.getPhone().contains(query);

            if (classMatches && queryMatches) {
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

                TextView btnEdit = new TextView(this);
                btnEdit.setText("✏️ Edit");
                btnEdit.setTextColor(0xFF2563EB);
                btnEdit.setTextSize(13);
                btnEdit.setPadding(12, 8, 12, 8);
                btnEdit.setOnClickListener(v -> showEditStudentDialog(student));

                TextView btnDel = createDeleteButton();
                btnDel.setOnClickListener(v -> showDeleteStudentConfirmation(student));

                row.addView(tvInfo);
                row.addView(btnEdit);
                row.addView(btnDel);
                container.addView(row);
            }
        }
    }

    private void showEditStudentDialog(StudentModel student) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("✏️ Edit Student (" + student.getName() + ")");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 20);

        final EditText etName = new EditText(this);
        etName.setHint("Student Full Name");
        etName.setText(student.getName());
        layout.addView(etName);

        final EditText etClass = new EditText(this);
        etClass.setHint("Academic Class (e.g. TY BCA)");
        etClass.setText(student.getClassName());
        layout.addView(etClass);

        final EditText etEmail = new EditText(this);
        etEmail.setHint("Student Email Address");
        etEmail.setText(student.getEmail());
        layout.addView(etEmail);

        final EditText etPhone = new EditText(this);
        etPhone.setHint("Student Phone Number");
        etPhone.setText(student.getPhone());
        layout.addView(etPhone);

        final EditText etPass = new EditText(this);
        etPass.setHint("Update Password");
        layout.addView(etPass);

        builder.setView(layout);

        builder.setPositiveButton("Save Changes", (dialog, which) -> {
            String newName = etName.getText().toString().trim();
            String newClass = etClass.getText().toString().trim();
            String newEmail = etEmail.getText().toString().trim();
            String newPhone = etPhone.getText().toString().trim();
            String newPass = etPass.getText().toString().trim();

            if (!newName.isEmpty()) {
                CloudRepository.addStudentAndReorder(newName, newPass, newClass, newEmail, newPhone);
                Toast.makeText(this, "Student details updated for " + newName + "!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
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

        // 2. Enter Email
        final EditText etEmail = new EditText(this);
        etEmail.setHint("Enter Student Email Address");
        etEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        layout.addView(etEmail);

        // 3. Enter Phone
        final EditText etPhone = new EditText(this);
        etPhone.setHint("Enter Student Phone Number");
        etPhone.setInputType(InputType.TYPE_CLASS_PHONE);
        layout.addView(etPhone);

        // 4. Set Password
        final EditText etPass = new EditText(this);
        etPass.setHint("Set Login Password");
        layout.addView(etPass);

        builder.setView(layout);

        builder.setPositiveButton("Add & Auto-Sort", (dialog, which) -> {
            String rawName = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String pass = etPass.getText().toString().trim();

            if (!rawName.isEmpty() && !pass.isEmpty()) {
                // Cloud write + auto-capitalization + auto-sort + auto-roll assignment
                CloudRepository.addStudentAndReorder(rawName, pass, "TY BCA", email, phone);

                String formatted = CloudRepository.formatToTitleCase(rawName);
                Toast.makeText(AdminDashboardActivity.this, formatted + " added & Roll numbers recalculated!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(AdminDashboardActivity.this, "Please enter both Student Name and Password", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    // --- Teacher Management Dialog (With Real-Time Search, Edit & Delete) ---
    private void showTeacherManagementDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Faculty Directory (" + facultyList.size() + " Teachers)");

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(30, 20, 30, 10);

        final EditText etSearchTeacher = new EditText(this);
        etSearchTeacher.setHint("🔍 Search Name, ID, Classes, Subjects...");
        etSearchTeacher.setPadding(20, 16, 20, 16);
        mainLayout.addView(etSearchTeacher);

        ScrollView scrollView = new ScrollView(this);
        final LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(10, 10, 10, 10);

        populateTeacherRows(container, facultyList, "");

        etSearchTeacher.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                populateTeacherRows(container, facultyList, s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        scrollView.addView(container);
        mainLayout.addView(scrollView);
        builder.setView(mainLayout);

        builder.setPositiveButton("+ Add New Teacher", (dialog, which) -> showAddNewTeacherDialog());
        builder.setNegativeButton("Close", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void populateTeacherRows(LinearLayout container, List<TeacherModel> list, String query) {
        container.removeAllViews();

        List<TeacherModel> filtered = new ArrayList<>();
        for (TeacherModel t : list) {
            if (query.isEmpty()
                    || t.getName().toLowerCase().contains(query.toLowerCase())
                    || t.getId().equalsIgnoreCase(query)
                    || t.getAssignedClasses().toLowerCase().contains(query.toLowerCase())
                    || t.getAssignedSubjects().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(t);
            }
        }

        if (filtered.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No matching teachers found.");
            tvEmpty.setPadding(10, 20, 10, 20);
            tvEmpty.setTextColor(0xFF64748B);
            container.addView(tvEmpty);
        } else {
            for (TeacherModel teacher : filtered) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 16, 0, 16);

                TextView tvInfo = new TextView(this);
                tvInfo.setText(teacher.getName() + " (#" + teacher.getId() + ")\nAssigned: " + teacher.getAssignedClasses() + "\nSubjects: " + teacher.getAssignedSubjects());
                tvInfo.setTextSize(13);
                tvInfo.setTextColor(0xFF1E293B);
                tvInfo.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

                TextView btnEdit = new TextView(this);
                btnEdit.setText("✏️ Edit");
                btnEdit.setTextColor(0xFF2563EB);
                btnEdit.setTextSize(13);
                btnEdit.setPadding(12, 8, 12, 8);
                btnEdit.setOnClickListener(v -> showEditTeacherDialog(teacher));

                TextView btnDel = createDeleteButton();
                btnDel.setOnClickListener(v -> showDeleteTeacherConfirmation(teacher));

                row.addView(tvInfo);
                row.addView(btnEdit);
                row.addView(btnDel);
                container.addView(row);
            }
        }
    }

    private void showEditTeacherDialog(TeacherModel teacher) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("✏️ Edit Faculty Details (" + teacher.getName() + ")");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 20);

        final EditText etName = new EditText(this);
        etName.setHint("Faculty Full Name");
        etName.setText(teacher.getName());
        layout.addView(etName);

        final EditText etEmail = new EditText(this);
        etEmail.setHint("Faculty Email Address");
        etEmail.setText(teacher.getEmail());
        layout.addView(etEmail);

        final EditText etPhone = new EditText(this);
        etPhone.setHint("Faculty Phone Number");
        etPhone.setText(teacher.getPhone());
        layout.addView(etPhone);

        final EditText etClasses = new EditText(this);
        etClasses.setHint("Assigned Classes (e.g. TY BCA, SY BCA)");
        etClasses.setText(teacher.getAssignedClasses());
        layout.addView(etClasses);

        final EditText etSubjects = new EditText(this);
        etSubjects.setHint("Assigned Subjects (e.g. Core Java, Cloud Computing)");
        etSubjects.setText(teacher.getAssignedSubjects());
        layout.addView(etSubjects);

        final EditText etPass = new EditText(this);
        etPass.setHint("Update Password");
        layout.addView(etPass);

        builder.setView(layout);

        builder.setPositiveButton("Save Changes", (dialog, which) -> {
            String newName = etName.getText().toString().trim();
            String newEmail = etEmail.getText().toString().trim();
            String newPhone = etPhone.getText().toString().trim();
            String newClasses = etClasses.getText().toString().trim();
            String newSubjects = etSubjects.getText().toString().trim();
            String newPass = etPass.getText().toString().trim();

            if (!newName.isEmpty()) {
                CloudRepository.teacherRef.child(teacher.getId()).child("name").setValue(newName);
                CloudRepository.teacherRef.child(teacher.getId()).child("email").setValue(newEmail);
                CloudRepository.teacherRef.child(teacher.getId()).child("phone").setValue(newPhone);
                CloudRepository.teacherRef.child(teacher.getId()).child("assignedClasses").setValue(newClasses);
                CloudRepository.teacherRef.child(teacher.getId()).child("assignedSubjects").setValue(newSubjects);
                if (!newPass.isEmpty()) {
                    CloudRepository.teacherRef.child(teacher.getId()).child("password").setValue(newPass);
                }
                Toast.makeText(this, "Faculty details updated for " + newName + "!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
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

    // --- PHOTO-EXACT ACCORDION FACULTY REGISTRATION DIALOG MATCHING USER ATTACHED IMAGE ---
    private void showAddNewTeacherDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(40, 30, 40, 30);

        // Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText("Register New Faculty");
        tvTitle.setTextSize(24);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setTextColor(0xFF0F172A);
        tvTitle.setPadding(0, 0, 0, 24);
        mainLayout.addView(tvTitle);

        // Faculty Full Name Edit
        final EditText etName = new EditText(this);
        etName.setHint("Faculty Full Name (e.g. Prof. More)");
        etName.setTextSize(14);
        etName.setPadding(16, 16, 16, 16);
        mainLayout.addView(etName);

        // Faculty Email
        final EditText etEmail = new EditText(this);
        etEmail.setHint("Faculty Email Address (e.g. prof.more@smart.com)");
        etEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        etEmail.setTextSize(14);
        etEmail.setPadding(16, 16, 16, 16);
        mainLayout.addView(etEmail);

        // Faculty Phone
        final EditText etPhone = new EditText(this);
        etPhone.setHint("Faculty Phone Number (e.g. 9876543210)");
        etPhone.setInputType(InputType.TYPE_CLASS_PHONE);
        etPhone.setTextSize(14);
        etPhone.setPadding(16, 16, 16, 16);
        mainLayout.addView(etPhone);

        // Accordion Header: Assign Department / Classes -> "Select Class ∨"
        final TextView tvLabel = new TextView(this);
        tvLabel.setText("Assign Department / Classes:\nSelect Class ∨");
        tvLabel.setTextSize(14);
        tvLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        tvLabel.setTextColor(0xFF2563EB);
        tvLabel.setPadding(0, 20, 0, 10);
        tvLabel.setClickable(true);
        tvLabel.setFocusable(true);
        mainLayout.addView(tvLabel);

        // Container for Department Rows (COLLAPSED BY DEFAULT AS REQUESTED)
        final LinearLayout containerDeptRows = new LinearLayout(this);
        containerDeptRows.setOrientation(LinearLayout.VERTICAL);
        containerDeptRows.setVisibility(View.GONE); // Collapsed default!

        tvLabel.setOnClickListener(v -> {
            int currentVis = containerDeptRows.getVisibility();
            containerDeptRows.setVisibility(currentVis == View.VISIBLE ? View.GONE : View.VISIBLE);
        });

        // Departments Array: BCA, BSC, BSC cs, BBA, B.Com, More...
        String[][] departments = {
                {"BCA", "FY BCA", "SY BCA", "TY BCA"},
                {"BSC", "FY B.Sc", "SY B.Sc", "TY B.Sc"},
                {"BSC cs", "FY B.Sc (CS)", "SY B.Sc (CS)", "TY B.Sc (CS)"},
                {"BBA", "FY BBA", "SY BBA", "TY BBA"},
                {"B.Com", "FY B.Com", "SY B.Com", "TY B.Com"}
        };

        final List<String> selectedClassesList = new ArrayList<>();

        for (String[] dept : departments) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, 8, 0, 8);

            TextView tvDeptName = new TextView(this);
            tvDeptName.setText(dept[0]);
            tvDeptName.setTextSize(14);
            tvDeptName.setTypeface(null, android.graphics.Typeface.BOLD);
            tvDeptName.setTextColor(0xFF1E293B);
            tvDeptName.setLayoutParams(new LinearLayout.LayoutParams(160, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView tvArrow = new TextView(this);
            tvArrow.setText(" ➔ ");
            tvArrow.setTextSize(14);
            tvArrow.setTextColor(0xFF64748B);

            row.addView(tvDeptName);
            row.addView(tvArrow);

            // Create FY, SY, TY Chips
            String[] years = {"FY", "SY", "TY"};
            for (int i = 0; i < 3; i++) {
                final String yearTag = years[i];
                final String fullClassCode = dept[i + 1];

                MaterialButton chipBtn = new MaterialButton(this, null, com.google.android.material.R.style.Widget_MaterialComponents_Button_OutlinedButton);
                chipBtn.setText(yearTag);
                chipBtn.setPadding(0, 0, 0, 0);
                chipBtn.setTextSize(12);

                LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(110, 85);
                btnLp.setMargins(6, 0, 6, 0);
                chipBtn.setLayoutParams(btnLp);

                boolean isSelected = selectedClassesList.contains(fullClassCode);
                updateChipStyle(chipBtn, isSelected);

                chipBtn.setOnClickListener(v -> {
                    if (selectedClassesList.contains(fullClassCode)) {
                        selectedClassesList.remove(fullClassCode);
                        updateChipStyle(chipBtn, false);
                    } else {
                        selectedClassesList.add(fullClassCode);
                        updateChipStyle(chipBtn, true);
                    }
                });

                row.addView(chipBtn);
            }

            containerDeptRows.addView(row);
        }

        mainLayout.addView(containerDeptRows);

        // Subject Assignment Text
        final EditText etSubjects = new EditText(this);
        etSubjects.setHint("Assign Subjects / Lectures (e.g. Java, Cloud Computing)");
        etSubjects.setTextSize(14);
        etSubjects.setPadding(16, 16, 16, 16);
        mainLayout.addView(etSubjects);

        // Set Login Password Field
        final EditText etPass = new EditText(this);
        etPass.setHint("Set Login Password 👁️");
        etPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etPass.setTextSize(14);
        etPass.setPadding(16, 20, 16, 16);
        mainLayout.addView(etPass);

        scrollView.addView(mainLayout);
        builder.setView(scrollView);

        builder.setPositiveButton("Generate ID & Save", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String pass = etPass.getText().toString().trim();
            String subjects = etSubjects.getText().toString().trim();

            if (selectedClassesList.isEmpty()) {
                selectedClassesList.add("TY BCA");
            }
            if (subjects.isEmpty()) {
                subjects = "Java, Cloud Computing, UI-UX Design";
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < selectedClassesList.size(); i++) {
                sb.append(selectedClassesList.get(i));
                if (i < selectedClassesList.size() - 1) sb.append(", ");
            }
            String assignedClasses = sb.toString();

            if (!name.isEmpty() && !pass.isEmpty()) {
                Random random = new Random();
                int eightDigitId = 10000000 + random.nextInt(90000000);
                String idStr = "" + eightDigitId;

                // Save to Firebase Cloud
                CloudRepository.saveTeacher(idStr, name, pass, assignedClasses, subjects, email, phone);

                new AlertDialog.Builder(AdminDashboardActivity.this)
                        .setTitle("Faculty Registered! 👨‍🏫")
                        .setMessage("Faculty Name: " + name + "\nAssigned 8-Digit ID: #" + idStr + "\nEmail: " + email + "\nPhone: " + phone + "\nAssigned Classes: " + assignedClasses + "\nAssigned Subjects: " + subjects + "\nPassword: " + pass + "\n\n(Saved to Cloud permanently)")
                        .setPositiveButton("OK", null)
                        .show();
            } else {
                Toast.makeText(AdminDashboardActivity.this, "Please enter Name and Password", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void updateChipStyle(MaterialButton chipBtn, boolean isSelected) {
        if (isSelected) {
            chipBtn.setBackgroundTintList(ColorStateList.valueOf(0xFF86EFAC));
            chipBtn.setTextColor(0xFF065F46);
            chipBtn.setStrokeColor(ColorStateList.valueOf(0xFF16A34A));
        } else {
            chipBtn.setBackgroundTintList(ColorStateList.valueOf(0xFFFFFFFF));
            chipBtn.setTextColor(0xFF0F172A);
            chipBtn.setStrokeColor(ColorStateList.valueOf(0xFFCBD5E1));
        }
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

        final EditText etEmail = new EditText(this);
        etEmail.setHint("Admin Email Address");
        etEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        layout.addView(etEmail);

        final EditText etPhone = new EditText(this);
        etPhone.setHint("Admin Phone Number");
        etPhone.setInputType(InputType.TYPE_CLASS_PHONE);
        layout.addView(etPhone);

        final EditText etPass = new EditText(this);
        etPass.setHint("Set Login Password");
        layout.addView(etPass);

        builder.setView(layout);

        builder.setPositiveButton("Generate ID & Authorize", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String pass = etPass.getText().toString().trim();

            if (!name.isEmpty() && !pass.isEmpty()) {
                Random random = new Random();
                int eightDigitId = 10000000 + random.nextInt(90000000);
                String idStr = "" + eightDigitId;

                // Save to Firebase Cloud
                CloudRepository.saveAdmin(idStr, name, pass, email, phone);

                new AlertDialog.Builder(AdminDashboardActivity.this)
                        .setTitle("Admin Created Successfully! 🛡️")
                        .setMessage("Admin Name: " + name + "\nAssigned 8-Digit ID: #" + idStr + "\nEmail: " + email + "\nPhone: " + phone + "\nSet Password: " + pass + "\n\n(Saved to Cloud permanently)")
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