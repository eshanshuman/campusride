package com.campusride.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Opens JDBC connections to MySQL.
 *
 * The URL, user and password are read from db.properties, which lives in
 * src/main/resources (Maven copies it onto the classpath). That file is in
 * .gitignore, so each team member keeps their own password out of GitHub.
 *
 * Every call to getConnection() returns a NEW connection. The caller must close
 * it, which we always do with try-with-resources:
 *
 *     try (Connection con = DBConnection.getConnection()) { ... }
 */
public final class DBConnection {

    private static String url;
    private static String user;
    private static String password;

    private DBConnection() { }

    /** Reads db.properties once, the first time we need it. */
    private static synchronized void loadSettings() throws SQLException {
        if (url != null) {
            return;   // already loaded
        }
        try (InputStream in = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new SQLException("db.properties not found. Copy db.properties.example to "
                        + "src/main/resources/db.properties and enter your MySQL login.");
            }
            Properties props = new Properties();
            props.load(in);
            url = props.getProperty("db.url");
            user = props.getProperty("db.user");
            password = props.getProperty("db.password");
        } catch (IOException e) {
            throw new SQLException("Could not read db.properties: " + e.getMessage());
        }
    }

    /** Returns a new open connection (auto-commit is ON by default). */
    public static Connection getConnection() throws SQLException {
        loadSettings();
        return DriverManager.getConnection(url, user, password);
    }
}
