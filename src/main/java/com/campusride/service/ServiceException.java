package com.campusride.service;

import java.sql.SQLException;

/**
 * The ONE kind of error the screens need to handle.
 * Its message is always a friendly sentence that can be shown directly in a
 * JOptionPane - never a stack trace.
 *
 * It is a checked exception (extends Exception), so the compiler forces every
 * screen to catch it.
 */
public class ServiceException extends Exception {

    public ServiceException(String friendlyMessage) {
        super(friendlyMessage);
    }

    /**
     * Turns a technical SQLException into a friendly ServiceException.
     * MySQL error codes used here:
     *   1062 duplicate value for a UNIQUE key
     *   1644 our own trigger rule (SIGNAL SQLSTATE '45000')
     *   3819 CHECK constraint failed
     *   1451 cannot delete - row still referenced by a foreign key
     *   1452 foreign key points to a row that does not exist
     *   1205 / 1213 lock wait timeout / deadlock
     */
    public static ServiceException fromSql(SQLException e) {
        // Details go to the console for the developers; the user gets a short sentence.
        System.err.println("SQL error " + e.getErrorCode() + ": " + e.getMessage());

        String msg = e.getMessage() == null ? "" : e.getMessage();
        switch (e.getErrorCode()) {
            case 1062:
                if (msg.contains("users.username"))      return new ServiceException("That username is already taken. Please choose another.");
                if (msg.contains("students.roll_no"))    return new ServiceException("That roll number is already registered.");
                if (msg.contains("buses.reg_no"))        return new ServiceException("A bus with this registration number already exists.");
                if (msg.contains("routes.route_name"))   return new ServiceException("A route with this name already exists.");
                if (msg.contains("routes.bus_id"))       return new ServiceException("That bus is already assigned to another route.");
                if (msg.contains("uq_stops_seq"))        return new ServiceException("This route already has a stop with that sequence number.");
                if (msg.contains("bus_passes"))          return new ServiceException("Another pass was issued at the same moment. Please try again.");
                return new ServiceException("This record already exists.");
            case 1644:
                return new ServiceException(msg);   // trigger messages are already friendly
            case 3819:
                return new ServiceException("Some of the data is not valid. Please check the fields and try again.");
            case 1451:
                return new ServiceException("This record is still in use, so it cannot be deleted.");
            case 1452:
                return new ServiceException("A linked record no longer exists. Please refresh and try again.");
            case 1205:
            case 1213:
                return new ServiceException("The database was busy. Please try again.");
            default:
                // SQLState 08xxx = connection problems; "db.properties" = our own message
                if ((e.getSQLState() != null && e.getSQLState().startsWith("08")) || msg.contains("db.properties")
                        || e.getErrorCode() == 1045 || e.getErrorCode() == 1049) {
                    return new ServiceException("Cannot connect to the database. Is MySQL running and is "
                            + "db.properties correct?");
                }
                return new ServiceException("A database error occurred (code " + e.getErrorCode() + "). Please try again.");
        }
    }
}
