package com.example.smartattendance;

public class LeaveApplicationModel {
    private String leaveId;
    private String applicantName;
    private String applicantRollOrId;
    private String leaveType;
    private String reason;
    private String status;
    private long timestamp;
    private long actionTimestamp;

    public LeaveApplicationModel() {
        // Required for Firebase
    }

    public LeaveApplicationModel(String leaveId, String applicantName, String applicantRollOrId, String leaveType, String reason, String status, long timestamp) {
        this.leaveId = leaveId;
        this.applicantName = applicantName;
        this.applicantRollOrId = applicantRollOrId;
        this.leaveType = leaveType;
        this.reason = reason;
        this.status = status;
        this.timestamp = timestamp;
        this.actionTimestamp = 0L;
    }

    public String getLeaveId() { return leaveId; }
    public void setLeaveId(String leaveId) { this.leaveId = leaveId; }

    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }

    public String getApplicantRollOrId() { return applicantRollOrId; }
    public void setApplicantRollOrId(String applicantRollOrId) { this.applicantRollOrId = applicantRollOrId; }

    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public long getActionTimestamp() { return actionTimestamp; }
    public void setActionTimestamp(long actionTimestamp) { this.actionTimestamp = actionTimestamp; }
}