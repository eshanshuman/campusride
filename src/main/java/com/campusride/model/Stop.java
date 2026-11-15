package com.campusride.model;

import java.time.LocalTime;

// one row of the route_stops table
public class Stop {
    private int stopId;
    private int routeId;
    private String stopName;
    private int stopSeq;           // order of the stop on the route (1, 2, 3 ...)
    private LocalTime pickupTime;

    public int getStopId() { return stopId; }
    public void setStopId(int stopId) { this.stopId = stopId; }
    public int getRouteId() { return routeId; }
    public void setRouteId(int routeId) { this.routeId = routeId; }
    public String getStopName() { return stopName; }
    public void setStopName(String stopName) { this.stopName = stopName; }
    public int getStopSeq() { return stopSeq; }
    public void setStopSeq(int stopSeq) { this.stopSeq = stopSeq; }
    public LocalTime getPickupTime() { return pickupTime; }
    public void setPickupTime(LocalTime pickupTime) { this.pickupTime = pickupTime; }

    // this text is what a JComboBox shows for a stop
    @Override
    public String toString() {
        return stopSeq + ". " + stopName + " (" + pickupTime + ")";
    }
}
