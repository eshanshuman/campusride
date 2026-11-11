package com.campusride.service;

/**
 * The numbers shown in the admin "capacity panel" for one application.
 * (A small read-only holder - built by AdminService.getCapacityInfo.)
 */
public class CapacityInfo {

    private final String routeName;
    private final String busRegNo;
    private final int capacity;
    private final int activeOverlapping;      // ACTIVE passes on the route overlapping the period
    private final boolean studentHasOverlap;  // student already holds an overlapping ACTIVE pass

    public CapacityInfo(String routeName, String busRegNo, int capacity,
                        int activeOverlapping, boolean studentHasOverlap) {
        this.routeName = routeName;
        this.busRegNo = busRegNo;
        this.capacity = capacity;
        this.activeOverlapping = activeOverlapping;
        this.studentHasOverlap = studentHasOverlap;
    }

    public String getRouteName() { return routeName; }
    public String getBusRegNo() { return busRegNo; }
    public int getCapacity() { return capacity; }
    public int getActiveOverlapping() { return activeOverlapping; }
    public boolean isStudentHasOverlap() { return studentHasOverlap; }

    /** Seats still free for the period (never below zero). */
    public int getSeatsLeft() { return Math.max(0, capacity - activeOverlapping); }

    /** True when approval would pass both checks. */
    public boolean canApprove() { return getSeatsLeft() > 0 && !studentHasOverlap; }
}
