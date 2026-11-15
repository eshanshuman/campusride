package com.campusride.service;

import java.sql.SQLException;

// The only error the screens have to catch.
// The message is always a simple sentence we can show in a JOptionPane.
public class ServiceException extends Exception {

    public ServiceException(String message) {
        super(message);
    }

    // Turns a MySQL error into a message a normal user can understand.
    public static ServiceException fromSql(SQLException e) {
        String msg = e.getMessage();
        if (msg == null) {
            msg = "";
        }

        switch (e.getErrorCode()) {
            case 1062:   // duplicate value in a UNIQUE column - find out which one
                if (msg.contains("users.username")) {
                    return new ServiceException("That username is already taken. Please choose another.");
                } else if (msg.contains("students.roll_no")) {
                    return new ServiceException("That roll number is already registered.");
                } else if (msg.contains("buses.reg_no")) {
                    return new ServiceException("A bus with this registration number already exists.");
                } else if (msg.contains("routes.route_name")) {
                    return new ServiceException("A route with this name already exists.");
                } else if (msg.contains("routes.bus_id")) {
                    return new ServiceException("That bus is already assigned to another route.");
                } else if (msg.contains("uq_stops_seq")) {
                    return new ServiceException("This route already has a stop with that sequence number.");
                } else if (msg.contains("bus_passes")) {
                    return new ServiceException("Another pass was issued at the same moment. Please try again.");
                } else {
                    return new ServiceException("This record already exists.");
                }
            case 1644:   // error from one of our triggers - the trigger message is already readable
                return new ServiceException(msg);
            case 3819:   // a CHECK constraint failed
                return new ServiceException("Some of the data is not valid. Please check the fields and try again.");
            case 1451:   // can't delete, another table still points to this row
                return new ServiceException("This record is still in use, so it cannot be deleted.");
            case 1452:   // foreign key points to a row that doesn't exist
                return new ServiceException("A linked record no longer exists. Please refresh and try again.");
            case 1205:   // waited too long for a lock
            case 1213:   // deadlock
                return new ServiceException("The database was busy. Please try again.");
            case 1045:   // wrong MySQL password
            case 1049:   // database doesn't exist
                return new ServiceException("Cannot connect to the database. Is MySQL running and is db.properties correct?");
            default:
                // SQL state 08... means a connection problem; our own db.properties message has error code 0
                String state = e.getSQLState();
                if ((state != null && state.startsWith("08")) || msg.contains("db.properties")) {
                    return new ServiceException("Cannot connect to the database. Is MySQL running and is db.properties correct?");
                }
                return new ServiceException("A database error occurred (code " + e.getErrorCode() + "). Please try again.");
        }
    }
}
