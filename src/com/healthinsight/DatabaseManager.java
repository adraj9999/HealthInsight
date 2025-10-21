package com.healthinsight;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DatabaseManager
 * - MySQL + JDBC
 * - Creates database/tables if permitted
 * - Saves/fetches user assessments
 *
 * Make sure you add MySQL Connector/J to the classpath (com.mysql.cj.jdbc.Driver).
 */
public class DatabaseManager {

    // ---- Configure these as needed ----
    private static final String DB_HOST = "localhost";
    private static final int DB_PORT = 3306;
    private static final String DB_NAME = "health_insight";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "NewStrongPassword123!"; // your current root password
    // -----------------------------------

    private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    private boolean connected = false;

    public void initializeDatabase() {
        try {
            Class.forName(JDBC_DRIVER);
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found. Add mysql-connector-j to classpath.");
            connected = false;
            return;
        }

        // 1) Create DB if not exists (requires privileges)
        try (Connection conn = DriverManager.getConnection(serverUrl(), DB_USER, DB_PASS);
             Statement st = conn.createStatement()) {
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        } catch (SQLException e) {
            System.err.println("Could not create database (may lack privileges): " + e.getMessage());
        }

        // 2) Create tables if not exists
        try (Connection conn = DriverManager.getConnection(dbUrl(), DB_USER, DB_PASS);
             Statement st = conn.createStatement()) {

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  name VARCHAR(120) NOT NULL,
                  age INT,
                  sex VARCHAR(40),
                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS assessments (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  user_id BIGINT NOT NULL,
                  symptoms TEXT,
                  top_conditions TEXT,
                  advice TEXT,
                  urgent TINYINT(1) DEFAULT 0,
                  notes TEXT,
                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """);

            connected = true;
        } catch (SQLException e) {
            System.err.println("DB init error: " + e.getMessage());
            connected = false;
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public long ensureUser(String name, int age, String sex) throws SQLException {
        Long existing = findUserIdByName(name);
        if (existing != null) return existing;

        String sql = "INSERT INTO users(name, age, sex) VALUES(?,?,?)";
        try (Connection conn = DriverManager.getConnection(dbUrl(), DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setInt(2, age);
            ps.setString(3, sex);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new SQLException("Could not create user.");
    }

    public Long findUserIdByName(String name) throws SQLException {
        String sql = "SELECT id FROM users WHERE name = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl(), DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("id");
                return null;
            }
        }
    }

    public void saveAssessment(long userId,
                               String symptomsCsv,
                               String topConditions,
                               String advice,
                               boolean urgent,
                               String notes) throws SQLException {
        String sql = "INSERT INTO assessments(user_id, symptoms, top_conditions, advice, urgent, notes) VALUES(?,?,?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(dbUrl(), DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, symptomsCsv);
            ps.setString(3, topConditions);
            ps.setString(4, advice);
            ps.setBoolean(5, urgent);
            ps.setString(6, emptyToNull(notes));
            ps.executeUpdate();
        }
    }

    public List<AssessmentRecord> fetchRecentAssessments(long userId, int limit) throws SQLException {
        String sql = "SELECT id, symptoms, top_conditions, advice, urgent, notes, created_at " +
                     "FROM assessments WHERE user_id = ? ORDER BY created_at DESC LIMIT ?";
        try (Connection conn = DriverManager.getConnection(dbUrl(), DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, limit);
            List<AssessmentRecord> out = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new AssessmentRecord(
                        rs.getLong("id"),
                        userId,
                        rs.getString("symptoms"),
                        rs.getString("top_conditions"),
                        rs.getString("advice"),
                        rs.getBoolean("urgent"),
                        rs.getString("notes"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                    ));
                }
            }
            return out;
        }
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private String serverUrl() {
        return "jdbc:mysql://" + DB_HOST + ":" + DB_PORT +
               "/?serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false";
    }

    private String dbUrl() {
        return "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME +
               "?serverTimezone=UTC&useUnicode=true&characterEncoding=utf8&allowPublicKeyRetrieval=true&useSSL=false";
    }

    /* ---------- Models for history ---------- */
    public record AssessmentRecord(
        long id,
        long userId,
        String symptoms,
        String topConditions,
        String advice,
        boolean urgent,
        String notes,
        LocalDateTime createdAt
    ) {}

    // ---------- Quick connection test ----------
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/health_insight?serverTimezone=UTC&useUnicode=true&characterEncoding=utf8&allowPublicKeyRetrieval=true&useSSL=false";
        try (Connection conn = DriverManager.getConnection(url, "root", "NewStrongPassword123!")) {
            System.out.println("Connected successfully: " + (conn != null));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}