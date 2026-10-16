package com.example.smartattendance;

public class AttendanceEntryModel {
    private String attendanceId;
    private String stream;
    private String standard;
    private String division;
    private String batch;
    private String date;
    private String subject;
    private String subSubject;
    private String lectureType;
    private String status;

    public AttendanceEntryModel() {
        // Required for Firebase
    }

    public AttendanceEntryModel(String attendanceId, String stream, String standard, String division, String batch, String date, String subject, String subSubject, String lectureType, String status) {
        this.attendanceId = attendanceId;
        this.stream = stream;
        this.standard = standard;
        this.division = division;
        this.batch = batch;
        this.date = date;
        this.subject = subject;
        this.subSubject = subSubject;
        this.lectureType = lectureType;
        this.status = status;
    }

    public String getAttendanceId() { return attendanceId; }
    public void setAttendanceId(String attendanceId) { this.attendanceId = attendanceId; }

    public String getStream() { return stream; }
    public void setStream(String stream) { this.stream = stream; }

    public String getStandard() { return standard; }
    public void setStandard(String standard) { this.standard = standard; }

    public String getDivision() { return division; }
    public void setDivision(String division) { this.division = division; }

    public String getBatch() { return batch; }
    public void setBatch(String batch) { this.batch = batch; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getSubSubject() { return subSubject; }
    public void setSubSubject(String subSubject) { this.subSubject = subSubject; }

    public String getLectureType() { return lectureType; }
    public void setLectureType(String lectureType) { this.lectureType = lectureType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}