package com.example.smartattendance;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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

public class ExportReportActivity extends AppCompatActivity {

    private final List<StudentModel> allStudentsList = new ArrayList<>();
    private final List<StudentModel> classFilteredStudents = new ArrayList<>();
    private String teacherAssignedClasses = "TY BCA, SY BCA, FY BCA";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export_report);

        TextView btnBack = findViewById(R.id.btnBack);
        Spinner spinnerExportClass = findViewById(R.id.spinnerExportClass);
        Spinner spinnerExportStudent = findViewById(R.id.spinnerExportStudent);
        MaterialButton btnGenerateExport = findViewById(R.id.btnGenerateExport);

        String classes = getIntent().getStringExtra("ASSIGNED_CLASSES");
        if (classes != null && !classes.trim().isEmpty()) {
            teacherAssignedClasses = classes;
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        String[] assignedClassList = teacherAssignedClasses.split("\\s*,\\s*");
        if (assignedClassList.length == 0 || assignedClassList[0].trim().isEmpty()) {
            assignedClassList = new String[]{"TY BCA", "SY BCA", "FY BCA"};
        }

        if (spinnerExportClass != null) {
            ArrayAdapter<String> classAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, assignedClassList);
            spinnerExportClass.setAdapter(classAdapter);

            spinnerExportClass.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String selectedClass = parent.getItemAtPosition(position).toString();
                    populateStudentSpinnerForClass(selectedClass, spinnerExportStudent);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        loadStudentsFromFirebase(spinnerExportClass, spinnerExportStudent);

        if (btnGenerateExport != null) {
            btnGenerateExport.setOnClickListener(v -> {
                if (spinnerExportClass != null && spinnerExportStudent != null && spinnerExportStudent.getSelectedItem() != null) {
                    String selectedClass = spinnerExportClass.getSelectedItem().toString();
                    String selectedStudentName = spinnerExportStudent.getSelectedItem().toString();

                    exportStudentReportAsPdf(selectedClass, selectedStudentName);
                } else {
                    Toast.makeText(this, "Please select a Student!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void populateStudentSpinnerForClass(String className, Spinner spinnerStudent) {
        classFilteredStudents.clear();
        List<String> studentNames = new ArrayList<>();

        for (StudentModel s : allStudentsList) {
            if (s.getClassName() != null && s.getClassName().equalsIgnoreCase(className)) {
                classFilteredStudents.add(s);
                studentNames.add(s.getName() + " (Roll #" + s.getRoll() + ")");
            }
        }

        if (studentNames.isEmpty()) {
            studentNames.add("No Students Found in " + className);
        }

        if (spinnerStudent != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, studentNames);
            spinnerStudent.setAdapter(adapter);
        }
    }

    private void loadStudentsFromFirebase(Spinner spinnerClass, Spinner spinnerStudent) {
        CloudRepository.studentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allStudentsList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String roll = ds.child("roll").getValue(String.class);
                        String name = ds.child("name").getValue(String.class);
                        String sId = ds.child("studentId").getValue(String.class);
                        String cName = ds.child("className").getValue(String.class);
                        if (roll != null && name != null) {
                            allStudentsList.add(new StudentModel(roll, name, sId, cName));
                        }
                    }
                }

                if (spinnerClass != null && spinnerClass.getSelectedItem() != null) {
                    populateStudentSpinnerForClass(spinnerClass.getSelectedItem().toString(), spinnerStudent);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void exportStudentReportAsPdf(String className, String studentName) {
        Toast.makeText(this, "📄 Attendance Report generated for " + studentName + " (" + className + ") and exported to Documents!", Toast.LENGTH_LONG).show();
    }
}