package com.example.smartattendance;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class MarkAttendanceActivity extends AppCompatActivity {

    private TextView tvRosterCount;
    private StudentAdapter adapter;
    private final List<StudentModel> studentList = new ArrayList<>();

    private String selectedClass = "TY BCA";
    private String selectedSubject = "Core Java";
    private String selectedDate = "2026-10-10";
    private String lectureNumber = "Lecture 1";
    private String lectureType = "Regular";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mark_attendance);

        TextView btnBack = findViewById(R.id.btnBack);
        TextView tvSessionHeader = findViewById(R.id.tvSessionHeader);
        TextView tvSessionMeta = findViewById(R.id.tvSessionMeta);
        tvRosterCount = findViewById(R.id.tvRosterCount);

        MaterialButton btnSelectAll = findViewById(R.id.btnSelectAll);
        MaterialButton btnDeselectAll = findViewById(R.id.btnDeselectAll);
        MaterialButton btnSaveAttendance = findViewById(R.id.btnSaveAttendance);
        RecyclerView rvMarkStudents = findViewById(R.id.rvMarkStudents);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Get Intent details
        String c = getIntent().getStringExtra("SELECTED_CLASS");
        String s = getIntent().getStringExtra("SELECTED_SUBJECT");
        String d = getIntent().getStringExtra("SELECTED_DATE");
        String lNum = getIntent().getStringExtra("LECTURE_NUMBER");
        String lType = getIntent().getStringExtra("LECTURE_TYPE");

        if (c != null && !c.isEmpty()) selectedClass = c;
        if (s != null && !s.isEmpty()) selectedSubject = s;
        if (d != null && !d.isEmpty()) selectedDate = d;
        if (lNum != null && !lNum.isEmpty()) lectureNumber = lNum;
        if (lType != null && !lType.isEmpty()) lectureType = lType;

        if (tvSessionHeader != null) {
            tvSessionHeader.setText("Class: " + selectedClass + " | Subject: " + selectedSubject);
        }
        if (tvSessionMeta != null) {
            tvSessionMeta.setText("Date: " + selectedDate + " | " + lectureNumber + " (" + lectureType + ")");
        }

        // Setup RecyclerView
        if (rvMarkStudents != null) {
            rvMarkStudents.setLayoutManager(new LinearLayoutManager(this));
            adapter = new StudentAdapter(studentList);
            rvMarkStudents.setAdapter(adapter);
        }

        // Load student roster for selected class
        loadRosterForClass(selectedClass);

        // Select All Handler
        if (btnSelectAll != null) {
            btnSelectAll.setOnClickListener(v -> {
                for (StudentModel sm : studentList) {
                    sm.setPresent(true);
                }
                notifyAdapterChanged();
                Toast.makeText(this, "Marked All Students Present ✓", Toast.LENGTH_SHORT).show();
            });
        }

        // Deselect All Handler
        if (btnDeselectAll != null) {
            btnDeselectAll.setOnClickListener(v -> {
                for (StudentModel sm : studentList) {
                    sm.setPresent(false);
                }
                notifyAdapterChanged();
                Toast.makeText(this, "Marked All Students Absent ✕", Toast.LENGTH_SHORT).show();
            });
        }

        // Save & Submit Attendance Handler
        if (btnSaveAttendance != null) {
            btnSaveAttendance.setOnClickListener(v -> {
                if (studentList.isEmpty()) {
                    Toast.makeText(this, "No students to submit attendance!", Toast.LENGTH_SHORT).show();
                    return;
                }

                Random random = new Random();
                int logIdNum = 2600 + random.nextInt(900);
                String attendanceId = String.valueOf(logIdNum);

                int presentCount = 0;
                for (StudentModel student : studentList) {
                    if (student.isPresent()) presentCount++;
                    AttendanceData.markAttendance(student.getName(), selectedSubject, student.isPresent());
                }

                // Save Real-Time Log to Firebase Cloud
                CloudRepository.saveAttendanceLog(attendanceId, selectedClass, selectedSubject, selectedDate, lectureNumber, lectureType, presentCount, studentList.size());

                Toast.makeText(this, "Attendance Saved! Log #" + attendanceId + " (" + presentCount + "/" + studentList.size() + " Present)", Toast.LENGTH_LONG).show();
                finish();
            });
        }
    }

    private void loadRosterForClass(String className) {
        CloudRepository.studentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                studentList.clear();
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
                        studentList.add(new StudentModel(String.valueOf(i + 1), cloudMatched.get(i).getName()));
                    }
                } else {
                    // Fallback default roster
                    String[] defaultNames = {"Aditya Thete", "Tejas Rajwade", "Gaurav Pawar", "Atharv Gaikwad", "Komal Jagtap", "Bhakti Samudre", "Kajal Jadhav", "Vaishnavi Jadhav"};
                    for (int i = 0; i < defaultNames.length; i++) {
                        studentList.add(new StudentModel(String.valueOf(i + 1), defaultNames[i]));
                    }
                }

                notifyAdapterChanged();
                if (tvRosterCount != null) {
                    tvRosterCount.setText(getString(R.string.student_roster_format, studentList.size()));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void notifyAdapterChanged() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private static class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {
        private final List<StudentModel> list;

        public StudentAdapter(List<StudentModel> list) {
            this.list = list;
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
            TextView tvStudentInfo;
            CheckBox cbAttendance;

            public StudentViewHolder(@NonNull View itemView) {
                super(itemView);
                tvStudentInfo = itemView.findViewById(R.id.tvStudentInfo);
                cbAttendance = itemView.findViewById(R.id.cbAttendance);
            }
        }
    }
}