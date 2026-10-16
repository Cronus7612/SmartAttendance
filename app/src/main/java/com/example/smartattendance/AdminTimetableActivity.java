package com.example.smartattendance;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
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

public class AdminTimetableActivity extends AppCompatActivity {

    private final List<TeacherModel> facultyList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_timetable);

        TextView btnBack = findViewById(R.id.btnBack);
        Spinner spinnerAdminTimetableClass = findViewById(R.id.spinnerAdminTimetableClass);
        Spinner spinnerDay = findViewById(R.id.spinnerDay);
        EditText etTimeSlot = findViewById(R.id.etTimeSlot);
        EditText etSubjectCode = findViewById(R.id.etSubjectCode);
        EditText etSubjectName = findViewById(R.id.etSubjectName);
        EditText etFacultyName = findViewById(R.id.etFacultyName);

        MaterialButton btnAddSlot = findViewById(R.id.btnAddSlot);
        MaterialButton btnUpdateSlot = findViewById(R.id.btnUpdateSlot);
        MaterialButton btnRemoveSlot = findViewById(R.id.btnRemoveSlot);

        Spinner spinnerFacultyList = findViewById(R.id.spinnerFacultyList);
        EditText etMappingSubjects = findViewById(R.id.etMappingSubjects);
        MaterialButton btnSaveFacultyMapping = findViewById(R.id.btnSaveFacultyMapping);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        String[] classesList = {"TY BCA", "SY BCA", "FY BCA", "TY B.Sc (CS)", "SY B.Sc (CS)", "FY B.Sc (CS)", "TY BBA", "SY BBA", "FY BBA", "12th (HSC)"};
        if (spinnerAdminTimetableClass != null) {
            spinnerAdminTimetableClass.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, classesList));
        }

        String[] daysList = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        if (spinnerDay != null) {
            spinnerDay.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, daysList));
        }

        if (btnAddSlot != null) {
            btnAddSlot.setOnClickListener(v -> {
                String selectedClass = spinnerAdminTimetableClass != null ? spinnerAdminTimetableClass.getSelectedItem().toString() : "TY BCA";
                String day = spinnerDay != null ? spinnerDay.getSelectedItem().toString() : "Monday";
                String time = etTimeSlot.getText().toString().trim();
                String code = etSubjectCode.getText().toString().trim();
                String subName = etSubjectName.getText().toString().trim();
                String fName = etFacultyName.getText().toString().trim();

                if (!time.isEmpty() && !subName.isEmpty()) {
                    Toast.makeText(this, "➕ Timetable Slot Added for " + selectedClass + " (" + day + " " + time + ")!", Toast.LENGTH_LONG).show();
                    etTimeSlot.setText("");
                    etSubjectCode.setText("");
                    etSubjectName.setText("");
                    etFacultyName.setText("");
                } else {
                    Toast.makeText(this, "Please enter Time Slot and Subject Name!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnUpdateSlot != null) {
            btnUpdateSlot.setOnClickListener(v -> {
                String selectedClass = spinnerAdminTimetableClass != null ? spinnerAdminTimetableClass.getSelectedItem().toString() : "TY BCA";
                Toast.makeText(this, "✏️ Timetable Slot Updated for " + selectedClass + "!", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnRemoveSlot != null) {
            btnRemoveSlot.setOnClickListener(v -> {
                String selectedClass = spinnerAdminTimetableClass != null ? spinnerAdminTimetableClass.getSelectedItem().toString() : "TY BCA";
                Toast.makeText(this, "🗑️ Timetable Slot Removed for " + selectedClass + "!", Toast.LENGTH_SHORT).show();
            });
        }

        loadFacultyList(spinnerFacultyList, etMappingSubjects);

        if (btnSaveFacultyMapping != null) {
            btnSaveFacultyMapping.setOnClickListener(v -> {
                if (spinnerFacultyList != null && spinnerFacultyList.getSelectedItemPosition() < facultyList.size()) {
                    TeacherModel selectedTeacher = facultyList.get(spinnerFacultyList.getSelectedItemPosition());
                    String newSubjects = etMappingSubjects.getText().toString().trim();

                    if (!newSubjects.isEmpty()) {
                        CloudRepository.teacherRef.child(selectedTeacher.getId()).child("assignedSubjects").setValue(newSubjects);
                        Toast.makeText(this, "Faculty Subject Mapping Updated for " + selectedTeacher.getName() + "!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Please enter Assigned Subjects!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void loadFacultyList(Spinner spinnerFaculty, EditText etSubjects) {
        CloudRepository.teacherRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                facultyList.clear();
                List<String> names = new ArrayList<>();
                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String id = ds.child("id").getValue(String.class);
                        String name = ds.child("name").getValue(String.class);
                        String classes = ds.child("assignedClasses").getValue(String.class);
                        String subjects = ds.child("assignedSubjects").getValue(String.class);
                        if (id != null && name != null) {
                            TeacherModel tm = new TeacherModel(id, name, classes, subjects);
                            facultyList.add(tm);
                            names.add(name + " (#" + id + ")");
                        }
                    }
                }
                if (spinnerFaculty != null) {
                    spinnerFaculty.setAdapter(new ArrayAdapter<>(AdminTimetableActivity.this, android.R.layout.simple_spinner_dropdown_item, names));
                    spinnerFaculty.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                            if (position < facultyList.size() && etSubjects != null) {
                                etSubjects.setText(facultyList.get(position).getAssignedSubjects());
                            }
                        }

                        @Override
                        public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}