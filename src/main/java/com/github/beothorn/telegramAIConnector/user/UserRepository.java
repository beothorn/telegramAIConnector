package com.github.beothorn.telegramAIConnector.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Service
public class UserRepository {

    private final Logger logger = LoggerFactory.getLogger(UserRepository.class);

    private String dbUrl;

    public void initDatabase(final String dbUrl) {
        this.dbUrl = dbUrl;
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    chatId INTEGER PRIMARY KEY,
                    profile TEXT NOT NULL,
                    username TEXT,
                    first_name TEXT,
                    last_name TEXT
                )
            """);
            logger.info("Users table created or already exists.");
        } catch (SQLException e) {
            logger.error("Failed to initialize database", e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }
}
