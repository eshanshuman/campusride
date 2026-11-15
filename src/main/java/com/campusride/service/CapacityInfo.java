package com.campusride.service;

// The numbers shown in the admin's "Capacity check" box for one application.
public class CapacityInfo {

    private String routeName;
    private String busRegNo;
    private int capacity;
    private int seatsUsed;            // ACTIVE passes on this route that overlap the dates
    private boolean studentHasPass;   // student already has an ACTIVE pass for these dates

    public CapacityInfo(String routeName, String busRegNo, int capacity, int seatsUsed, boolean studentHasPass) {
        this.routeName = routeName;
        this.busRegNo = busRegNo;
        this.capacity = capacity;
        this.seatsUsed = seatsUsed;
        this.studentHasPass = studentHasPass;
    }

    public String getRouteName() { return routeName; }
    public String getBusRegNo() { return busRegNo; }
    public int getCapacity() { return capacity; }
    public int getSeatsUsed() { return seatsUsed; }
    public boolean isStudentHasPass() { return studentHasPass; }

    public int getSeatsLeft() {
        int left = capacity - seatsUsed;
        if (left < 0) {
            left = 0;
        }
        return left;
    }

    // true when both approval checks would pass
    public boolean canApprove() {
        return getSeatsLeft() > 0 && !studentHasPass;
    }
}
