package com.campusride.model;

// one row of the buses table
public class Bus {
    private int busId;
    private String regNo;
    private int capacity;

    public int getBusId() { return busId; }
    public void setBusId(int busId) { this.busId = busId; }
    public String getRegNo() { return regNo; }
    public void setRegNo(String regNo) { this.regNo = regNo; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    // this text is what a JComboBox shows for a bus
    @Override
    public String toString() {
        return regNo + " (" + capacity + " seats)";
    }
}
