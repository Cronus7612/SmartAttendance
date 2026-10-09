package com.example.smartattendance;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CloudRepository {

    private static final DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
    public static final DatabaseReference adminRef = rootRef.child("admins");
    public static final DatabaseReference teacherRef = rootRef.child("teachers");
    public static final DatabaseReference studentRef = rootRef.child("students");
    public static final DatabaseReference attendanceRef = rootRef.child("attendance");
    public static final DatabaseReference announcementsRef = rootRef.child("announcements");
    public static final DatabaseReference leaveRef = rootRef.child("leave_applications");

    // Title Case Helper: "amit shah" -> "Amit Shah"
    public static String formatToTitleCase(String input) {
        if (input == null || input.trim().isEmpty()) return "";
        String[] words = input.trim().toLowerCase().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0)))
                        .append(w.substring(1))
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }

    public static String generate8DigitStudentId(String roll, String name) {
        int seed = Math.abs((roll + name).hashCode());
        return String.valueOf(39000000 + (seed % 9000000));
    }

    public static String formatTimestamp(long timestamp) {
        if (timestamp <= 0) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public static void postAnnouncement(String title, String message, String category, String postedBy) {
        String id = announcementsRef.push().getKey();
        if (id != null) {
            AnnouncementModel model = new AnnouncementModel(id, title, message, category, postedBy, System.currentTimeMillis());
            announcementsRef.child(id).setValue(model);
        }
    }

    public static void clearAllAnnouncements() {
        announcementsRef.removeValue();
    }

    public static void submitLeaveApplication(String applicantName, String applicantRollOrId, String leaveType, String reason) {
        String leaveId = leaveRef.push().getKey();
        if (leaveId != null) {
            LeaveApplicationModel model = new LeaveApplicationModel(leaveId, applicantName, applicantRollOrId, leaveType, reason, "PENDING", System.currentTimeMillis());
            leaveRef.child(leaveId).setValue(model);
        }
    }

    public static void updateLeaveStatus(String leaveId, String newStatus, String applicantName, String reason) {
        long currentActionTime = System.currentTimeMillis();
        leaveRef.child(leaveId).child("status").setValue(newStatus);
        leaveRef.child(leaveId).child("actionTimestamp").setValue(currentActionTime);

        String badge = "APPROVED 🟢";
        if ("REJECTED".equalsIgnoreCase(newStatus)) badge = "REJECTED 🔴";

        String title = "📝 Leave Application " + newStatus;
        String msg = "Leave request for " + applicantName + " (" + reason + ") was " + badge + " by HOD/Admin.";

        postAnnouncement(title, msg, "Leave Status", "Admin / HOD");
    }

    public static void updateLeaveStatus(String leaveId, String newStatus) {
        updateLeaveStatus(leaveId, newStatus, "Student", "Medical/Duty");
    }

    public static void initializeDefaultsIfEmpty() {
        adminRef.child("10842931").get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                saveAdmin("10842931", "Siddharth Bhambare", "siddharthbhambare");
            }
        });

        teacherRef.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists() || snapshot.getChildrenCount() < 27) {
                uploadAll27Teachers();
            }
        });

        studentRef.get().addOnSuccessListener(snapshot -> {
            boolean needsReseed = !snapshot.exists() || snapshot.getChildrenCount() == 0;
            if (snapshot.exists()) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String sId = ds.child("studentId").getValue(String.class);
                    if (sId == null || sId.length() != 8) {
                        needsReseed = true;
                        break;
                    }
                }
            }
            if (needsReseed) {
                seedAllDefaultStudentsToCloud();
            }
        });
    }

    private static void seedAllDefaultStudentsToCloud() {
        List<TempStudent> initialList = new ArrayList<>();

        // TY BCA
        String[] tyBca = {"Aditya Thete", "Tejas Rajwade", "Gaurav Pawar", "Atharv Gaikwad", "Komal Jagtap", "Bhakti Samudre", "Kajal Jadhav", "Vaishnavi Jadhav"};
        for (String s : tyBca) {
            initialList.add(new TempStudent(s, s.toLowerCase().replaceAll("\\s+", ""), "TY BCA"));
        }

        // SY BCA
        String[] syBca = {"Abhishek More", "Aniket Shinde", "Chetan Patil", "Deepak Thorat", "Harshada Deshmukh", "Jayesh Kadam", "Manish Wagh", "Mayuri Gite", "Nilesh Chavan", "Pooja Bhamre", "Prasad Joshi", "Rohit Sonawane"};
        for (String s : syBca) {
            initialList.add(new TempStudent(s, s.toLowerCase().replaceAll("\\s+", ""), "SY BCA"));
        }

        // FY BCA
        String[] fyBca = {"Akash Pawar", "Bhavana Kale", "Dhananjay Jagtap", "Ishwar Ghadge", "Kunal Desale", "Madhuri Kulkarni", "Pratiksha Gaikwad", "Rakesh Shinde", "Sagar Patil", "Snehal Jadhav", "Tushar More", "Yogesh Thorat"};
        for (String s : fyBca) {
            initialList.add(new TempStudent(s, s.toLowerCase().replaceAll("\\s+", ""), "FY BCA"));
        }

        // Other classes default rosters
        String[] otherClasses = {"TY B.Sc (CS)", "SY B.Sc (CS)", "FY B.Sc (CS)", "TY B.Sc", "SY B.Sc", "FY B.Sc", "TY BBA", "SY BBA", "FY BBA", "TY B.Com", "SY B.Com", "FY B.Com", "12th (HSC)", "11th (FYJC)"};
        String[] boyFirst = {"Aarav", "Rohan", "Siddharth", "Omkar", "Kunal", "Pranav", "Abhishek", "Harshal", "Sanket", "Mayur", "Rushikesh", "Swapnil"};
        String[] girlFirst = {"Ananya", "Sakshi", "Pooja", "Neha", "Sayali", "Snehal", "Shruti", "Tanvi", "Rutuja", "Pranali", "Aishwarya", "Shweta"};
        String[] lastNames = {"Patil", "Shinde", "Deshmukh", "More", "Kulkarni", "Joshi", "Gite", "Gaikwad", "Kale", "Wagh", "Sonawane", "Thorat"};

        for (String cName : otherClasses) {
            int baseSeed = Math.abs(cName.hashCode());
            for (int i = 1; i <= 10; i++) {
                int seedIdx = (baseSeed + (i * 7));
                String lName = lastNames[Math.abs((seedIdx + i) % lastNames.length)];
                String fName = (i % 2 == 0) ? girlFirst[Math.abs((seedIdx + 3) % girlFirst.length)] : boyFirst[Math.abs((seedIdx + 5) % boyFirst.length)];
                String full = fName + " " + lName;
                initialList.add(new TempStudent(full, full.toLowerCase().replaceAll("\\s+", ""), cName));
            }
        }

        // Group by className and assign class-level roll numbers 1, 2, 3...
        Map<String, List<TempStudent>> classMap = new HashMap<>();
        for (TempStudent ts : initialList) {
            classMap.computeIfAbsent(ts.className, k -> new ArrayList<>()).add(ts);
        }

        Map<String, Object> dbMap = new HashMap<>();
        int globalCounter = 1;
        for (Map.Entry<String, List<TempStudent>> entry : classMap.entrySet()) {
            List<TempStudent> cList = entry.getValue();
            cList.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
            for (int i = 0; i < cList.size(); i++) {
                String classRoll = String.valueOf(i + 1);
                String sId = generate8DigitStudentId(classRoll, cList.get(i).name);
                Map<String, Object> sMap = new HashMap<>();
                sMap.put("roll", classRoll);
                sMap.put("name", cList.get(i).name);
                sMap.put("password", cList.get(i).password);
                sMap.put("className", entry.getKey());
                sMap.put("studentId", sId);
                dbMap.put(String.valueOf(globalCounter++), sMap);
            }
        }

        studentRef.setValue(dbMap);
    }

    private static void uploadAll27Teachers() {
        saveTeacher("20481923", "Prof. S. R. Patil", "patil");
        saveTeacher("59182341", "Prof. V. M. Shinde", "shinde");
        saveTeacher("83920145", "Prof. A. K. Deshmukh", "deshmukh");
        saveTeacher("11245890", "Prof. R. B. More", "more");
        saveTeacher("78451296", "Prof. N. S. Kulkarni", "kulkarni");
        saveTeacher("63214587", "Prof. P. D. Joshi", "joshi");
        saveTeacher("95123648", "Prof. K. T. Gite", "gite");
        saveTeacher("45871236", "Prof. M. L. Gaikwad", "gaikwad");
        saveTeacher("32145698", "Prof. S. P. Kale", "kale");
        saveTeacher("85214796", "Prof. H. R. Wagh", "wagh");
        saveTeacher("96325874", "Prof. C. V. Sonawane", "sonawane");
        saveTeacher("74125896", "Prof. D. S. Thorat", "thorat");
        saveTeacher("36985214", "Prof. J. K. Ghadge", "ghadge");
        saveTeacher("15975348", "Prof. B. M. Jadhav", "jadhav");
        saveTeacher("75395146", "Prof. T. R. Chavan", "chavan");
        saveTeacher("85296314", "Prof. U. S. Jagtap", "jagtap");
        saveTeacher("95135748", "Prof. W. P. Bhamre", "bhamre");
        saveTeacher("35715948", "Prof. Y. K. Pawar", "pawar");
        saveTeacher("45612378", "Prof. Z. N. Kadam", "kadam");
        saveTeacher("78945612", "Prof. E. T. Borse", "borse");
        saveTeacher("12378945", "Prof. F. R. Salunkhe", "salunkhe");
        saveTeacher("98712345", "Prof. G. H. Mane", "mane");
        saveTeacher("65498712", "Prof. I. J. Nikam", "nikam");
        saveTeacher("32165498", "Prof. L. M. Tambe", "tambe");
        saveTeacher("85274196", "Prof. O. P. Shelar", "shelar");
        saveTeacher("96385274", "Prof. Q. R. Khairnar", "khairnar");
        saveTeacher("74185296", "Prof. X. Y. Thete", "thete");
    }

    public static void saveAdmin(String id, String name, String password) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("password", password);
        adminRef.child(id).setValue(map);
    }

    public static void saveTeacher(String id, String name, String password) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("password", password);
        teacherRef.child(id).setValue(map);
    }

    public static void removeTeacher(String id) {
        teacherRef.child(id).removeValue();
    }

    // Dynamic Student Object for sorting
    private static class TempStudent {
        String name;
        String password;
        String className;
        TempStudent(String n, String p, String c) { this.name = n; this.password = p; this.className = c; }
    }

    public static void addStudentAndReorder(String rawName, String password) {
        addStudentAndReorder(rawName, password, "TY BCA");
    }

    // Add Student, Sort A-to-Z and Re-assign Roll numbers 1, 2, 3... per class
    public static void addStudentAndReorder(String rawName, String password, String className) {
        String formattedName = formatToTitleCase(rawName);
        String finalPass = (password == null || password.trim().isEmpty())
                ? formattedName.toLowerCase().replaceAll("\\s+", "")
                : password.trim();

        studentRef.get().addOnSuccessListener(snapshot -> {
            List<TempStudent> list = new ArrayList<>();
            if (snapshot.exists()) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String n = ds.child("name").getValue(String.class);
                    String p = ds.child("password").getValue(String.class);
                    String c = ds.child("className").getValue(String.class);
                    if (n != null) {
                        list.add(new TempStudent(n, p != null ? p : n.toLowerCase().replaceAll("\\s+", ""), c != null ? c : "TY BCA"));
                    }
                }
            }

            boolean exists = false;
            for (TempStudent ts : list) {
                if (ts.name.equalsIgnoreCase(formattedName)) {
                    ts.password = finalPass;
                    ts.className = className;
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                list.add(new TempStudent(formattedName, finalPass, className));
            }

            // Group by className and re-assign class-level Roll numbers 1, 2, 3...
            Map<String, List<TempStudent>> classMap = new HashMap<>();
            for (TempStudent ts : list) {
                classMap.computeIfAbsent(ts.className, k -> new ArrayList<>()).add(ts);
            }

            Map<String, Object> updatedStudents = new HashMap<>();
            int globalCounter = 1;
            for (Map.Entry<String, List<TempStudent>> entry : classMap.entrySet()) {
                List<TempStudent> cList = entry.getValue();
                cList.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
                for (int i = 0; i < cList.size(); i++) {
                    String classRoll = String.valueOf(i + 1);
                    String sId = generate8DigitStudentId(classRoll, cList.get(i).name);
                    Map<String, Object> sMap = new HashMap<>();
                    sMap.put("roll", classRoll);
                    sMap.put("name", cList.get(i).name);
                    sMap.put("password", cList.get(i).password);
                    sMap.put("className", entry.getKey());
                    sMap.put("studentId", sId);
                    updatedStudents.put(String.valueOf(globalCounter++), sMap);
                }
            }

            studentRef.setValue(updatedStudents);
        });
    }

    // Remove Student and Re-assign Roll numbers per class
    public static void removeStudentAndReorder(String rollToRemove, String className) {
        studentRef.get().addOnSuccessListener(snapshot -> {
            List<TempStudent> list = new ArrayList<>();
            if (snapshot.exists()) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String r = ds.child("roll").getValue(String.class);
                    String n = ds.child("name").getValue(String.class);
                    String p = ds.child("password").getValue(String.class);
                    String c = ds.child("className").getValue(String.class);
                    if (n != null) {
                        String targetClass = c != null ? c : "TY BCA";
                        if (r != null && r.equalsIgnoreCase(rollToRemove) && targetClass.equalsIgnoreCase(className)) {
                            continue;
                        }
                        list.add(new TempStudent(n, p != null ? p : n.toLowerCase().replaceAll("\\s+", ""), targetClass));
                    }
                }
            }

            // Group by className and re-assign class-level Roll numbers 1, 2, 3...
            Map<String, List<TempStudent>> classMap = new HashMap<>();
            for (TempStudent ts : list) {
                classMap.computeIfAbsent(ts.className, k -> new ArrayList<>()).add(ts);
            }

            Map<String, Object> updatedStudents = new HashMap<>();
            int globalCounter = 1;
            for (Map.Entry<String, List<TempStudent>> entry : classMap.entrySet()) {
                List<TempStudent> cList = entry.getValue();
                cList.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
                for (int i = 0; i < cList.size(); i++) {
                    String classRoll = String.valueOf(i + 1);
                    String sId = generate8DigitStudentId(classRoll, cList.get(i).name);
                    Map<String, Object> sMap = new HashMap<>();
                    sMap.put("roll", classRoll);
                    sMap.put("name", cList.get(i).name);
                    sMap.put("password", cList.get(i).password);
                    sMap.put("className", entry.getKey());
                    sMap.put("studentId", sId);
                    updatedStudents.put(String.valueOf(globalCounter++), sMap);
                }
            }

            studentRef.setValue(updatedStudents);
        });
    }

    public static void removeStudentAndReorder(String rollToRemove) {
        removeStudentAndReorder(rollToRemove, "TY BCA");
    }
}