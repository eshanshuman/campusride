package com.campusride.model;

import java.time.LocalTime;

/** One row of the route_stops table. */
public class Stop {
    private int stopId;
    private int routeId;
    private String stopName;
    private int stopSeq;
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

    /** Text shown when a Stop is put inside a JComboBox. */
    @Override
    public String toString() { return stopSeq + ". " + stopName + " (" + pickupTime + ")"; }
}
