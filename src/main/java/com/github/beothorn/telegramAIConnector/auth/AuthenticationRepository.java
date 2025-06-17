package com.github.beothorn.telegramAIConnector.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.Optional;

@Service
public class AuthenticationRepository {

    private final Logger logger = LoggerFactory.getLogger(AuthenticationRepository.class);

    private String dbUrl;

    /**
     * Initializes the repository using the given SQLite database URL.
     *
     * @param dbUrl JDBC URL of the database
     */
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

    /**
     * Creates or updates an authentication entry.
     *
     * @param chatId           chat identifier
     * @param passwordHash     hashed password
     * @param logged           logged in state
     * @param logExpirationDate expiration date for the logged state
     * @return the stored {@link AuthData}
     */
    public AuthData addAuthEntry(
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
        return new AuthData(chatId, passwordHash, logged, logExpirationDate);
    }

    /**
     * Updates the logged state for a chat.
     * The column expiration date is used to determine if the password is still valid.
     *
     * @param chatId           chat identifier
     * @param logged           new logged state
     * @param logExpirationDate expiration date
     */
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
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBoolean(1, logged);
            stmt.setString(2, logExpirationDate);
            stmt.setLong(3, chatId);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Set logged state for chatId {} to {} with expiration {}", chatId, logged, logExpirationDate);
            } else {
                logger.warn("No auth entry found for chatId {} to update logged state", chatId);
            }

        } catch (SQLException e) {
            logger.error("Failed to update logged state for chatId {}", chatId, e);
        }
    }

    /**
     * Retrieves authentication data for a chat.
     * Checking expiration date must be done by the caller.
     *
     * @param chatId chat identifier
     * @return authentication data or empty if not found
     */
    public Optional<AuthData> getAuthData(final long chatId) {
        String sql = "SELECT chatId, password_hash, logged, log_expiration_date FROM auth WHERE chatId = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, chatId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Long foundChatId = rs.getLong("chatId");
                    String passwordHash = rs.getString("password_hash");
                    boolean logged = rs.getBoolean("logged");
                    String logExpirationDate = rs.getString("log_expiration_date");

                    return Optional.of(new AuthData(foundChatId, passwordHash, logged, logExpirationDate));
                } else {
                    logger.warn("No auth data found for chatId {}", chatId);
                    return Optional.empty();
                }
            }

        } catch (SQLException e) {
            logger.error("Failed to retrieve auth data for chatId {}", chatId, e);
            return Optional.empty();
        }
    }

    /**
     * Deletes authentication data for a chat.
     *
     * @param chatId chat identifier
     */
    public void deleteAuthData(long chatId) {
        String sql = "DELETE FROM auth WHERE chatId = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, chatId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to delete auth data for chatId {}", chatId, e);
        }
    }
}
