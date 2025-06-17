package com.github.beothorn.telegramAIConnector.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.*;

@Service
public class UserRepository {

    private final Logger logger = LoggerFactory.getLogger(UserRepository.class);

    private String dbUrl;

    /**
     * Initializes the user table in the given database.
     *
     * @param dbUrl JDBC connection string
     */
    public void initDatabase(final String dbUrl) {
        this.dbUrl = dbUrl;
        try (Connection conn = DriverManager.getConnection(dbUrl); Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    chatId INTEGER PRIMARY KEY,
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

    /**
     * Inserts or updates a user entry.
     *
     * @param chatId    chat identifier
     * @param username  user nickname
     * @param firstName first name
     * @param lastName  last name
     */
    public void createOrUpdateUser(
        final long chatId,
        final String username,
        final String firstName,
        final String lastName
    ) {
        String insertSql = """
            INSERT OR REPLACE INTO users (chatId, username, first_name, last_name)
            VALUES (?, ?, ?, ?)
        """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(insertSql)) {

            stmt.setLong(1, chatId);
            stmt.setString(2, username);
            stmt.setString(3, firstName);
            stmt.setString(4, lastName);

            stmt.executeUpdate();
            logger.info("User with chatId {} added/updated.", chatId);

        } catch (SQLException e) {
            logger.error("Failed to add user with chatId {}", chatId, e);
        }
    }

    /**
     * Retrieves a user by chat id.
     *
     * @param chatId chat identifier
     * @return stored user info or {@code null}
     */
    public UserInfo getUser(long chatId) {
        String sql = "SELECT chatId, username, first_name, last_name FROM users WHERE chatId = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, chatId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new UserInfo(
                        rs.getLong("chatId"),
                        rs.getString("username"),
                        rs.getString("first_name"),
                        rs.getString("last_name")
                    );
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to fetch user with chatId {}", chatId, e);
        }
        return null;
    }

    /**
     * Deletes a stored user.
     *
     * @param chatId chat identifier
     */
    public void deleteUser(long chatId) {
        String sql = "DELETE FROM users WHERE chatId = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, chatId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to delete user with chatId {}", chatId, e);
        }
    }
}
