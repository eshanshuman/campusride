package com.campusride.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

// one row of the pass_applications table
// we don't store the route here - the stop already tells us the route
public class Application {
    private int applicationId;
    private int studentId;
    private int stopId;
    private LocalDate validFrom;
    private LocalDate validTo;
    private String status;          // DRAFT, SUBMITTED, APPROVED or REJECTED
    private String remarks;
    private LocalDateTime appliedAt;
    private Integer reviewedBy;     // Integer, not int, because it is NULL until an admin decides
    private LocalDateTime reviewedAt;
    private String adminRemarks;

    // extra fields filled from JOINs, only used to show the tables on screen
    private String studentName;
    private String rollNo;
    private String stopName;
    private int routeId;
    private String routeName;

    public int getApplicationId() { return applicationId; }
    public void setApplicationId(int applicationId) { this.applicationId = applicationId; }
    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }
    public int getStopId() { return stopId; }
    public void setStopId(int stopId) { this.stopId = stopId; }
    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
    public Integer getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Integer reviewedBy) { this.reviewedBy = reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getAdminRemarks() { return adminRemarks; }
    public void setAdminRemarks(String adminRemarks) { this.adminRemarks = adminRemarks; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getRollNo() { return rollNo; }
    public void setRollNo(String rollNo) { this.rollNo = rollNo; }
    public String getStopName() { return stopName; }
    public void setStopName(String stopName) { this.stopName = stopName; }
    public int getRouteId() { return routeId; }
    public void setRouteId(int routeId) { this.routeId = routeId; }
    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }
}
