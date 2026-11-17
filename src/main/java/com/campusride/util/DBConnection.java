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
public class DBConnection {

    public static Connection getConnection() throws SQLException {
        // 1. read the url, user and password from db.properties
        Properties props = new Properties();
        InputStream in = null;
        try {
            in = DBConnection.class.getClassLoader().getResourceAsStream("db.properties");
            if (in == null) {
                throw new SQLException("db.properties not found. Copy db.properties.example to "
                        + "src/main/resources/db.properties and put your MySQL login in it.");
            }
            props.load(in);
        } catch (IOException e) {
            throw new SQLException("Could not read db.properties: " + e.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // nothing to do, we already have the settings
                }
            }
        }
        String url = props.getProperty("db.url");
        String user = props.getProperty("db.user");
        String password = props.getProperty("db.password");

        // 2. load the MySQL JDBC driver class (it comes from the mysql-connector-j Maven dependency)
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found. Add mysql-connector-j to the project (pom.xml).");
        }

        // 3. connect
        Connection con = DriverManager.getConnection(url, user, password);
        return con;
    }

    // Closes the connection if it was opened. Used in the finally block of every service method.
    public static void close(Connection con) {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                System.out.println("Could not close connection: " + e.getMessage());
            }
        }
    }
}
