package com.campusride.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

// Opens a connection to MySQL.
// The url, user and password are in src/main/resources/db.properties.
// That file is in .gitignore so our passwords don't go to GitHub.
// Whoever calls getConnection() must close it (we always use try-with-resources).
public class DBConnection {

    public static Connection getConnection() throws SQLException {
        Properties props = new Properties();

        // read db.properties from the classpath (Maven copies it from src/main/resources)
        try (InputStream in = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new SQLException("db.properties not found. Copy db.properties.example to "
                        + "src/main/resources/db.properties and put your MySQL login in it.");
            }
            props.load(in);
        } catch (IOException e) {
            throw new SQLException("Could not read db.properties: " + e.getMessage());
        }

        String url = props.getProperty("db.url");
        String user = props.getProperty("db.user");
        String password = props.getProperty("db.password");
        return DriverManager.getConnection(url, user, password);
    }
}
