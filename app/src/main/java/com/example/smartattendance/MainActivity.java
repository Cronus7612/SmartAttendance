package com.example.smartattendance;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    // Local cached list for quick access
    public static final List<TeacherModel> registeredTeachers = new ArrayList<>();

    private static class AuthResult {
        final String name;
        final String idOrRoll;
        final String className;
        final String studentId;

        AuthResult(String name, String idOrRoll, String className, String studentId) {
            this.name = name;
            this.idOrRoll = idOrRoll;
            this.className = className;
            this.studentId = studentId;
        }

        AuthResult(String name, String idOrRoll) {
            this(name, idOrRoll, "TY BCA", null);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize default cloud data if database is empty
        CloudRepository.initializeDefaultsIfEmpty();

        // Bind UI Views
        RadioGroup roleRadioGroup = findViewById(R.id.roleRadioGroup);
        RadioButton radioAdmin = findViewById(R.id.radioAdmin);
        RadioButton radioTeacher = findViewById(R.id.radioTeacher);
        RadioButton radioStudent = findViewById(R.id.radioStudent);
        TextInputLayout tilInput = findViewById(R.id.tilInput);
        TextInputEditText etEmail = findViewById(R.id.etEmail);
        TextInputEditText etPassword = findViewById(R.id.etPassword);
        MaterialButton btnLogin = findViewById(R.id.btnLogin);

        // Keep local teachers list synchronized with Firebase
        syncTeachersFromCloud();

        // Dynamic Role Hint Toggle
        roleRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            etEmail.setText("");
            etPassword.setText("");
            if (checkedId == R.id.radioAdmin) {
                tilInput.setHint("Enter Name or Admin ID (#10842931)");
                etEmail.setInputType(InputType.TYPE_CLASS_TEXT);
            } else if (checkedId == R.id.radioTeacher) {
                tilInput.setHint("Enter Name or Teacher ID (#20481923)");
                etEmail.setInputType(InputType.TYPE_CLASS_TEXT);
            } else if (checkedId == R.id.radioStudent) {
                tilInput.setHint(getString(R.string.student_login_hint));
                etEmail.setInputType(InputType.TYPE_CLASS_TEXT);
            }
        });

        // Soft Keyboard Enter / Done key listener on Password field
        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                btnLogin.performClick();
                return true;
            }
            return false;
        });

        // Cloud-Authenticated Login Action
        btnLogin.setOnClickListener(v -> {
            String inputVal = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String pass = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

            if (inputVal.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please enter both ID/Name and Password", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1. ADMIN CLOUD LOGIN
            if (radioAdmin.isChecked()) {
                authenticateAdmin(inputVal, pass);

                // 2. TEACHER CLOUD LOGIN
            } else if (radioTeacher.isChecked()) {
                authenticateTeacher(inputVal, pass);

                // 3. STUDENT CLOUD LOGIN
            } else if (radioStudent.isChecked()) {
                authenticateStudent(inputVal, pass);
            }
        });
    }

    private void syncTeachersFromCloud() {
        CloudRepository.teacherRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                registeredTeachers.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String id = ds.child("id").getValue(String.class);
                    String name = ds.child("name").getValue(String.class);
                    if (id != null && name != null) {
                        registeredTeachers.add(new TeacherModel(id, name));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void authenticateAdmin(String inputVal, String pass) {
        CloudRepository.adminRef.get().addOnSuccessListener(snapshot -> {
            AuthResult result = matchAdminCredentials(snapshot, inputVal, pass);

            if (result != null) {
                Toast.makeText(MainActivity.this, "Welcome " + result.name + "!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, AdminDashboardActivity.class);
                intent.putExtra("ADMIN_NAME", result.name);
                intent.putExtra("ADMIN_ID", result.idOrRoll);
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "Invalid Admin Credentials!", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e ->
                Toast.makeText(MainActivity.this, "Database Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    @Nullable
    private AuthResult matchAdminCredentials(DataSnapshot snapshot, String inputVal, String pass) {
        String cleanInput = inputVal.trim().replaceAll("^#", "").trim();

        if ((cleanInput.equalsIgnoreCase("Siddharth Bhambare") || cleanInput.equalsIgnoreCase("10842931"))
                && Objects.equals(pass, "siddharthbhambare")) {
            return new AuthResult("Siddharth Bhambare", "10842931");
        }

        if (snapshot.exists()) {
            for (DataSnapshot ds : snapshot.getChildren()) {
                String dbId = ds.child("id").getValue(String.class);
                String dbName = ds.child("name").getValue(String.class);
                String dbPass = ds.child("password").getValue(String.class);

                if (dbId != null && dbName != null && Objects.equals(pass, dbPass)) {
                    if (cleanInput.equalsIgnoreCase(dbName) || cleanInput.equalsIgnoreCase(dbId)) {
                        return new AuthResult(dbName, dbId);
                    }
                }
            }
        }
        return null;
    }

    private void authenticateTeacher(String inputVal, String pass) {
        CloudRepository.teacherRef.get().addOnSuccessListener(snapshot -> {
            AuthResult result = matchTeacherCredentials(snapshot, inputVal, pass);

            if (result != null) {
                Toast.makeText(MainActivity.this, "Welcome " + result.name + "!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, TeacherDashboardActivity.class);
                intent.putExtra("TEACHER_NAME", result.name);
                intent.putExtra("TEACHER_ID", result.idOrRoll);
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "Invalid Teacher Credentials!", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e ->
                Toast.makeText(MainActivity.this, "Database Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    @Nullable
    private AuthResult matchTeacherCredentials(DataSnapshot snapshot, String inputVal, String pass) {
        String cleanInput = inputVal.trim().replaceAll("^#", "").trim();

        if (snapshot.exists()) {
            for (DataSnapshot ds : snapshot.getChildren()) {
                String dbId = ds.child("id").getValue(String.class);
                String dbName = ds.child("name").getValue(String.class);
                String dbPass = ds.child("password").getValue(String.class);

                if (dbId != null && dbName != null && Objects.equals(pass, dbPass)) {
                    if (cleanInput.equalsIgnoreCase(dbId) || cleanInput.equalsIgnoreCase(dbName)) {
                        return new AuthResult(dbName, dbId);
                    }
                }
            }
        }
        return null;
    }

    private void authenticateStudent(String inputVal, String pass) {
        CloudRepository.studentRef.get().addOnSuccessListener(snapshot -> {
            AuthResult result = matchStudentCredentials(snapshot, inputVal, pass);

            if (result != null) {
                Toast.makeText(MainActivity.this, "Welcome " + result.name + "!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, StudentDashboardActivity.class);
                intent.putExtra("STUDENT_NAME", result.name);
                intent.putExtra("STUDENT_ROLL", result.idOrRoll);
                intent.putExtra("STUDENT_CLASS", result.className);
                intent.putExtra("STUDENT_ID", result.studentId);
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "Invalid Student Roll/ID/Name or Password!", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e ->
                Toast.makeText(MainActivity.this, "Database Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    @Nullable
    private AuthResult matchStudentCredentials(DataSnapshot snapshot, String inputVal, String pass) {
        String cleanInput = inputVal.trim().replaceAll("^#", "").trim();
        String inputPass = pass.trim();

        if (snapshot.exists()) {
            for (DataSnapshot ds : snapshot.getChildren()) {
                String dbRoll = ds.child("roll").getValue(String.class);
                String dbName = ds.child("name").getValue(String.class);
                String dbPass = ds.child("password").getValue(String.class);
                String dbStudentId = ds.child("studentId").getValue(String.class);
                String dbClass = ds.child("className").getValue(String.class);

                if (dbRoll != null && dbName != null) {
                    String expectedAutoPass = dbName.toLowerCase().replaceAll("\\s+", "");

                    boolean isPassValid = Objects.equals(inputPass, dbPass)
                            || Objects.equals(inputPass.toLowerCase(), expectedAutoPass);

                    boolean isUserValid = cleanInput.equalsIgnoreCase(dbRoll)
                            || cleanInput.equalsIgnoreCase(dbName)
                            || (dbStudentId != null && cleanInput.equalsIgnoreCase(dbStudentId));

                    if (isUserValid && isPassValid) {
                        String finalClass = dbClass != null ? dbClass : "TY BCA";
                        return new AuthResult(dbName, dbRoll, finalClass, dbStudentId);
                    }
                }
            }
        }
        return null;
    }
}