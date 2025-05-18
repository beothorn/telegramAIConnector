package com.github.beothorn.telegramAIConnector.user.profile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.Optional;

@Service
public class UserProfileRepository {

    private final Logger logger = LoggerFactory.getLogger(UserProfileRepository.class);

    private String dbUrl;

    public void initDatabase(final String dbUrl) {
        this.dbUrl = dbUrl;
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    chatId INTEGER PRIMARY KEY,
                    profile TEXT NOT NULL
                )
            """);
            logger.info("Users table created or already exists.");
        } catch (SQLException e) {
            logger.error("Failed to initialize database", e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    public void setProfile(long chatId, String profile) {
        if (dbUrl == null) {
            logger.error("Database not initialized. Call initDatabase() first.");
            throw new IllegalStateException("Database not initialized.");
        }
        // Using INSERT ... ON CONFLICT for upsert behavior (update if exists, insert if not)
        // This is standard SQL and well-supported by SQLite.
        String sql = "INSERT INTO users (chatId, profile) VALUES (?, ?) " +
                "ON CONFLICT(chatId) DO UPDATE SET profile = excluded.profile";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, chatId);
            stmt.setString(2, profile);
            stmt.executeUpdate();
            logger.info("Profile set/updated for chatId: {}", chatId);

        } catch (SQLException e) {
            logger.error("Failed to set profile for chatId: {}", chatId, e);
            throw new RuntimeException("Failed to set profile", e);
        }
    }

    public Optional<String> getProfile(long chatId) {
        if (dbUrl == null) {
            logger.error("Database not initialized. Call initDatabase() first.");
            throw new IllegalStateException("Database not initialized.");
        }
        String sql = "SELECT profile FROM users WHERE chatId = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, chatId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String profile = rs.getString("profile");
                    logger.debug("Profile retrieved for chatId: {}", chatId);
                    return Optional.of(profile);
                } else {
                    logger.debug("No profile found for chatId: {}", chatId);
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get profile for chatId: {}", chatId, e);
            throw new RuntimeException("Failed to get profile", e);
        }
    }

}
