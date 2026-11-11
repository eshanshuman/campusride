package com.campusride.model;

/**
 * One row of the routes table, plus two read-only fields (busRegNo, capacity)
 * that the DAO fills from a JOIN with buses, so screens need only one query.
 */
public class Route {
    private int routeId;
    private String routeName;
    private int busId;
    private boolean active;
    private String busRegNo;   // from JOIN buses
    private int capacity;      // from JOIN buses

    public int getRouteId() { return routeId; }
    public void setRouteId(int routeId) { this.routeId = routeId; }
    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }
    public int getBusId() { return busId; }
    public void setBusId(int busId) { this.busId = busId; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getBusRegNo() { return busRegNo; }
    public void setBusRegNo(String busRegNo) { this.busRegNo = busRegNo; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    /** Text shown when a Route is put inside a JComboBox. */
    @Override
    public String toString() { return routeName; }
}
