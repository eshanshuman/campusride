package com.campusride.model;

// one row of the routes table
// busRegNo and capacity are not in the routes table, they come from a JOIN with buses
public class Route {
    private int routeId;
    private String routeName;
    private int busId;
    private boolean active;
    private String busRegNo;
    private int capacity;

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

    // this text is what a JComboBox shows for a route
    @Override
    public String toString() {
        return routeName;
    }
}
