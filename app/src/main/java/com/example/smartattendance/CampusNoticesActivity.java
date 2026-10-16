package com.example.smartattendance;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class CampusNoticesActivity extends AppCompatActivity {

    private LinearLayout containerNotices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campus_notices);

        TextView btnBack = findViewById(R.id.btnBack);
        MaterialButton btnClearNoticesHistory = findViewById(R.id.btnClearNoticesHistory);
        containerNotices = findViewById(R.id.containerNotices);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnClearNoticesHistory != null) {
            btnClearNoticesHistory.setOnClickListener(v -> {
                CloudRepository.clearAllAnnouncements();
                Toast.makeText(this, "Campus Notices History Cleared!", Toast.LENGTH_SHORT).show();
            });
        }

        listenCampusAnnouncements();
    }

    private void listenCampusAnnouncements() {
        CloudRepository.announcementsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (containerNotices == null) return;
                containerNotices.removeAllViews();

                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    TextView tvEmpty = new TextView(CampusNoticesActivity.this);
                    tvEmpty.setText("No campus announcements or broadcast notices posted.");
                    tvEmpty.setTextColor(0xFF64748B);
                    tvEmpty.setPadding(10, 20, 10, 20);
                    containerNotices.addView(tvEmpty);
                } else {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        String title = child.child("title").getValue(String.class);
                        String msg = child.child("message").getValue(String.class);
                        String postedBy = child.child("postedBy").getValue(String.class);
                        Long ts = child.child("timestamp").getValue(Long.class);

                        long timestamp = ts != null ? ts : 0L;
                        String timeStr = CloudRepository.formatTimestamp(timestamp);

                        MaterialCardView card = new MaterialCardView(CampusNoticesActivity.this);
                        card.setRadius(12);
                        card.setCardElevation(2);
                        card.setCardBackgroundColor(0xFFFEF3C7);
                        card.setStrokeColor(0xFFFDE68A);
                        card.setStrokeWidth(1);

                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        lp.setMargins(0, 0, 0, 16);
                        card.setLayoutParams(lp);

                        LinearLayout inner = new LinearLayout(CampusNoticesActivity.this);
                        inner.setOrientation(LinearLayout.VERTICAL);
                        inner.setPadding(20, 16, 20, 16);

                        TextView tvTitle = new TextView(CampusNoticesActivity.this);
                        tvTitle.setText("📌 " + (title != null ? title : "Notice") + (timeStr.isEmpty() ? "" : " [" + timeStr + "]"));
                        tvTitle.setTextSize(15);
                        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
                        tvTitle.setTextColor(0xFF92400E);

                        TextView tvMsg = new TextView(CampusNoticesActivity.this);
                        tvMsg.setText(msg);
                        tvMsg.setTextSize(13);
                        tvMsg.setTextColor(0xFF78350F);
                        tvMsg.setPadding(0, 6, 0, 0);

                        inner.addView(tvTitle);
                        inner.addView(tvMsg);
                        card.addView(inner);

                        containerNotices.addView(card, 0);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}