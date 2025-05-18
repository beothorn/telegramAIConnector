package com.github.beothorn.telegramAIConnector.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class AuthenticationRepository {

    private final Logger logger = LoggerFactory.getLogger(AuthenticationRepository.class);

    private String dbUrl;

    public void initDatabase(final String dbUrl) {
        this.dbUrl = dbUrl;
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS auth (
                    chatId INTEGER PRIMARY KEY,
                    password_hash TEXT NOT NULL,
                    logged BOOLEAN NOT NULL DEFAULT FALSE,
                    log_expiration_date TEXT
                )
            """);
            logger.info("Auth table created or already exists.");
        } catch (SQLException e) {
            logger.error("Failed to initialize database", e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    public void addAuthEntry(
        final long chatId,
        final String passwordHash,
        final boolean logged,
        final String logExpirationDate
    ) {
        String sql = """
            INSERT OR REPLACE INTO auth (chatId, password_hash, logged, log_expiration_date)
            VALUES (?, ?, ?, ?)
        """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, chatId);
            stmt.setString(2, passwordHash);
            stmt.setBoolean(3, logged);
            stmt.setString(4, logExpirationDate);

            stmt.executeUpdate();
            logger.info("Auth entry added/updated for chatId {}", chatId);

        } catch (SQLException e) {
            logger.error("Failed to add/update auth entry for chatId {}", chatId, e);
        }
    }

    public void setLoggedState(
        final long chatId,
        final boolean logged,
        final String logExpirationDate
    ) {
        String sql = """
        UPDATE auth
        SET logged = ?, log_expiration_date = ?
        WHERE chatId = ?
    """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBoolean(1, logged);
            pstmt.setString(2, logExpirationDate);
            pstmt.setLong(3, chatId);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Set logged state for chatId {} to {} with expiration {}", chatId, logged, logExpirationDate);
            } else {
                logger.warn("No auth entry found for chatId {} to update logged state", chatId);
            }

        } catch (SQLException e) {
            logger.error("Failed to update logged state for chatId {}", chatId, e);
        }
    }

}
