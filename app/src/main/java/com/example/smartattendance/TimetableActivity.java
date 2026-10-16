package com.example.smartattendance;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

public class TimetableActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable);

        TextView btnBack = findViewById(R.id.btnBack);
        TextView tvTimetableTitle = findViewById(R.id.tvTimetableTitle);
        MaterialCardView cardClassSelector = findViewById(R.id.cardClassSelector);
        Spinner spinnerTimetableClass = findViewById(R.id.spinnerTimetableClass);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        String studentClass = getIntent().getStringExtra("STUDENT_CLASS");
        String userRole = getIntent().getStringExtra("USER_ROLE");
        String teacherClasses = getIntent().getStringExtra("ASSIGNED_CLASSES");

        if (studentClass == null || studentClass.trim().isEmpty()) {
            studentClass = "TY BCA";
        }

        if ("STUDENT".equalsIgnoreCase(userRole)) {
            if (cardClassSelector != null) cardClassSelector.setVisibility(View.GONE);
            if (tvTimetableTitle != null) tvTimetableTitle.setText(studentClass + " Timetable");
        } else {
            if (cardClassSelector != null) cardClassSelector.setVisibility(View.VISIBLE);
            if (tvTimetableTitle != null) tvTimetableTitle.setText("Class Timetable: " + studentClass);
        }

        String[] classesList = {
                "TY BCA", "SY BCA", "FY BCA",
                "TY B.Sc (CS)", "SY B.Sc (CS)", "FY B.Sc (CS)",
                "TY BBA", "SY BBA", "FY BBA",
                "TY B.Com", "SY B.Com", "FY B.Com",
                "12th (HSC)"
        };

        if ("TEACHER".equalsIgnoreCase(userRole) && teacherClasses != null && !teacherClasses.trim().isEmpty()) {
            String[] assigned = teacherClasses.split("\\s*,\\s*");
            if (assigned.length > 0 && !assigned[0].trim().isEmpty()) {
                classesList = assigned;
            }
        }

        final String[] finalClassesList = classesList;

        if (spinnerTimetableClass != null) {
            ArrayAdapter<String> classAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, finalClassesList);
            spinnerTimetableClass.setAdapter(classAdapter);

            spinnerTimetableClass.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position >= 0 && tvTimetableTitle != null) {
                        tvTimetableTitle.setText(finalClassesList[position] + " Timetable");
                        Toast.makeText(TimetableActivity.this, "Loaded Timetable for " + finalClassesList[position] + "!", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }
    }
}