package com.campusride.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * One row of bus_passes, plus display-only fields from JOINs.
 * An ACTIVE pass is the student's route allocation.
 */
public class BusPass {
    private int passId;
    private int applicationId;
    private String passNo;
    private LocalDate validFrom;
    private LocalDate validTo;
    private String status;          // ACTIVE, CANCELLED, EXPIRED
    private LocalDateTime issuedAt;

    // ---- display-only (from JOINs) ----
    private String studentName;
    private String rollNo;
    private String phone;
    private String routeName;
    private String busRegNo;
    private String stopName;
    private int stopSeq;
    private LocalTime pickupTime;

    public int getPassId() { return passId; }
    public void setPassId(int passId) { this.passId = passId; }
    public int getApplicationId() { return applicationId; }
    public void setApplicationId(int applicationId) { this.applicationId = applicationId; }
    public String getPassNo() { return passNo; }
    public void setPassNo(String passNo) { this.passNo = passNo; }
    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getRollNo() { return rollNo; }
    public void setRollNo(String rollNo) { this.rollNo = rollNo; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }
    public String getBusRegNo() { return busRegNo; }
    public void setBusRegNo(String busRegNo) { this.busRegNo = busRegNo; }
    public String getStopName() { return stopName; }
    public void setStopName(String stopName) { this.stopName = stopName; }
    public int getStopSeq() { return stopSeq; }
    public void setStopSeq(int stopSeq) { this.stopSeq = stopSeq; }
    public LocalTime getPickupTime() { return pickupTime; }
    public void setPickupTime(LocalTime pickupTime) { this.pickupTime = pickupTime; }
}
