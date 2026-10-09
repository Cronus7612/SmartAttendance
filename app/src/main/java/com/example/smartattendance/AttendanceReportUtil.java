package com.example.smartattendance;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttendanceReportUtil {

    public static String buildCsvContent(String className, String subjectName, List<StudentModel> studentList) {
        StringBuilder csv = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        String generatedAt = sdf.format(new Date());

        csv.append("SmartAttendance College Attendance Report\n");
        csv.append("Class,").append(className).append("\n");
        csv.append("Subject,").append(subjectName).append("\n");
        csv.append("Generated On,").append(generatedAt).append("\n\n");
        csv.append("Roll No,Student Name,Student ID,Attended Lectures,Total Lectures,Attendance %,Exam Eligibility Status\n");

        if (studentList != null && !studentList.isEmpty()) {
            for (StudentModel s : studentList) {
                int total = 10;
                int attended = s.isPresent() ? 8 : 4;
                int percent = (int) Math.round(((double) attended / total) * 100);
                String status = percent >= 75 ? "Eligible (Safe)" : "Defaulter (<75%)";

                csv.append(s.getRollNo()).append(",")
                        .append("\"").append(s.getName()).append("\"").append(",")
                        .append(s.getStudentId()).append(",")
                        .append(attended).append(",")
                        .append(total).append(",")
                        .append(percent).append("%,")
                        .append(status).append("\n");
            }
        }
        return csv.toString();
    }

    public static void exportAndShareReport(Context context, String className, String subjectName, List<StudentModel> studentList) {
        try {
            String csvData = buildCsvContent(className, subjectName, studentList);
            File cacheDir = context.getCacheDir();
            File reportFile = new File(cacheDir, "Attendance_Report_" + className.replaceAll("\\s+", "_") + ".csv");

            FileOutputStream fos = new FileOutputStream(reportFile);
            fos.write(csvData.getBytes(StandardCharsets.UTF_8));
            fos.close();

            Uri contentUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", reportFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/csv");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Attendance Report - " + className + " (" + subjectName + ")");
            shareIntent.putExtra(Intent.EXTRA_TEXT, csvData);
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(Intent.createChooser(shareIntent, "Share / Save Attendance Report"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}