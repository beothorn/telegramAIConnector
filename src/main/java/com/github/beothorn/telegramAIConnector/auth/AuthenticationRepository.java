package com.github.beothorn.telegramAIConnector.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

// TODO: Use this to persist logged state
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
            logger.info("Users table created or already exists.");
        } catch (SQLException e) {
            logger.error("Failed to initialize database", e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

}
