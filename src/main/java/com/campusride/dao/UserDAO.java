package com.campusride.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.campusride.model.User;

// SQL for the users table.
// DAO classes only run SQL. The rules (validation etc.) are in the service classes.
// The service opens the Connection and passes it in, so it can run many DAO calls in one transaction.
// There is no catch block here: an SQLException goes up to the service, which shows a message.
// The finally block always closes the ResultSet and PreparedStatement (the service closes the Connection).
public class UserDAO {

    // returns null if there is no user with this username
    public User findByUsername(Connection con, String username) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement("SELECT user_id, username, password_hash, password_salt, role, created_at "
                    + "FROM users WHERE username = ?");
            ps.setString(1, username);
            rs = ps.executeQuery();
            if (rs.next()) {
                User u = new User();
                u.setUserId(rs.getInt("user_id"));
                u.setUsername(rs.getString("username"));
                u.setPasswordHash(rs.getString("password_hash"));
                u.setPasswordSalt(rs.getString("password_salt"));
                u.setRole(rs.getString("role"));
                u.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                return u;
            } else {
                return null;
            }
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
    }

    public boolean usernameExists(Connection con, String username) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement("SELECT 1 FROM users WHERE username = ?");
            ps.setString(1, username);
            rs = ps.executeQuery();
            return rs.next();
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
    }

    // inserts the user and returns the new user_id that MySQL generated
    public int insert(Connection con, User u) throws SQLException {
        PreparedStatement ps = null;
        ResultSet keys = null;
        try {
            ps = con.prepareStatement("INSERT INTO users (username, password_hash, password_salt, role) "
                    + "VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPasswordHash());
            ps.setString(3, u.getPasswordSalt());
            ps.setString(4, u.getRole());
            ps.executeUpdate();
            keys = ps.getGeneratedKeys();
            keys.next();
            return keys.getInt(1);
        } finally {
            if (keys != null) keys.close();
            if (ps != null) ps.close();
        }
    }
}
