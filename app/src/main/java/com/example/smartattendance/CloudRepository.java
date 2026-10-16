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
import java.util.Objects;

@SuppressWarnings("unused")
public class CloudRepository {

    private static final DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
    public static final DatabaseReference adminRef = rootRef.child("admins");
    public static final DatabaseReference teacherRef = rootRef.child("teachers");
    public static final DatabaseReference studentRef = rootRef.child("students");
    public static final DatabaseReference attendanceRef = rootRef.child("attendance");
    public static final DatabaseReference attendanceLogsRef = rootRef.child("attendance_logs");
    public static final DatabaseReference announcementsRef = rootRef.child("announcements");
    public static final DatabaseReference leaveRef = rootRef.child("leave_applications");
    public static final DatabaseReference assignmentsRef = rootRef.child("assignments");
    public static final DatabaseReference resourcesRef = rootRef.child("study_resources");

    // Safe String Extractor from Firebase DataSnapshot preventing DatabaseException on numeric types
    public static String getSafeString(DataSnapshot snapshot, String fieldName) {
        if (snapshot != null && snapshot.hasChild(fieldName)) {
            Object val = snapshot.child(fieldName).getValue();
            if (val != null) {
                return ("" + val).trim();
            }
        }
        return "";
    }

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

    public static void saveAttendanceLog(String attendanceId, String className, String subject, String date, String lectureNumber, String lectureType, int presentCount, int totalCount) {
        String key = attendanceLogsRef.push().getKey();
        if (key != null) {
            Map<String, Object> logMap = new HashMap<>();
            logMap.put("attendanceId", attendanceId);
            logMap.put("className", className);
            logMap.put("subject", subject);
            logMap.put("date", date);
            logMap.put("lectureNumber", lectureNumber);
            logMap.put("lectureType", lectureType);
            logMap.put("presentCount", presentCount);
            logMap.put("totalCount", totalCount);
            logMap.put("status", "Saved 🟢");
            logMap.put("timestamp", System.currentTimeMillis());
            attendanceLogsRef.child(key).setValue(logMap);
        }
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

    public static void postAssignment(String title, String description, String subject, String className, String dueDate, String postedBy) {
        String id = assignmentsRef.push().getKey();
        if (id != null) {
            AssignmentModel model = new AssignmentModel(id, title, description, subject, className, dueDate, postedBy, System.currentTimeMillis());
            assignmentsRef.child(id).setValue(model);
        }
    }

    public static void clearAllAssignments() {
        assignmentsRef.removeValue();
    }

    public static void postStudyResource(String title, String subject, String className, String resourceUrl, String postedBy) {
        String id = resourcesRef.push().getKey();
        if (id != null) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", id);
            map.put("title", title);
            map.put("subject", subject);
            map.put("className", className);
            map.put("resourceUrl", resourceUrl);
            map.put("postedBy", postedBy);
            map.put("timestamp", System.currentTimeMillis());
            resourcesRef.child(id).setValue(map);
        }
    }

    public static void clearAllStudyResources() {
        resourcesRef.removeValue();
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

        String badge = "REJECTED 🔴";
        if ("APPROVED".equalsIgnoreCase(newStatus)) {
            badge = "APPROVED 🟢";
        }

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
                saveAdmin("10842931", "Siddharth Bhambare", "siddharthbhambare", "siddharth@smartattendance.edu", "9876543210");
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
                    String sId = getSafeString(ds, "studentId");
                    if (sId.length() != 8) {
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
            String cleanName = s.toLowerCase().replaceAll("\\s+", "");
            initialList.add(new TempStudent(s, cleanName, "TY BCA", cleanName + "@smartattendance.edu", "98" + (Math.abs(s.hashCode()) % 100000000)));
        }

        // SY BCA
        String[] syBca = {"Abhishek More", "Aniket Shinde", "Chetan Patil", "Deepak Thorat", "Harshada Deshmukh", "Jayesh Kadam", "Manish Wagh", "Mayuri Gite", "Nilesh Chavan", "Pooja Bhamre", "Prasad Joshi", "Rohit Sonawane"};
        for (String s : syBca) {
            String cleanName = s.toLowerCase().replaceAll("\\s+", "");
            initialList.add(new TempStudent(s, cleanName, "SY BCA", cleanName + "@smartattendance.edu", "98" + (Math.abs(s.hashCode()) % 100000000)));
        }

        // FY BCA
        String[] fyBca = {"Akash Pawar", "Bhavana Kale", "Dhananjay Jagtap", "Ishwar Ghadge", "Kunal Desale", "Madhuri Kulkarni", "Pratiksha Gaikwad", "Rakesh Shinde", "Sagar Patil", "Snehal Jadhav", "Tushar More", "Yogesh Thorat"};
        for (String s : fyBca) {
            String cleanName = s.toLowerCase().replaceAll("\\s+", "");
            initialList.add(new TempStudent(s, cleanName, "FY BCA", cleanName + "@smartattendance.edu", "98" + (Math.abs(s.hashCode()) % 100000000)));
        }

        // Other classes default rosters
        String[] otherClasses = {"TY B.Sc (CS)", "SY B.Sc (CS)", "FY B.Sc (CS)", "TY B.Sc", "SY B.Sc", "FY B.Sc", "TY BBA", "SY BBA", "FY BBA", "TY B.Com", "SY B.Com", "FY B.Com", "12th (HSC)", "11th (FYJC)"};
        String[] boyFirst = {"Aarav", "Rohan", "Siddharth", "Omkar", "Kunal", "Pranav", "Abhishek", "Harshal", "Sanket", "Mayur", "Rushikesh", "Swapnil"};
        String[] girlFirst = {"Ananya", "Sakshi", "Pooja", "Neha", "Sayali", "Snehal", "Shruti", "Tanvi", "Rutuja", "Pranali", "Aishwarya", "Shweta"};
        String[] lastNames = {"Patil", "Shinde", "Deshmukh", "More", "Kulkarni", "Joshi", "Gite", "Gaikwad", "Kale", "Wagh", "Sonawane", "Thorat"};

        for (String cName : otherClasses) {
            int baseSeed = Math.abs(cName.hashCode());
            int i = 1;
            while (i <= 10) {
                int seedIdx = (baseSeed + (i * 7));
                String lName = lastNames[Math.abs((seedIdx + i) % lastNames.length)];
                String fName = (i % 2 == 0) ? girlFirst[Math.abs((seedIdx + 3) % girlFirst.length)] : boyFirst[Math.abs((seedIdx + 5) % boyFirst.length)];
                String full = fName + " " + lName;
                String clean = full.toLowerCase().replaceAll("\\s+", "");
                initialList.add(new TempStudent(full, clean, cName, clean + "@smartattendance.edu", "98" + Math.abs((seedIdx * 12345) % 100000000)));
                i++;
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
                sMap.put("email", cList.get(i).email);
                sMap.put("phone", cList.get(i).phone);
                dbMap.put(String.valueOf(globalCounter++), sMap);
            }
        }

        studentRef.setValue(dbMap);
    }

    private static void uploadAll27Teachers() {
        String bca = "TY BCA, SY BCA";
        String bcs = "TY B.Sc (CS), SY B.Sc (CS)";
        String bba = "TY BBA, SY BBA";
        String bcom = "TY B.Com, SY B.Com";
        String bsc = "TY B.Sc, SY B.Sc";
        String hsc = "12th (HSC), 11th (FYJC)";

        // Image Timetable Mapping Teachers
        saveTeacher("99102451", "Prof-Nikam R.B.", "nikam", "TY BCA", "Essential of Cloud Computing", "nikam@smartattendance.edu", "9823014589");
        saveTeacher("99102452", "Prof-Dhawale S.S", "dhawale", "TY BCA", "Software Engineering", "dhawale@smartattendance.edu", "9823014590");
        saveTeacher("99102453", "Prof-Pawar A.V", "pawar", "TY BCA", "Entrepreneurship Essentials (ERP)", "pawar@smartattendance.edu", "9823014591");
        saveTeacher("99102454", "Prof-Pansare M.B", "pansare", "TY BCA", "Project based in Major Mandatory", "pansare@smartattendance.edu", "9823014592");
        saveTeacher("99102455", "Prof-Dhole S.B", "dhole", "TY BCA", "Java Programming", "dhole@smartattendance.edu", "9823014593");
        saveTeacher("99102456", "Prof-Baviskar R.R", "baviskar", "TY BCA", "User Interface & User Experience Design", "baviskar@smartattendance.edu", "9823014594");

        // General Teachers
        saveTeacher("20481923", "Prof. S. R. Patil", "patil", bca, "Java, Cloud Computing, UI-UX Design", "patil@smartattendance.edu", "9876543210");
        saveTeacher("59182341", "Prof. V. M. Shinde", "shinde", bcs, "Advanced Java, OS, Python", "shinde@smartattendance.edu", "9876543211");
        saveTeacher("83920145", "Prof. A. K. Deshmukh", "deshmukh", bba, "Marketing, HR, Business Law", "deshmukh@smartattendance.edu", "9876543212");
        saveTeacher("11245890", "Prof. R. B. More", "more", bcom, "Advanced Accounting, Banking", "more@smartattendance.edu", "9876543213");
        saveTeacher("78451296", "Prof. N. S. Kulkarni", "kulkarni", bsc, "Solid State Physics, Chemistry", "kulkarni@smartattendance.edu", "9876543214");
        saveTeacher("63214587", "Prof. P. D. Joshi", "joshi", hsc, "Physics, Chemistry, IT", "joshi@smartattendance.edu", "9876543215");
        saveTeacher("95123648", "Prof. K. T. Gite", "gite", bca, "Data Structures, Web Technology", "gite@smartattendance.edu", "9876543216");
        saveTeacher("45871236", "Prof. M. L. Gaikwad", "gaikwad", bcs, "Microcontroller, RDBMS", "gaikwad@smartattendance.edu", "9876543217");
        saveTeacher("32145698", "Prof. S. P. Kale", "kale", bba, "Financial Management, SCM", "kale@smartattendance.edu", "9876543218");
        saveTeacher("85214796", "Prof. H. R. Wagh", "wagh", bcom, "Corporate Accounting, Economics", "wagh@smartattendance.edu", "9876543219");
        saveTeacher("96325874", "Prof. C. V. Sonawane", "sonawane", bsc, "Optics, Multivariable Calculus", "sonawane@smartattendance.edu", "9876543220");
        saveTeacher("74125896", "Prof. D. S. Thorat", "thorat", hsc, "Mathematics, Biology", "thorat@smartattendance.edu", "9876543221");
        saveTeacher("36985214", "Prof. J. K. Ghadge", "ghadge", bca, "C Programming, Office Automation", "ghadge@smartattendance.edu", "9876543222");
        saveTeacher("15975348", "Prof. B. M. Jadhav", "jadhav", bcs, "Problem Solving, Electronics", "jadhav@smartattendance.edu", "9876543223");
        saveTeacher("75395146", "Prof. T. R. Chavan", "chavan", bba, "Principles of Management, Stats", "chavan@smartattendance.edu", "9876543224");
        saveTeacher("85296314", "Prof. U. S. Jagtap", "jagtap", bcom, "Financial Accounting, Marketing", "jagtap@smartattendance.edu", "9876543225");
        saveTeacher("95135748", "Prof. W. P. Bhamre", "bhamre", bsc, "Mechanics, Fundamentals of Chemistry", "bhamre@smartattendance.edu", "9876543226");
        saveTeacher("35715948", "Prof. Y. K. Pawar", "pawar", hsc, "English, Environmental Education", "pawar@smartattendance.edu", "9876543227");
        saveTeacher("45612378", "Prof. Z. N. Kadam", "kadam", bca, "Entrepreneurship, Cloud", "kadam@smartattendance.edu", "9876543228");
        saveTeacher("78945612", "Prof. E. T. Borse", "borse", bcs, "Machine Learning, Software Testing", "borse@smartattendance.edu", "9876543229");
        saveTeacher("12378945", "Prof. F. R. Salunkhe", "salunkhe", bba, "Digital Marketing, Supply Chain", "salunkhe@smartattendance.edu", "9876543230");
    }

    public static void saveAdmin(String id, String name, String password, String email, String phone) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("password", password);
        map.put("email", Objects.requireNonNullElse(email, ""));
        map.put("phone", Objects.requireNonNullElse(phone, ""));
        adminRef.child(id).setValue(map);
    }

    public static void saveAdmin(String id, String name, String password) {
        saveAdmin(id, name, password, "admin@smartattendance.edu", "9876543210");
    }

    public static void saveTeacher(String id, String name, String password, String assignedClasses, String assignedSubjects, String email, String phone) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("password", password);
        map.put("assignedClasses", assignedClasses != null ? assignedClasses : "TY BCA, SY BCA");
        map.put("assignedSubjects", assignedSubjects != null ? assignedSubjects : "Java, Cloud Computing, UI-UX Design");
        map.put("email", Objects.requireNonNullElse(email, ""));
        map.put("phone", Objects.requireNonNullElse(phone, ""));
        teacherRef.child(id).setValue(map);
    }

    public static void saveTeacher(String id, String name, String password, String assignedClasses, String assignedSubjects) {
        saveTeacher(id, name, password, assignedClasses, assignedSubjects, "", "");
    }

    public static void saveTeacher(String id, String name, String password, String assignedClasses) {
        saveTeacher(id, name, password, assignedClasses, "Java, Cloud Computing, UI-UX Design", "", "");
    }

    public static void saveTeacher(String id, String name, String password) {
        saveTeacher(id, name, password, "TY BCA, SY BCA", "Java, Cloud Computing, UI-UX Design", "", "");
    }

    public static void removeTeacher(String id) {
        teacherRef.child(id).removeValue();
    }

    // Dynamic Student Object for sorting
    private static class TempStudent {
        String name;
        String password;
        String className;
        String email;
        String phone;

        TempStudent(String n, String p, String c, String e, String ph) {
            this.name = n;
            this.password = p;
            this.className = c;
            this.email = e;
            this.phone = ph;
        }

        TempStudent(String n, String p, String c) {
            this(n, p, c, "", "");
        }
    }

    public static void addStudentAndReorder(String rawName, String password, String className, String email, String phone) {
        String formattedName = formatToTitleCase(rawName);
        String finalPass = (password == null || password.trim().isEmpty())
                ? formattedName.toLowerCase().replaceAll("\\s+", "")
                : password.trim();

        studentRef.get().addOnSuccessListener(snapshot -> {
            List<TempStudent> list = new ArrayList<>();
            if (snapshot.exists()) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String n = getSafeString(ds, "name");
                    String p = getSafeString(ds, "password");
                    String c = getSafeString(ds, "className");
                    String e = getSafeString(ds, "email");
                    String ph = getSafeString(ds, "phone");
                    if (!n.isEmpty()) {
                        list.add(new TempStudent(n, !p.isEmpty() ? p : n.toLowerCase().replaceAll("\\s+", ""), !c.isEmpty() ? c : "TY BCA", e, ph));
                    }
                }
            }

            boolean exists = false;
            for (TempStudent ts : list) {
                if (ts.name.equalsIgnoreCase(formattedName)) {
                    ts.password = finalPass;
                    ts.className = className;
                    ts.email = email;
                    ts.phone = phone;
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                list.add(new TempStudent(formattedName, finalPass, className, email, phone));
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
                    sMap.put("email", cList.get(i).email);
                    sMap.put("phone", cList.get(i).phone);
                    updatedStudents.put(String.valueOf(globalCounter++), sMap);
                }
            }

            studentRef.setValue(updatedStudents);
        });
    }

    public static void addStudentAndReorder(String rawName, String password, String className) {
        addStudentAndReorder(rawName, password, className, "", "");
    }

    public static void addStudentAndReorder(String rawName, String password) {
        addStudentAndReorder(rawName, password, "TY BCA", "", "");
    }

    // Remove Student and Re-assign Roll numbers per class
    public static void removeStudentAndReorder(String rollToRemove, String className) {
        studentRef.get().addOnSuccessListener(snapshot -> {
            List<TempStudent> list = new ArrayList<>();
            if (snapshot.exists()) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String r = getSafeString(ds, "roll");
                    String n = getSafeString(ds, "name");
                    String p = getSafeString(ds, "password");
                    String c = getSafeString(ds, "className");
                    String e = getSafeString(ds, "email");
                    String ph = getSafeString(ds, "phone");
                    if (!n.isEmpty()) {
                        String targetClass = !c.isEmpty() ? c : "TY BCA";
                        if (!r.isEmpty() && r.equalsIgnoreCase(rollToRemove) && targetClass.equalsIgnoreCase(className)) {
                            continue;
                        }
                        list.add(new TempStudent(n, !p.isEmpty() ? p : n.toLowerCase().replaceAll("\\s+", ""), targetClass, e, ph));
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
                    sMap.put("email", cList.get(i).email);
                    sMap.put("phone", cList.get(i).phone);
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