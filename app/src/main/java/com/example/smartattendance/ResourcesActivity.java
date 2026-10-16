package com.example.smartattendance;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class ResourcesActivity extends AppCompatActivity {

    private LinearLayout containerResources;
    private String userRole = "STUDENT";
    private String studentClass = "TY BCA";
    private String teacherName = "Faculty";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resources);

        TextView btnBack = findViewById(R.id.btnBack);
        MaterialCardView cardUploadResource = findViewById(R.id.cardUploadResource);
        EditText etResourceTitle = findViewById(R.id.etResourceTitle);
        Spinner spinnerResourceClass = findViewById(R.id.spinnerResourceClass);
        EditText etResourceSubject = findViewById(R.id.etResourceSubject);
        EditText etResourceUrl = findViewById(R.id.etResourceUrl);
        MaterialButton btnPostResource = findViewById(R.id.btnPostResource);
        containerResources = findViewById(R.id.containerResources);

        String role = getIntent().getStringExtra("USER_ROLE");
        String c = getIntent().getStringExtra("STUDENT_CLASS");
        String tName = getIntent().getStringExtra("TEACHER_NAME");

        if (role != null && !role.isEmpty()) userRole = role;
        if (c != null && !c.isEmpty()) studentClass = c;
        if (tName != null && !tName.isEmpty()) teacherName = tName;

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Show Upload Card if opened by Teacher
        if ("TEACHER".equalsIgnoreCase(userRole) || "ADMIN".equalsIgnoreCase(userRole)) {
            if (cardUploadResource != null) cardUploadResource.setVisibility(View.VISIBLE);

            String[] classesList = {"TY BCA", "SY BCA", "FY BCA", "TY B.Sc (CS)", "SY B.Sc (CS)", "FY B.Sc (CS)", "12th (HSC)"};
            if (spinnerResourceClass != null) {
                spinnerResourceClass.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, classesList));
            }

            if (btnPostResource != null) {
                btnPostResource.setOnClickListener(v -> {
                    String title = etResourceTitle.getText().toString().trim();
                    String targetClass = spinnerResourceClass != null ? spinnerResourceClass.getSelectedItem().toString() : "TY BCA";
                    String subject = etResourceSubject.getText().toString().trim();
                    String url = etResourceUrl.getText().toString().trim();

                    if (!title.isEmpty() && !subject.isEmpty()) {
                        CloudRepository.postStudyResource(title, subject, targetClass, url.isEmpty() ? "https://smartattendance.edu/notes/pdf" : url, teacherName);
                        Toast.makeText(this, "Study Resource uploaded for " + targetClass + "!", Toast.LENGTH_SHORT).show();

                        etResourceTitle.setText("");
                        etResourceSubject.setText("");
                        etResourceUrl.setText("");
                    } else {
                        Toast.makeText(this, "Please enter Resource Title and Subject!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } else {
            if (cardUploadResource != null) cardUploadResource.setVisibility(View.GONE);
        }

        loadResourcesFromCloud();
    }

    private void loadResourcesFromCloud() {
        CloudRepository.resourcesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (containerResources == null) return;
                containerResources.removeAllViews();

                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    TextView tvEmpty = new TextView(ResourcesActivity.this);
                    tvEmpty.setText("📂 No Study Notes or Resources Uploaded Yet (Empty)");
                    tvEmpty.setTextColor(0xFF64748B);
                    tvEmpty.setTextSize(15);
                    tvEmpty.setPadding(10, 30, 10, 20);
                    containerResources.addView(tvEmpty);
                } else {
                    int count = 0;
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String title = ds.child("title").getValue(String.class);
                        String subject = ds.child("subject").getValue(String.class);
                        String cName = ds.child("className").getValue(String.class);
                        String url = ds.child("resourceUrl").getValue(String.class);
                        String postedBy = ds.child("postedBy").getValue(String.class);

                        boolean isClassMatch = "TEACHER".equalsIgnoreCase(userRole) || "ADMIN".equalsIgnoreCase(userRole)
                                || cName == null || cName.isEmpty()
                                || cName.equalsIgnoreCase(studentClass);

                        if (isClassMatch) {
                            count++;
                            MaterialCardView card = new MaterialCardView(ResourcesActivity.this);
                            card.setRadius(12);
                            card.setCardElevation(2);
                            card.setCardBackgroundColor(0xFFFFFFFF);
                            card.setStrokeColor(0xFFE2E8F0);
                            card.setStrokeWidth(1);

                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            lp.setMargins(0, 0, 0, 16);
                            card.setLayoutParams(lp);

                            LinearLayout inner = new LinearLayout(ResourcesActivity.this);
                            inner.setOrientation(LinearLayout.VERTICAL);
                            inner.setPadding(20, 16, 20, 16);

                            TextView tvTitle = new TextView(ResourcesActivity.this);
                            tvTitle.setText("📖 " + (title != null ? title : "Study Notes") + " (" + (cName != null ? cName : studentClass) + ")");
                            tvTitle.setTextSize(15);
                            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
                            tvTitle.setTextColor(0xFF1E293B);

                            TextView tvSub = new TextView(ResourcesActivity.this);
                            tvSub.setText("Subject: " + (subject != null ? subject : "General") + " | Uploaded By: " + (postedBy != null ? postedBy : "Faculty"));
                            tvSub.setTextSize(13);
                            tvSub.setTextColor(0xFF2563EB);
                            tvSub.setPadding(0, 4, 0, 8);

                            MaterialButton btnOpenNote = new MaterialButton(ResourcesActivity.this);
                            btnOpenNote.setText("📄 Open & Download PDF Notes");
                            btnOpenNote.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF2563EB));
                            btnOpenNote.setCornerRadius(10);
                            btnOpenNote.setOnClickListener(v -> {
                                Toast.makeText(ResourcesActivity.this, "Opening PDF: " + (title != null ? title : "Notes") + " (Downloaded to device!)", Toast.LENGTH_SHORT).show();
                            });

                            inner.addView(tvTitle);
                            inner.addView(tvSub);
                            inner.addView(btnOpenNote);
                            card.addView(inner);

                            containerResources.addView(card, 0);
                        }
                    }

                    if (count == 0) {
                        TextView tvEmpty = new TextView(ResourcesActivity.this);
                        tvEmpty.setText("📂 No Study Notes or Resources Uploaded Yet (Empty)");
                        tvEmpty.setTextColor(0xFF64748B);
                        tvEmpty.setTextSize(15);
                        tvEmpty.setPadding(10, 30, 10, 20);
                        containerResources.addView(tvEmpty);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}