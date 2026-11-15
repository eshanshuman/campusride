package com.campusride.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.campusride.model.User;

// SQL for the users table.
// DAO classes only run SQL. The rules (validation etc.) are in the service classes.
// The service passes in the Connection, so it can run many DAO calls in one transaction.
public class UserDAO {

    // returns null if there is no user with this username
    public User findByUsername(Connection con, String username) throws SQLException {
        String sql = "SELECT user_id, username, password_hash, password_salt, role, created_at "
                   + "FROM users WHERE username = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
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
            }
        }
    }

    public boolean usernameExists(Connection con, String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // inserts the user and returns the new user_id that MySQL generated
    public int insert(Connection con, User u) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, password_salt, role) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPasswordHash());
            ps.setString(3, u.getPasswordSalt());
            ps.setString(4, u.getRole());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }
}
