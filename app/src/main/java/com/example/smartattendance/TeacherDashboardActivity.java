package com.example.smartattendance;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class TeacherDashboardActivity extends AppCompatActivity {

    private TextView tvStudentCount;
    private Spinner spinnerClass, spinnerSubject;
    private MaterialButton btnSelectDate;
    private LinearLayout layoutAttendancePanel;
    private StudentAdapter adapter;
    private final List<StudentModel> currentStudentList = new ArrayList<>();
    private final Map<String, String[]> subjectMap = new HashMap<>();

    private final Calendar selectedCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_dashboard);

        // Bind all Views
        TextView tvFacultyInfo = findViewById(R.id.tvFacultyInfo);
        MaterialCardView btnFacultyAvatar = findViewById(R.id.btnFacultyAvatar);
        TextView tvFacultyAvatarInitials = findViewById(R.id.tvFacultyAvatarInitials);

        tvStudentCount = findViewById(R.id.tvStudentCount);
        spinnerClass = findViewById(R.id.spinnerClass);
        spinnerSubject = findViewById(R.id.spinnerSubject);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        layoutAttendancePanel = findViewById(R.id.layoutAttendancePanel);
        RecyclerView rvStudents = findViewById(R.id.rvStudents);

        MaterialButton btnSubmitAttendance = findViewById(R.id.btnSubmitAttendance);
        MaterialButton btnEmbeddedQuickGrid = findViewById(R.id.btnEmbeddedQuickGrid);
        MaterialButton btnScheduleLecture1 = findViewById(R.id.btnScheduleLecture1);
        MaterialButton btnScheduleLecture2 = findViewById(R.id.btnScheduleLecture2);

        MaterialCardView cardOptionMarkAttendance = findViewById(R.id.cardOptionMarkAttendance);
        MaterialCardView cardOptionPostNotice = findViewById(R.id.cardOptionPostNotice);
        MaterialCardView cardOptionQuickGrid = findViewById(R.id.cardOptionQuickGrid);
        MaterialCardView cardOptionAdjustRoster = findViewById(R.id.cardOptionAdjustRoster);
        MaterialCardView cardOptionExportReport = findViewById(R.id.cardOptionExportReport);
        MaterialCardView cardOptionLectureHistory = findViewById(R.id.cardOptionLectureHistory);

        // Safe Profile / Teacher Info display
        String rawTeacherName = getIntent().getStringExtra("TEACHER_NAME");
        String rawTeacherId = getIntent().getStringExtra("TEACHER_ID");

        String teacherName = (rawTeacherName == null || rawTeacherName.trim().isEmpty())
                ? "Prof. S. R. Patil" : rawTeacherName;
        String teacherId = (rawTeacherId == null || rawTeacherId.trim().isEmpty())
                ? "20481923" : rawTeacherId;

        if (tvFacultyInfo != null) {
            tvFacultyInfo.setText(teacherName + " (#" + teacherId + ")");
        }

        if (tvFacultyAvatarInitials != null && !teacherName.isEmpty()) {
            String[] parts = teacherName.trim().split("\\s+");
            if (parts.length >= 2) {
                tvFacultyAvatarInitials.setText(("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase());
            } else if (!parts[0].isEmpty()) {
                tvFacultyAvatarInitials.setText(("" + parts[0].charAt(0)).toUpperCase());
            }
        }

        if (btnFacultyAvatar != null) {
            btnFacultyAvatar.setOnClickListener(v -> showFacultyProfileDialog(teacherName, teacherId));
        }

        // Initialize Date Button text with REAL-TIME current date
        updateDateButtonText();

        if (btnSelectDate != null) {
            btnSelectDate.setOnClickListener(v -> showDatePicker());
        }

        // Dropdown setup with prompt placeholders
        setupClassesAndSubjects();

        // Schedule Bar Click Listeners (Auto-Fill Class & Subject)
        if (btnScheduleLecture1 != null) {
            btnScheduleLecture1.setOnClickListener(v -> {
                if (layoutAttendancePanel != null) layoutAttendancePanel.setVisibility(View.VISIBLE);
                if (spinnerClass != null) spinnerClass.setSelection(1); // TY BCA
                if (spinnerSubject != null) spinnerSubject.setSelection(1); // Core Java
            });
        }

        if (btnScheduleLecture2 != null) {
            btnScheduleLecture2.setOnClickListener(v -> {
                if (layoutAttendancePanel != null) layoutAttendancePanel.setVisibility(View.VISIBLE);
                if (spinnerClass != null) spinnerClass.setSelection(1); // TY BCA
                if (spinnerSubject != null) spinnerSubject.setSelection(4); // Cloud Computing
            });
        }

        // Option Hub Handlers
        if (cardOptionMarkAttendance != null) {
            cardOptionMarkAttendance.setOnClickListener(v -> {
                if (layoutAttendancePanel != null) {
                    int newVisibility = (layoutAttendancePanel.getVisibility() == View.VISIBLE) ? View.GONE : View.VISIBLE;
                    layoutAttendancePanel.setVisibility(newVisibility);
                }
            });
        }

        if (cardOptionPostNotice != null) {
            cardOptionPostNotice.setOnClickListener(v -> showPostFacultyNoticeDialog(teacherName));
        }

        if (cardOptionQuickGrid != null) {
            cardOptionQuickGrid.setOnClickListener(v -> openQuickRollGrid());
        }

        if (btnEmbeddedQuickGrid != null) {
            btnEmbeddedQuickGrid.setOnClickListener(v -> openQuickRollGrid());
        }

        if (cardOptionAdjustRoster != null) {
            cardOptionAdjustRoster.setOnClickListener(v -> showAdjustRosterDialog());
        }

        if (cardOptionExportReport != null) {
            cardOptionExportReport.setOnClickListener(v -> showExportReportDialog());
        }

        if (cardOptionLectureHistory != null) {
            cardOptionLectureHistory.setOnClickListener(v -> showLectureHistoryDialog());
        }

        // RecyclerView setup
        if (rvStudents != null) {
            rvStudents.setLayoutManager(new LinearLayoutManager(this));
            adapter = new StudentAdapter(currentStudentList, position -> {});
            rvStudents.setAdapter(adapter);
        }

        // Submit Attendance Action
        if (btnSubmitAttendance != null) {
            btnSubmitAttendance.setOnClickListener(v -> {
                if (spinnerClass != null && spinnerClass.getSelectedItemPosition() == 0) {
                    Toast.makeText(this, "Please select a Class!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (spinnerSubject != null && spinnerSubject.getSelectedItemPosition() == 0) {
                    Toast.makeText(this, "Please select a Subject!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (currentStudentList.isEmpty()) {
                    Toast.makeText(this, "No students to submit attendance!", Toast.LENGTH_SHORT).show();
                    return;
                }

                String selectedClass = spinnerClass.getSelectedItem().toString();
                String selectedSub = spinnerSubject.getSelectedItem().toString();

                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                String formattedDate = sdf.format(selectedCalendar.getTime());

                // Save attendance for each student
                for (StudentModel student : currentStudentList) {
                    AttendanceData.markAttendance(student.getName(), selectedSub, student.isPresent());
                }

                Toast.makeText(this, "Attendance saved for " + formattedDate + " - " + selectedClass + " (" + selectedSub + ")!", Toast.LENGTH_LONG).show();
            });
        }
    }

    private void showExportReportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📄 Export Attendance Sheet & CSV");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 20);

        final Spinner spinnerExportClass = new Spinner(this);
        String[] classesList = {"TY BCA", "SY BCA", "FY BCA", "TY B.Sc (CS)", "SY B.Sc (CS)", "FY B.Sc (CS)", "12th (HSC)"};
        ArrayAdapter<String> classAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, classesList);
        spinnerExportClass.setAdapter(classAdapter);
        layout.addView(spinnerExportClass);

        final Spinner spinnerExportSub = new Spinner(this);
        String[] subList = {"Core Java", "Software Engineering", "Cloud Computing", "UI & UX Design", "Project"};
        ArrayAdapter<String> subAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, subList);
        spinnerExportSub.setAdapter(subAdapter);
        layout.addView(spinnerExportSub);

        builder.setView(layout);

        builder.setPositiveButton("Generate & Export CSV", (dialog, which) -> {
            String selectedClass = spinnerExportClass.getSelectedItem().toString();
            String selectedSub = spinnerExportSub.getSelectedItem().toString();

            AttendanceReportUtil.exportAndShareReport(this, selectedClass, selectedSub, currentStudentList);
            Toast.makeText(this, "Exporting Attendance Report for " + selectedClass + "...", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void openQuickRollGrid() {
        String selectedClass = (spinnerClass != null && spinnerClass.getSelectedItemPosition() > 0)
                ? spinnerClass.getSelectedItem().toString() : "TY BCA";
        String selectedSub = (spinnerSubject != null && spinnerSubject.getSelectedItemPosition() > 0)
                ? spinnerSubject.getSelectedItem().toString() : "Core Java";

        Intent intent = new Intent(TeacherDashboardActivity.this, QuickRollGridActivity.class);
        intent.putExtra("SELECTED_CLASS", selectedClass);
        intent.putExtra("SELECTED_SUBJECT", selectedSub);
        startActivity(intent);
    }

    private void showFacultyProfileDialog(String name, String id) {
        new AlertDialog.Builder(this)
                .setTitle("👨‍🏫 Faculty Profile")
                .setMessage("Name: " + name + "\nFaculty ID: #" + id + "\nDepartment: Computer Science & Applications\nAccount Status: Active & Cloud-Synced")
                .setPositiveButton("Close", null)
                .show();
    }

    private void updateDateButtonText() {
        if (btnSelectDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            String formattedDate = sdf.format(selectedCalendar.getTime());
            btnSelectDate.setText(getString(R.string.select_date_btn, formattedDate));
        }
    }

    private void showDatePicker() {
        int year = selectedCalendar.get(Calendar.YEAR);
        int month = selectedCalendar.get(Calendar.MONTH);
        int day = selectedCalendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
            selectedCalendar.set(Calendar.YEAR, selectedYear);
            selectedCalendar.set(Calendar.MONTH, selectedMonth);
            selectedCalendar.set(Calendar.DAY_OF_MONTH, selectedDay);
            updateDateButtonText();
        }, year, month, day);

        dialog.show();
    }

    private void showPostFacultyNoticeDialog(String teacherName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📢 Post Faculty Notice / Update");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 20);

        final EditText etTitle = new EditText(this);
        etTitle.setHint("Notice Title (e.g. Tomorrow Extra Lecture)");
        layout.addView(etTitle);

        final EditText etMessage = new EditText(this);
        etMessage.setHint("Notice Message / Details");
        layout.addView(etMessage);

        builder.setView(layout);

        builder.setPositiveButton("Post Notice", (dialog, which) -> {
            String title = etTitle.getText().toString().trim();
            String msg = etMessage.getText().toString().trim();

            if (!title.isEmpty() && !msg.isEmpty()) {
                CloudRepository.postAnnouncement(title, msg, "Faculty Update", teacherName);
                Toast.makeText(this, "Faculty notice posted successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Please enter both Title and Message", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNeutralButton("🗑️ Clear All Notices", (dialog, which) -> {
            CloudRepository.clearAllAnnouncements();
            Toast.makeText(this, "Notice History Cleared!", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    // --- Decoupled Independent Adjust Student Roster Dialog ---
    private void showAdjustRosterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("👥 Adjust Student Roster");

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(30, 20, 30, 10);

        // 1. Independent Class Dropdown Selector
        final Spinner spinnerClassRoster = new Spinner(this);
        String[] classesList = {
                "TY BCA", "SY BCA", "FY BCA",
                "TY B.Sc (CS)", "SY B.Sc (CS)", "FY B.Sc (CS)",
                "TY B.Sc", "SY B.Sc", "FY B.Sc",
                "TY BBA", "SY BBA", "FY BBA",
                "TY B.Com", "SY B.Com", "FY B.Com",
                "12th (HSC)", "11th (FYJC)"
        };
        ArrayAdapter<String> classAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, classesList);
        spinnerClassRoster.setAdapter(classAdapter);
        mainLayout.addView(spinnerClassRoster);

        // 2. Search Input Box
        final EditText etSearchRoster = new EditText(this);
        etSearchRoster.setHint("🔍 Search Name, Roll # or Student ID...");
        etSearchRoster.setPadding(20, 16, 20, 16);
        mainLayout.addView(etSearchRoster);

        ScrollView scrollView = new ScrollView(this);
        final LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(10, 10, 10, 10);

        final List<StudentModel> classRosterList = new ArrayList<>();

        spinnerClassRoster.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedClass = classesList[position];
                fetchRosterForClass(selectedClass, classRosterList, container, etSearchRoster);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        etSearchRoster.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String activeClass = classesList[spinnerClassRoster.getSelectedItemPosition()];
                populateRosterRows(container, classRosterList, s.toString().trim(), activeClass);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        scrollView.addView(container);
        mainLayout.addView(scrollView);
        builder.setView(mainLayout);

        builder.setPositiveButton("+ Add Student", (dialog, which) -> {
            String activeClass = classesList[spinnerClassRoster.getSelectedItemPosition()];
            showAddStudentDialog(activeClass);
        });

        builder.setNegativeButton("Close", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void fetchRosterForClass(String className, List<StudentModel> rosterList, LinearLayout container, EditText searchBox) {
        CloudRepository.studentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                rosterList.clear();
                List<StudentModel> temp = new ArrayList<>();

                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String name = ds.child("name").getValue(String.class);
                        String cName = ds.child("className").getValue(String.class);
                        String roll = ds.child("roll").getValue(String.class);
                        String sId = ds.child("studentId").getValue(String.class);

                        if (name != null) {
                            if (cName != null && cName.equalsIgnoreCase(className)) {
                                temp.add(new StudentModel(roll != null ? roll : "0", name, sId, className));
                            } else if (cName == null && Objects.equals(className, "TY BCA")) {
                                temp.add(new StudentModel(roll != null ? roll : "0", name, sId, className));
                            }
                        }
                    }
                }

                if (!temp.isEmpty()) {
                    temp.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                    for (int i = 0; i < temp.size(); i++) {
                        StudentModel old = temp.get(i);
                        rosterList.add(new StudentModel(String.valueOf(i + 1), old.getName(), old.getStudentId(), className));
                    }
                }

                String q = searchBox != null ? searchBox.getText().toString().trim() : "";
                populateRosterRows(container, rosterList, q, className);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void populateRosterRows(LinearLayout container, List<StudentModel> rosterList, String query, String className) {
        container.removeAllViews();

        List<StudentModel> filtered = new ArrayList<>();
        for (StudentModel s : rosterList) {
            if (query.isEmpty()
                    || s.getName().toLowerCase().contains(query.toLowerCase())
                    || s.getRoll().equalsIgnoreCase(query)
                    || s.getStudentId().equalsIgnoreCase(query)) {
                filtered.add(s);
            }
        }

        if (filtered.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No students found in " + className);
            tvEmpty.setPadding(10, 20, 10, 20);
            tvEmpty.setTextColor(0xFF64748B);
            container.addView(tvEmpty);
        } else {
            for (StudentModel s : filtered) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 12, 0, 12);

                TextView tvInfo = new TextView(this);
                tvInfo.setText("Roll #" + s.getRollNo() + " : " + s.getName() + "\nID: #" + s.getStudentId());
                tvInfo.setTextSize(14);
                tvInfo.setTextColor(0xFF1E293B);
                tvInfo.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

                TextView btnDel = new TextView(this);
                btnDel.setText("✕ Remove");
                btnDel.setTextColor(0xFFDC2626);
                btnDel.setTextSize(13);
                btnDel.setPadding(12, 6, 12, 6);
                btnDel.setOnClickListener(v -> showDeleteStudentConfirmation(s, className));

                row.addView(tvInfo);
                row.addView(btnDel);
                container.addView(row);
            }
        }
    }

    private void showDeleteStudentConfirmation(StudentModel student, String className) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Student?")
                .setMessage("Are you sure you want to remove " + student.getName() + " (Roll #" + student.getRollNo() + ") from " + className + "?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    CloudRepository.removeStudentAndReorder(student.getRollNo(), className);
                    Toast.makeText(this, student.getName() + " removed from " + className + "!", Toast.LENGTH_SHORT).show();
                    loadStudentsForClass(className);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
    }

    private void showAddStudentDialog(String className) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Student to " + className);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 20);

        final EditText etName = new EditText(this);
        etName.setHint("Enter Student Full Name");
        layout.addView(etName);

        final EditText etPass = new EditText(this);
        etPass.setHint("Set Login Password");
        layout.addView(etPass);

        builder.setView(layout);

        builder.setPositiveButton("Add & Save", (dialog, which) -> {
            String rawName = etName.getText().toString().trim();
            String pass = etPass.getText().toString().trim();

            if (!rawName.isEmpty() && !pass.isEmpty()) {
                String formattedName = CloudRepository.formatToTitleCase(rawName);
                CloudRepository.addStudentAndReorder(rawName, pass, className);
                Toast.makeText(this, formattedName + " added to " + className + "!", Toast.LENGTH_SHORT).show();
                loadStudentsForClass(className);
            } else {
                Toast.makeText(this, "Please enter both Student Name and Password", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showLectureHistoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📊 Saved Lecture Attendance History Log");

        ScrollView scrollView = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(40, 20, 40, 20);

        CloudRepository.attendanceRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    TextView tvEmpty = new TextView(TeacherDashboardActivity.this);
                    tvEmpty.setText("No saved lecture attendance history found.");
                    tvEmpty.setTextColor(0xFF64748B);
                    container.addView(tvEmpty);
                } else {
                    for (DataSnapshot studentDs : snapshot.getChildren()) {
                        String studentName = studentDs.getKey();
                        for (DataSnapshot subDs : studentDs.getChildren()) {
                            String subjectName = subDs.getKey();
                            Long a = subDs.child("attended").getValue(Long.class);
                            Long t = subDs.child("total").getValue(Long.class);

                            long attended = Objects.requireNonNullElse(a, 0L);
                            long total = Objects.requireNonNullElse(t, 0L);

                            TextView tvLog = new TextView(TeacherDashboardActivity.this);
                            tvLog.setText("• " + studentName + " | " + subjectName + " : " + attended + "/" + total + " Lectures Attended");
                            tvLog.setTextSize(13);
                            tvLog.setPadding(0, 6, 0, 8);
                            tvLog.setTextColor(0xFF334155);
                            container.addView(tvLog);
                        }
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

    private void updateCountText() {
        if (tvStudentCount != null) {
            tvStudentCount.setText(getString(R.string.student_roster_format, currentStudentList.size()));
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void notifyAdapterChanged() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void setupClassesAndSubjects() {
        String[] classesWithPrompt = {
                "Select Class...",
                "TY BCA", "SY BCA", "FY BCA",
                "TY B.Sc (CS)", "SY B.Sc (CS)", "FY B.Sc (CS)",
                "TY B.Sc", "SY B.Sc", "FY B.Sc",
                "TY BBA", "SY BBA", "FY BBA",
                "TY B.Com", "SY B.Com", "FY B.Com",
                "12th (HSC)", "11th (FYJC)"
        };

        subjectMap.put("TY BCA", new String[]{"Select Subject...", "Core Java", "Entrepreneurship", "Software Engineering", "Cloud Computing", "UI & UX Design", "Project"});
        subjectMap.put("SY BCA", new String[]{"Select Subject...", "Data Structures", "Database Management (DBMS)", "C++ Programming", "Web Technology (PHP)", "Computer Networks", "Environmental Studies"});
        subjectMap.put("FY BCA", new String[]{"Select Subject...", "C Programming", "Basics of Computer Science", "Office Automation", "Applied Mathematics", "Business Communication", "Principles of Management"});

        subjectMap.put("TY B.Sc (CS)", new String[]{"Select Subject...", "Advanced Java", "Operating Systems", "Python Programming", "Software Testing", "Machine Learning", "Final Capstone Project"});
        subjectMap.put("SY B.Sc (CS)", new String[]{"Select Subject...", "Data Structures in C", "RDBMS (PostgreSQL)", "Computer Networks", "Microcontroller Architecture", "Computational Statistics", "Technical Writing"});
        subjectMap.put("FY B.Sc (CS)", new String[]{"Select Subject...", "Problem Solving Using C", "Basics of Linux", "Discrete Mathematics", "Digital Electronics", "Calculus", "Descriptive Statistics"});

        subjectMap.put("TY B.Sc", new String[]{"Select Subject...", "Solid State Physics", "Organic Chemistry", "Real Analysis", "Nuclear Physics", "Industrial Chemistry", "Specialization Project"});
        subjectMap.put("SY B.Sc", new String[]{"Select Subject...", "Optics & Thermodynamics", "Physical & Inorganic Chemistry", "Multivariable Calculus", "Differential Equations", "Instrumentation", "Biophysics"});
        subjectMap.put("FY B.Sc", new String[]{"Select Subject...", "Mechanics & Properties of Matter", "Fundamentals of Chemistry", "Algebra & Geometry", "Basic Botany", "Basic Zoology", "Scientific English"});

        subjectMap.put("TY BBA", new String[]{"Select Subject...", "Strategic Management", "Supply Chain Management", "International Business", "Digital Marketing", "Entrepreneurship Development", "Management Project"});
        subjectMap.put("SY BBA", new String[]{"Select Subject...", "Human Resource Management", "Marketing Management", "Financial Management", "Production & Operations", "Business Law", "Management Accounting"});
        subjectMap.put("FY BBA", new String[]{"Select Subject...", "Principles of Management", "Business Economics (Micro)", "Business Communication", "Business Mathematics", "Financial Accounting", "Computer Applications"});

        subjectMap.put("TY B.Com", new String[]{"Select Subject...", "Advanced Accounting", "Auditing & Taxation", "Indian Economy", "Corporate Law", "Cost & Works Accounting", "Banking Specialization"});
        subjectMap.put("SY B.Com", new String[]{"Select Subject...", "Corporate Accounting", "Business Economics (Macro)", "Business Communication", "Business Administration", "Company Law", "Banking & Finance"});
        subjectMap.put("FY B.Com", new String[]{"Select Subject...", "Financial Accounting", "Business Economics", "Business Mathematics & Stats", "Banking Operations", "Marketing Essentials", "Compulsory English"});

        subjectMap.put("12th (HSC)", new String[]{"Select Subject...", "English", "Physics", "Chemistry", "Mathematics & Statistics", "Biology", "Information Technology"});
        subjectMap.put("11th (FYJC)", new String[]{"Select Subject...", "English", "Physics", "Chemistry", "Mathematics", "Biology", "Environmental Education"});

        if (spinnerClass != null) {
            ArrayAdapter<String> classAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, classesWithPrompt);
            spinnerClass.setAdapter(classAdapter);

            spinnerClass.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position > 0) {
                        String selectedClass = classesWithPrompt[position];
                        String[] subs = subjectMap.get(selectedClass);
                        if (subs != null && spinnerSubject != null) {
                            ArrayAdapter<String> subAdapter = new ArrayAdapter<>(TeacherDashboardActivity.this, android.R.layout.simple_spinner_dropdown_item, subs);
                            spinnerSubject.setAdapter(subAdapter);
                        }
                        loadStudentsForClass(selectedClass);
                    } else {
                        currentStudentList.clear();
                        notifyAdapterChanged();
                        updateCountText();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }
    }

    private void loadStudentsForClass(String className) {
        CloudRepository.studentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentStudentList.clear();
                List<StudentModel> cloudMatched = new ArrayList<>();

                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String name = ds.child("name").getValue(String.class);
                        String cName = ds.child("className").getValue(String.class);

                        if (name != null) {
                            if (cName != null && cName.equalsIgnoreCase(className)) {
                                cloudMatched.add(new StudentModel("0", name));
                            } else if (cName == null && Objects.equals(className, "TY BCA")) {
                                cloudMatched.add(new StudentModel("0", name));
                            }
                        }
                    }
                }

                if (!cloudMatched.isEmpty()) {
                    cloudMatched.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                    for (int i = 0; i < cloudMatched.size(); i++) {
                        currentStudentList.add(new StudentModel(String.valueOf(i + 1), cloudMatched.get(i).getName()));
                    }
                } else {
                    loadDefaultRosterForClass(className);
                }

                notifyAdapterChanged();
                updateCountText();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loadDefaultRosterForClass(className);
                notifyAdapterChanged();
                updateCountText();
            }
        });
    }

    private void loadDefaultRosterForClass(String className) {
        currentStudentList.clear();

        switch (className) {
            case "TY BCA":
                List<String> tyBcaNames = new ArrayList<>();
                tyBcaNames.add("Aditya Thete");
                tyBcaNames.add("Tejas Rajwade");
                tyBcaNames.add("Gaurav Pawar");
                tyBcaNames.add("Atharv Gaikwad");
                tyBcaNames.add("Komal Jagtap");
                tyBcaNames.add("Bhakti Samudre");
                tyBcaNames.add("Kajal Jadhav");
                tyBcaNames.add("Vaishnavi Jadhav");
                tyBcaNames.sort(null);

                for (int i = 0; i < tyBcaNames.size(); i++) {
                    currentStudentList.add(new StudentModel(String.valueOf(i + 1), tyBcaNames.get(i)));
                }
                break;
            case "SY BCA":
                String[] syBcaNames = {"Abhishek More", "Aniket Shinde", "Chetan Patil", "Deepak Thorat", "Harshada Deshmukh", "Jayesh Kadam", "Manish Wagh", "Mayuri Gite", "Nilesh Chavan", "Pooja Bhamre", "Prasad Joshi", "Rohit Sonawane"};
                for (int i = 0; i < syBcaNames.length; i++) {
                    currentStudentList.add(new StudentModel(String.valueOf(i + 1), syBcaNames[i]));
                }
                break;
            case "FY BCA":
                String[] fyBcaNames = {"Akash Pawar", "Bhavana Kale", "Dhananjay Jagtap", "Ishwar Ghadge", "Kunal Desale", "Madhuri Kulkarni", "Pratiksha Gaikwad", "Rakesh Shinde", "Sagar Patil", "Snehal Jadhav", "Tushar More", "Yogesh Thorat"};
                for (int i = 0; i < fyBcaNames.length; i++) {
                    currentStudentList.add(new StudentModel(String.valueOf(i + 1), fyBcaNames[i]));
                }
                break;
            default:
                String[] boyFirstNames = {"Aarav", "Rohan", "Siddharth", "Omkar", "Kunal", "Pranav", "Abhishek", "Harshal", "Sanket", "Mayur", "Rushikesh", "Swapnil", "Aniket", "Suraj", "Nikhil", "Yash", "Deepak", "Prathamesh", "Digambar", "Vicky", "Sachin", "Ajay", "Sumit", "Ajinkya", "Amol"};
                String[] girlFirstNames = {"Ananya", "Sakshi", "Pooja", "Neha", "Sayali", "Snehal", "Shruti", "Tanvi", "Rutuja", "Pranali", "Aishwarya", "Shweta", "Poonam", "Akshata", "Mrunal", "Divya", "Gayatri", "Kalyani", "Pallavi", "Sheetal", "Kavita", "Rupali", "Monika", "Sonali", "Archana"};
                String[] lastNames = {"Patil", "Shinde", "Deshmukh", "More", "Kulkarni", "Joshi", "Gite", "Gaikwad", "Kale", "Wagh", "Sonawane", "Thorat", "Ghadge", "Jadhav", "Chavan", "Jagtap", "Bhamre", "Pawar", "Kadam", "Borse", "Salunkhe", "Mane", "Nikam", "Tambe", "Shelar"};

                int baseSeed = Math.abs(className.hashCode());
                int targetCount = className.contains("B.Sc (CS)") ? 75 : 38;

                for (int i = 1; i <= targetCount; i++) {
                    int seedIdx = (baseSeed + (i * 7));
                    String lName = lastNames[Math.abs((seedIdx + i) % lastNames.length)];
                    String fName = (i % 2 == 0) ? girlFirstNames[Math.abs((seedIdx + 3) % girlFirstNames.length)] : boyFirstNames[Math.abs((seedIdx + 5) % girlFirstNames.length)];
                    currentStudentList.add(new StudentModel(String.valueOf(i), fName + " " + lName));
                }
                break;
        }
    }

    public interface OnStudentDeleteListener {
        void onDelete(int position);
    }

    private static class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {
        private final List<StudentModel> list;
        private final OnStudentDeleteListener deleteListener;

        public StudentAdapter(List<StudentModel> list, OnStudentDeleteListener deleteListener) {
            this.list = list;
            this.deleteListener = deleteListener;
        }

        @NonNull
        @Override
        public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student, parent, false);
            return new StudentViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
            StudentModel s = list.get(position);
            if (holder.tvStudentInfo != null) {
                holder.tvStudentInfo.setText(holder.itemView.getContext().getString(R.string.student_info_format, s.getRollNo(), s.getName()));
            }
            if (holder.cbAttendance != null) {
                holder.cbAttendance.setOnCheckedChangeListener(null);
                holder.cbAttendance.setChecked(s.isPresent());
                holder.cbAttendance.setOnCheckedChangeListener((btn, isChecked) -> s.setPresent(isChecked));
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class StudentViewHolder extends RecyclerView.ViewHolder {
            TextView tvStudentInfo, btnDeleteStudent;
            CheckBox cbAttendance;

            public StudentViewHolder(@NonNull View itemView) {
                super(itemView);
                tvStudentInfo = itemView.findViewById(R.id.tvStudentInfo);
                btnDeleteStudent = itemView.findViewById(R.id.btnDeleteStudent);
                cbAttendance = itemView.findViewById(R.id.cbAttendance);
            }
        }
    }
}