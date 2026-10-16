package com.example.smartattendance;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.Objects;

public class ChangePasswordUtil {

    public static void showChangePasswordDialog(Context context, String role, String userKey) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("🔑 Change Password (" + role + ")");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 20);

        final EditText etOldPass = new EditText(context);
        etOldPass.setHint("Enter Current Password");
        etOldPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etOldPass);

        final EditText etNewPass = new EditText(context);
        etNewPass.setHint("Enter New Password");
        etNewPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etNewPass);

        builder.setView(layout);

        builder.setPositiveButton("Update Password", (dialog, which) -> {
            String oldP = etOldPass.getText().toString().trim();
            String newP = etNewPass.getText().toString().trim();

            if (oldP.isEmpty() || newP.isEmpty()) {
                Toast.makeText(context, "Please enter both old and new passwords!", Toast.LENGTH_SHORT).show();
                return;
            }

            if ("ADMIN".equalsIgnoreCase(role)) {
                CloudRepository.adminRef.child(userKey).get().addOnSuccessListener(snapshot -> {
                    String actualPass = CloudRepository.getSafeString(snapshot, "password");
                    String finalPass = actualPass.isEmpty() && "10842931".equals(userKey) ? "siddharthbhambare" : actualPass;
                    if (Objects.equals(oldP, finalPass)) {
                        CloudRepository.adminRef.child(userKey).child("password").setValue(newP);
                        Toast.makeText(context, "Admin Password Updated Successfully! 🔑", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "🚨 Error: Current Password Does Not Match!", Toast.LENGTH_LONG).show();
                    }
                }).addOnFailureListener(e ->
                        Toast.makeText(context, "Database Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            } else if ("TEACHER".equalsIgnoreCase(role)) {
                CloudRepository.teacherRef.child(userKey).get().addOnSuccessListener(snapshot -> {
                    String actualPass = CloudRepository.getSafeString(snapshot, "password");
                    if (Objects.equals(oldP, actualPass)) {
                        CloudRepository.teacherRef.child(userKey).child("password").setValue(newP);
                        Toast.makeText(context, "Faculty Password Updated Successfully! 🔑", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "🚨 Error: Current Password Does Not Match!", Toast.LENGTH_LONG).show();
                    }
                }).addOnFailureListener(e ->
                        Toast.makeText(context, "Database Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            } else if ("STUDENT".equalsIgnoreCase(role)) {
                CloudRepository.studentRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            boolean userFound = false;
                            for (DataSnapshot ds : snapshot.getChildren()) {
                                String name = CloudRepository.getSafeString(ds, "name");
                                String roll = CloudRepository.getSafeString(ds, "roll");
                                String dbPass = CloudRepository.getSafeString(ds, "password");

                                boolean isMatch = (!name.isEmpty() && name.equalsIgnoreCase(userKey))
                                        || (!roll.isEmpty() && roll.equalsIgnoreCase(userKey));

                                if (isMatch) {
                                    userFound = true;
                                    String expectedAutoPass = name.toLowerCase().replaceAll("\\s+", "");
                                    boolean isOldPassValid = Objects.equals(oldP, dbPass)
                                            || Objects.equals(oldP.toLowerCase(), expectedAutoPass);

                                    if (isOldPassValid) {
                                        ds.getRef().child("password").setValue(newP);
                                        Toast.makeText(context, "Student Password Updated Successfully! 🔑", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(context, "🚨 Error: Current Password Does Not Match!", Toast.LENGTH_LONG).show();
                                    }
                                    break;
                                }
                            }
                            if (!userFound) {
                                Toast.makeText(context, "🚨 Error: Student account not found!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}