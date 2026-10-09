package com.example.smartattendance;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.Objects;

public class AttendanceData {

    private static final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("attendance");

    // Teacher Attendance mark kartana cloud var save karne
    public static void markAttendance(String studentName, String subject, boolean isPresent) {
        String safeName = studentName.replace(".", "_").replace(" ", "_");
        DatabaseReference subRef = dbRef.child(safeName).child(subject);

        subRef.get().addOnSuccessListener(snapshot -> {
            Long att = snapshot.exists() ? snapshot.child("attended").getValue(Long.class) : null;
            Long tot = snapshot.exists() ? snapshot.child("total").getValue(Long.class) : null;

            long oldAttended = Objects.requireNonNullElse(att, 0L);
            long oldTotal = Objects.requireNonNullElse(tot, 0L);

            long newTotal = oldTotal + 1;
            long newAttended = isPresent ? oldAttended + 1 : oldAttended;

            subRef.child("attended").setValue(newAttended);
            subRef.child("total").setValue(newTotal);
        });
    }

    // Student Dashboard sathi Real-Time Cloud Listener
    public static void listenStudentAttendance(String studentName, ValueEventListener listener) {
        String safeName = studentName.replace(".", "_").replace(" ", "_");
        dbRef.child(safeName).addValueEventListener(listener);
    }
}