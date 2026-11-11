package com.campusride.model;

/** One row of the buses table. */
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

    /** Text shown when a Bus is put inside a JComboBox. */
    @Override
    public String toString() { return regNo + " (" + capacity + " seats)"; }
}
