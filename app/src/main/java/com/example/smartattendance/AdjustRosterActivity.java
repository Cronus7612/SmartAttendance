package com.example.smartattendance;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AdjustRosterActivity extends AppCompatActivity {

    private LinearLayout containerRosterRows;
    private final List<StudentModel> classRosterList = new ArrayList<>();
    private String teacherAssignedClasses = "TY BCA, SY BCA, FY BCA";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adjust_roster);

        TextView btnBack = findViewById(R.id.btnBack);
        Spinner spinnerClassRoster = findViewById(R.id.spinnerClassRoster);
        EditText etSearchRoster = findViewById(R.id.etSearchRoster);
        MaterialButton btnAddStudent = findViewById(R.id.btnAddStudent);
        containerRosterRows = findViewById(R.id.containerRosterRows);

        String classes = getIntent().getStringExtra("ASSIGNED_CLASSES");
        if (classes != null && !classes.trim().isEmpty()) {
            teacherAssignedClasses = classes;
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        String[] classesList = teacherAssignedClasses.split("\\s*,\\s*");
        if (classesList.length == 0 || classesList[0].trim().isEmpty()) {
            classesList = new String[]{"TY BCA", "SY BCA", "FY BCA"};
        }

        final String[] finalClassesList = classesList;

        ArrayAdapter<String> classAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, finalClassesList);
        spinnerClassRoster.setAdapter(classAdapter);

        spinnerClassRoster.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedClass = finalClassesList[position];
                fetchRosterForClass(selectedClass, etSearchRoster);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        etSearchRoster.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int pos = spinnerClassRoster.getSelectedItemPosition();
                String activeClass = pos >= 0 && pos < finalClassesList.length ? finalClassesList[pos] : "TY BCA";
                populateRosterRows(s.toString().trim(), activeClass);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        if (btnAddStudent != null) {
            btnAddStudent.setOnClickListener(v -> {
                int pos = spinnerClassRoster.getSelectedItemPosition();
                String activeClass = pos >= 0 && pos < finalClassesList.length ? finalClassesList[pos] : "TY BCA";
                showAddStudentDialog(activeClass);
            });
        }
    }

    private void fetchRosterForClass(String className, EditText searchBox) {
        CloudRepository.studentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                classRosterList.clear();
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
                        classRosterList.add(new StudentModel(String.valueOf(i + 1), old.getName(), old.getStudentId(), className));
                    }
                }

                String q = searchBox != null ? searchBox.getText().toString().trim() : "";
                populateRosterRows(q, className);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void populateRosterRows(String query, String className) {
        if (containerRosterRows == null) return;
        containerRosterRows.removeAllViews();

        List<StudentModel> filtered = new ArrayList<>();
        for (StudentModel s : classRosterList) {
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
            containerRosterRows.addView(tvEmpty);
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
                containerRosterRows.addView(row);
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
                    fetchRosterForClass(className, null);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
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
                fetchRosterForClass(className, null);
            } else {
                Toast.makeText(this, "Please enter both Student Name and Password", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}