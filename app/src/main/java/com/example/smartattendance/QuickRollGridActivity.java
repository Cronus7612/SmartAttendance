package com.example.smartattendance;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

public class QuickRollGridActivity extends AppCompatActivity {

    private String selectedClass = "TY BCA";
    private String selectedSubject = "Core Java";
    private final List<StudentModel> studentList = new ArrayList<>();
    private RollGridAdapter gridAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_roll_grid);

        TextView tvGridClassSubject = findViewById(R.id.tvGridClassSubject);
        GridView rollGridView = findViewById(R.id.rollGridView);
        MaterialButton btnSubmitGridAttendance = findViewById(R.id.btnSubmitGridAttendance);
        MaterialButton btnSelectAll = findViewById(R.id.btnSelectAll);
        MaterialButton btnDeselectAll = findViewById(R.id.btnDeselectAll);

        String c = getIntent().getStringExtra("SELECTED_CLASS");
        String s = getIntent().getStringExtra("SELECTED_SUBJECT");
        if (c != null && !c.isEmpty()) selectedClass = c;
        if (s != null && !s.isEmpty()) selectedSubject = s;

        if (tvGridClassSubject != null) {
            tvGridClassSubject.setText(getString(R.string.class_subject_format, selectedClass, selectedSubject));
        }

        gridAdapter = new RollGridAdapter(studentList);
        if (rollGridView != null) {
            rollGridView.setAdapter(gridAdapter);
        }

        // 1. Select All / Mark All Present
        if (btnSelectAll != null) {
            btnSelectAll.setOnClickListener(v -> {
                for (StudentModel student : studentList) {
                    student.setPresent(true);
                }
                if (gridAdapter != null) {
                    gridAdapter.notifyDataSetChanged();
                }
                Toast.makeText(this, "Marked All as Present", Toast.LENGTH_SHORT).show();
            });
        }

        // 2. Deselect All / Mark All Absent
        if (btnDeselectAll != null) {
            btnDeselectAll.setOnClickListener(v -> {
                for (StudentModel student : studentList) {
                    student.setPresent(false);
                }
                if (gridAdapter != null) {
                    gridAdapter.notifyDataSetChanged();
                }
                Toast.makeText(this, "Marked All as Absent", Toast.LENGTH_SHORT).show();
            });
        }

        loadStudentsFromCloud();

        // 3. Submit Attendance
        if (btnSubmitGridAttendance != null) {
            btnSubmitGridAttendance.setOnClickListener(v -> {
                if (studentList.isEmpty()) {
                    Toast.makeText(this, "No students in grid to submit!", Toast.LENGTH_SHORT).show();
                    return;
                }

                int presentCount = 0;
                for (StudentModel student : studentList) {
                    if (student.isPresent()) presentCount++;
                    AttendanceData.markAttendance(student.getName(), selectedSubject, student.isPresent());
                }

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String todayStr = sdf.format(new Date());

                Random random = new Random();
                String attendanceId = String.valueOf(2600 + random.nextInt(900));

                CloudRepository.saveAttendanceLog(attendanceId, selectedClass, selectedSubject, todayStr, "Grid Lecture", "Regular", presentCount, studentList.size());

                Toast.makeText(this, "Grid Attendance submitted for " + selectedClass + " (" + selectedSubject + ")!", Toast.LENGTH_LONG).show();
                finish();
            });
        }
    }

    private void loadStudentsFromCloud() {
        CloudRepository.studentRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                studentList.clear();
                List<StudentModel> cloudMatched = new ArrayList<>();

                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String name = ds.child("name").getValue(String.class);
                        String cName = ds.child("className").getValue(String.class);

                        if (name != null) {
                            if (cName != null && cName.equalsIgnoreCase(selectedClass)) {
                                cloudMatched.add(new StudentModel("0", name));
                            } else if (cName == null && Objects.equals(selectedClass, "TY BCA")) {
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
                    loadDefaultRosterForGrid(selectedClass);
                }

                if (gridAdapter != null) {
                    gridAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loadDefaultRosterForGrid(selectedClass);
                if (gridAdapter != null) {
                    gridAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    private void loadDefaultRosterForGrid(String className) {
        studentList.clear();

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
                    studentList.add(new StudentModel(String.valueOf(i + 1), tyBcaNames.get(i)));
                }
                break;
            case "SY BCA":
                String[] syBcaNames = {"Abhishek More", "Aniket Shinde", "Chetan Patil", "Deepak Thorat", "Harshada Deshmukh", "Jayesh Kadam", "Manish Wagh", "Mayuri Gite", "Nilesh Chavan", "Pooja Bhamre", "Prasad Joshi", "Rohit Sonawane"};
                for (int i = 0; i < syBcaNames.length; i++) {
                    studentList.add(new StudentModel(String.valueOf(i + 1), syBcaNames[i]));
                }
                break;
            case "FY BCA":
                String[] fyBcaNames = {"Akash Pawar", "Bhavana Kale", "Dhananjay Jagtap", "Ishwar Ghadge", "Kunal Desale", "Madhuri Kulkarni", "Pratiksha Gaikwad", "Rakesh Shinde", "Sagar Patil", "Snehal Jadhav", "Tushar More", "Yogesh Thorat"};
                for (int i = 0; i < fyBcaNames.length; i++) {
                    studentList.add(new StudentModel(String.valueOf(i + 1), fyBcaNames[i]));
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
                    String fName = (i % 2 == 0) ? girlFirstNames[Math.abs((seedIdx + 3) % girlFirstNames.length)] : boyFirstNames[Math.abs((seedIdx + 5) % boyFirstNames.length)];
                    studentList.add(new StudentModel(String.valueOf(i), fName + " " + lName));
                }
                break;
        }
    }

    private static class RollGridAdapter extends BaseAdapter {
        private final List<StudentModel> list;

        public RollGridAdapter(List<StudentModel> list) {
            this.list = list;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView btn;
            if (convertView == null) {
                btn = new TextView(parent.getContext());
                btn.setLayoutParams(new GridView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 110));
                btn.setGravity(Gravity.CENTER);
                btn.setTextSize(14);
                btn.setPadding(6, 6, 6, 6);
                btn.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                btn = (TextView) convertView;
            }

            StudentModel student = list.get(position);
            btn.setText(holderText(student));

            if (student.isPresent()) {
                btn.setBackgroundColor(Color.parseColor("#16A34A"));
                btn.setTextColor(Color.WHITE);
            } else {
                btn.setBackgroundColor(Color.parseColor("#EF4444"));
                btn.setTextColor(Color.WHITE);
            }

            btn.setOnClickListener(v -> {
                student.setPresent(!student.isPresent());
                notifyDataSetChanged();
            });

            return btn;
        }

        private String holderText(StudentModel s) {
            return "Roll #" + s.getRollNo();
        }
    }
}