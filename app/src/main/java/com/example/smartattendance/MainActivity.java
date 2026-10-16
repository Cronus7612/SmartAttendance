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
import androidx.appcompat.app.AppCompatDelegate;
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
    private SessionManager sessionManager;

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
        // Initialize Session Manager & Apply Saved Theme Preference
        sessionManager = new SessionManager(this);
        AppCompatDelegate.setDefaultNightMode(sessionManager.getThemeMode());

        super.onCreate(savedInstanceState);

        // Check Auto-Login with Safe Recovery
        if (sessionManager.isLoggedIn()) {
            try {
                String role = sessionManager.getRole();
                if ("ADMIN".equalsIgnoreCase(role)) {
                    Intent intent = new Intent(this, AdminDashboardActivity.class);
                    intent.putExtra("ADMIN_NAME", sessionManager.getName());
                    intent.putExtra("ADMIN_ID", sessionManager.getIdOrRoll());
                    startActivity(intent);
                    finish();
                    return;
                } else if ("TEACHER".equalsIgnoreCase(role)) {
                    Intent intent = new Intent(this, TeacherDashboardActivity.class);
                    intent.putExtra("TEACHER_NAME", sessionManager.getName());
                    intent.putExtra("TEACHER_ID", sessionManager.getIdOrRoll());
                    startActivity(intent);
                    finish();
                    return;
                } else if ("STUDENT".equalsIgnoreCase(role)) {
                    Intent intent = new Intent(this, StudentDashboardActivity.class);
                    intent.putExtra("STUDENT_NAME", sessionManager.getName());
                    intent.putExtra("STUDENT_ROLL", sessionManager.getIdOrRoll());
                    intent.putExtra("STUDENT_CLASS", sessionManager.getClassName());
                    intent.putExtra("STUDENT_ID", sessionManager.getStudentId());
                    startActivity(intent);
                    finish();
                    return;
                } else if ("PARENT".equalsIgnoreCase(role)) {
                    Intent intent = new Intent(this, ParentDashboardActivity.class);
                    intent.putExtra("STUDENT_NAME", sessionManager.getName());
                    intent.putExtra("STUDENT_ROLL", sessionManager.getIdOrRoll());
                    startActivity(intent);
                    finish();
                    return;
                }
            } catch (Exception e) {
                sessionManager.logoutUser();
            }
        }

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
            tilInput.setHint("Enter Email / Phone / ID / Name");
            etEmail.setInputType(InputType.TYPE_CLASS_TEXT);
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
                Toast.makeText(this, "Please enter both Email/Phone/ID/Name and Password", Toast.LENGTH_SHORT).show();
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
                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String rawId = CloudRepository.getSafeString(ds, "id");
                        String id = rawId.isEmpty() ? Objects.requireNonNullElse(ds.getKey(), "") : rawId;
                        String name = CloudRepository.getSafeString(ds, "name");
                        String classes = CloudRepository.getSafeString(ds, "assignedClasses");
                        String subjects = CloudRepository.getSafeString(ds, "assignedSubjects");
                        String email = CloudRepository.getSafeString(ds, "email");
                        String phone = CloudRepository.getSafeString(ds, "phone");

                        if (!id.isEmpty() && !name.isEmpty()) {
                            registeredTeachers.add(new TeacherModel(id, name, classes, subjects, email, phone));
                        }
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
                sessionManager.createSession("ADMIN", result.name, result.idOrRoll, "TY BCA", "");
                Toast.makeText(MainActivity.this, "Welcome " + result.name + "!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, AdminDashboardActivity.class);
                intent.putExtra("ADMIN_NAME", result.name);
                intent.putExtra("ADMIN_ID", result.idOrRoll);
                startActivity(intent);
                finish();
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

        if ((cleanInput.equalsIgnoreCase("Siddharth Bhambare") || cleanInput.equalsIgnoreCase("10842931") || cleanInput.equalsIgnoreCase("siddharth@smartattendance.edu") || Objects.equals(cleanInput, "9876543210"))
                && Objects.equals(pass, "siddharthbhambare")) {
            return new AuthResult("Siddharth Bhambare", "10842931");
        }

        if (snapshot != null && snapshot.exists()) {
            for (DataSnapshot ds : snapshot.getChildren()) {
                String rawDbId = CloudRepository.getSafeString(ds, "id");
                String dbId = rawDbId.isEmpty() ? Objects.requireNonNullElse(ds.getKey(), "") : rawDbId;
                String dbName = CloudRepository.getSafeString(ds, "name");
                String dbPass = CloudRepository.getSafeString(ds, "password");
                String dbEmail = CloudRepository.getSafeString(ds, "email");
                String dbPhone = CloudRepository.getSafeString(ds, "phone");

                if (!dbId.isEmpty() && !dbName.isEmpty() && !dbPass.isEmpty() && Objects.equals(pass, dbPass)) {
                    boolean isMatch = cleanInput.equalsIgnoreCase(dbName)
                            || cleanInput.equalsIgnoreCase(dbId)
                            || (!dbEmail.isEmpty() && cleanInput.equalsIgnoreCase(dbEmail))
                            || (!dbPhone.isEmpty() && cleanInput.equalsIgnoreCase(dbPhone));

                    if (isMatch) {
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
                sessionManager.createSession("TEACHER", result.name, result.idOrRoll, "TY BCA", "");
                Toast.makeText(MainActivity.this, "Welcome " + result.name + "!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, TeacherDashboardActivity.class);
                intent.putExtra("TEACHER_NAME", result.name);
                intent.putExtra("TEACHER_ID", result.idOrRoll);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(MainActivity.this, "Invalid Teacher Credentials!", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e ->
                Toast.makeText(MainActivity.this, "Database Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    @Nullable
    private AuthResult matchTeacherCredentials(DataSnapshot snapshot, String inputVal, String pass) {
        if (inputVal == null || pass == null) return null;
        String cleanInput = inputVal.trim().replaceAll("^#", "").trim();
        String cleanPass = pass.trim();

        if (snapshot != null && snapshot.exists()) {
            for (DataSnapshot ds : snapshot.getChildren()) {
                String nodeKey = ds.getKey() != null ? ds.getKey().trim() : "";
                String rawTeacherDbId = CloudRepository.getSafeString(ds, "id");
                String dbId = rawTeacherDbId.isEmpty() ? nodeKey : rawTeacherDbId;

                String rawDbName = CloudRepository.getSafeString(ds, "name");
                String dbName = rawDbName.isEmpty() ? "Prof. " + dbId : rawDbName;

                String dbPass = CloudRepository.getSafeString(ds, "password");
                String dbEmail = CloudRepository.getSafeString(ds, "email");
                String dbPhone = CloudRepository.getSafeString(ds, "phone");

                boolean isPassMatch = cleanPass.equalsIgnoreCase(dbPass)
                        || dbPass.isEmpty()
                        || cleanPass.equalsIgnoreCase(nodeKey)
                        || cleanPass.equalsIgnoreCase(dbId);

                boolean isUserMatch = cleanInput.equalsIgnoreCase(dbId)
                        || cleanInput.equalsIgnoreCase(nodeKey)
                        || cleanInput.equalsIgnoreCase(dbName)
                        || (!dbEmail.isEmpty() && cleanInput.equalsIgnoreCase(dbEmail))
                        || (!dbPhone.isEmpty() && cleanInput.equalsIgnoreCase(dbPhone))
                        || (cleanInput.length() >= 3 && dbName.toLowerCase().contains(cleanInput.toLowerCase()));

                if (isUserMatch && isPassMatch) {
                    return new AuthResult(dbName, dbId);
                }
            }
        }

        // Hardcoded Fallback for Default Teacher "patil" / "20481923"
        if ((cleanInput.equalsIgnoreCase("20481923") || cleanInput.equalsIgnoreCase("patil") || cleanInput.equalsIgnoreCase("Prof. S. R. Patil"))
                && (cleanPass.equalsIgnoreCase("patil") || cleanPass.equalsIgnoreCase("20481923") || cleanPass.isEmpty())) {
            return new AuthResult("Prof. S. R. Patil", "20481923");
        }

        return null;
    }

    private void authenticateStudent(String inputVal, String pass) {
        CloudRepository.studentRef.get().addOnSuccessListener(snapshot -> {
            AuthResult result = matchStudentCredentials(snapshot, inputVal, pass);

            if (result != null) {
                sessionManager.createSession("STUDENT", result.name, result.idOrRoll, result.className, result.studentId);
                Toast.makeText(MainActivity.this, "Welcome " + result.name + "!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, StudentDashboardActivity.class);
                intent.putExtra("STUDENT_NAME", result.name);
                intent.putExtra("STUDENT_ROLL", result.idOrRoll);
                intent.putExtra("STUDENT_CLASS", result.className);
                intent.putExtra("STUDENT_ID", result.studentId);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(MainActivity.this, "Invalid Student Credentials!", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e ->
                Toast.makeText(MainActivity.this, "Database Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    @Nullable
    private AuthResult matchStudentCredentials(DataSnapshot snapshot, String inputVal, String pass) {
        String cleanInput = inputVal.trim().replaceAll("^#", "").trim();
        String inputPass = pass.trim();

        if (snapshot != null && snapshot.exists()) {
            for (DataSnapshot ds : snapshot.getChildren()) {
                String dbRoll = CloudRepository.getSafeString(ds, "roll");
                String dbName = CloudRepository.getSafeString(ds, "name");
                String dbPass = CloudRepository.getSafeString(ds, "password");
                String dbStudentId = CloudRepository.getSafeString(ds, "studentId");
                String dbClass = CloudRepository.getSafeString(ds, "className");
                String dbEmail = CloudRepository.getSafeString(ds, "email");
                String dbPhone = CloudRepository.getSafeString(ds, "phone");

                if (!dbRoll.isEmpty() && !dbName.isEmpty()) {
                    String expectedAutoPass = dbName.toLowerCase().replaceAll("\\s+", "");

                    boolean isPassValid = Objects.equals(inputPass, dbPass)
                            || Objects.equals(inputPass.toLowerCase(), expectedAutoPass);

                    boolean isUserValid = cleanInput.equalsIgnoreCase(dbRoll)
                            || cleanInput.equalsIgnoreCase(dbName)
                            || (!dbStudentId.isEmpty() && cleanInput.equalsIgnoreCase(dbStudentId))
                            || (!dbEmail.isEmpty() && cleanInput.equalsIgnoreCase(dbEmail))
                            || (!dbPhone.isEmpty() && cleanInput.equalsIgnoreCase(dbPhone));

                    if (isUserValid && isPassValid) {
                        String finalClass = !dbClass.isEmpty() ? dbClass : "TY BCA";
                        return new AuthResult(dbName, dbRoll, finalClass, dbStudentId);
                    }
                }
            }
        }
        return null;
    }
}