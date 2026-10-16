package com.example.smartattendance;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class PostNoticeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_notice);

        TextView btnBack = findViewById(R.id.btnBack);
        EditText etNoticeTitle = findViewById(R.id.etNoticeTitle);
        EditText etNoticeMessage = findViewById(R.id.etNoticeMessage);
        MaterialButton btnPostNotice = findViewById(R.id.btnPostNotice);
        MaterialButton btnClearNotices = findViewById(R.id.btnClearNotices);

        String teacherName = getIntent().getStringExtra("TEACHER_NAME");
        if (teacherName == null || teacherName.isEmpty()) teacherName = "Faculty";

        final String finalTeacherName = teacherName;

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnPostNotice != null) {
            btnPostNotice.setOnClickListener(v -> {
                String title = etNoticeTitle.getText().toString().trim();
                String msg = etNoticeMessage.getText().toString().trim();

                if (!title.isEmpty() && !msg.isEmpty()) {
                    CloudRepository.postAnnouncement(title, msg, "Faculty Update", finalTeacherName);
                    Toast.makeText(this, "Faculty notice posted successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Please enter both Title and Message", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnClearNotices != null) {
            btnClearNotices.setOnClickListener(v -> {
                CloudRepository.clearAllAnnouncements();
                Toast.makeText(this, "Notice History Cleared!", Toast.LENGTH_SHORT).show();
            });
        }
    }
}