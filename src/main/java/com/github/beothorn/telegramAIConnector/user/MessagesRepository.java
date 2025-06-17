package com.github.beothorn.telegramAIConnector.user;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class MessagesRepository implements ChatMemoryRepository {

    private static final Logger logger = LoggerFactory.getLogger(MessagesRepository.class);
    private final int messageWindowSize;

    private String dbUrl;

    /**
     * Creates a repository with the configured conversation window size.
     *
     * @param messagesOnConversation number of messages kept in memory
     */
    public MessagesRepository(
        @Value("${telegramIAConnector.messagesOnConversation}") final int messagesOnConversation
    ) {
        this.messageWindowSize = messagesOnConversation;
    }

    /**
     * Initializes the repository using the provided database.
     *
     * @param dbUrl JDBC connection string
     */
    public void initDatabase(
        final String dbUrl
    ) {
        this.dbUrl = dbUrl;
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS messages (
                    chatId TEXT NOT NULL,
                    role TEXT NOT NULL,
                    content TEXT NOT NULL,
                    timestamp INTEGER DEFAULT (strftime('%s','now') * 1000)
                )
            """);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize messages database", e);
        }
    }

    @Override
    /**
     * Returns all stored conversation identifiers.
     *
     * @return list of conversation ids
     */
    public List<String> findConversationIds() {
        List<String> conversationIds = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT DISTINCT chatId FROM messages")) {

            while (rs.next()) {
                conversationIds.add(rs.getString("chatId"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve chatIds.", e);
        }
        return conversationIds;
    }

    @Override
    /**
     * Retrieves the last messages for a conversation limited by the window size.
     *
     * @param conversationId conversation identifier
     * @return list of messages
     */
    public List<Message> findByConversationId(
        @NotNull final String conversationId
    ) {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT role, content FROM messages WHERE chatId = ? ORDER BY timestamp DESC LIMIT ?")) {

            stmt.setString(1, conversationId);
            stmt.setInt(2, messageWindowSize);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String role = rs.getString("role");
                String text = rs.getString("content");

                Message message = switch (role) {
                    case "user" -> new UserMessage(text);
                    case "assistant" -> new AssistantMessage(text);
                    case "system", "tool" -> new SystemMessage(text); // Reuse SystemMessage for tool
                    default -> null;
                };

                if (message != null) {
                    messages.add(0, message); // reverse order back to ascending
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch messages", e);
        }
        return messages;
    }

    /**
     * Retrieves the full conversation in chronological order.
     *
     * @param conversationId conversation identifier
     * @return list of messages in chronological order
     */
    public List<Message> getConversations(
        @NotNull final String conversationId
    ) {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT role, content FROM messages WHERE chatId = ? ORDER BY timestamp")) {

            stmt.setString(1, conversationId);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String role = rs.getString("role");
                String text = rs.getString("content");

                Message message = switch (role) {
                    case "user" -> new UserMessage(text);
                    case "assistant" -> new AssistantMessage(text);
                    case "system", "tool" -> new SystemMessage(text); // Reuse SystemMessage for tool
                    default -> null;
                };

                if (message != null) {
                    messages.add(0, message); // reverse order back to ascending
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch messages", e);
        }
        return messages;
    }

    @Override
    /**
     * Persists the latest message of a conversation.
     *
     * @param conversationId conversation identifier
     * @param messages       messages to store
     */
    public void saveAll(
        @NotNull final String conversationId,
        final List<Message> messages
    ) {
        if (messages.isEmpty()) return;

        final String sql = "INSERT INTO messages (chatId, role, content, timestamp) VALUES (?, ?, ?, ?)";

        Message message = messages.getLast(); // Only last message matters

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, conversationId);
            stmt.setString(2, message.getMessageType().getValue());
            stmt.setString(3, message.getText());
            stmt.setLong(4, System.currentTimeMillis());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save message", e);
        }
    }

    @Override
    /**
     * Deletes all messages of a conversation.
     *
     * @param conversationId conversation identifier
     */
    public void deleteByConversationId(@NotNull final String conversationId) {
        String sql = "DELETE FROM messages WHERE chatId = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, conversationId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete conversation", e);
        }
    }

    /**
     * Returns paginated stored messages for UI consumption.
     *
     * @param chatId chat identifier
     * @param limit  maximum number of messages
     * @param offset offset for pagination
     * @return list of stored messages
     */
    public List<StoredMessage> getMessages(String chatId, int limit, int offset) {
        List<StoredMessage> messages = new ArrayList<>();
        String sql = "SELECT rowid, role, content, timestamp FROM messages WHERE chatId = ? ORDER BY timestamp DESC LIMIT ? OFFSET ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, chatId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                long ts = rs.getLong("timestamp");
                String formatted = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        .withZone(java.time.ZoneId.systemDefault())
                        .format(java.time.Instant.ofEpochMilli(ts));
                messages.add(new StoredMessage(
                        rs.getLong("rowid"),
                        rs.getString("role"),
                        rs.getString("content"),
                        formatted
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch paginated messages", e);
        }
        return messages;
    }

    /**
     * Inserts a new message.
     *
     * @param chatId chat identifier
     * @param role   message role
     * @param content message text
     */
    public void insertMessage(String chatId, String role, String content) {
        String sql = "INSERT INTO messages (chatId, role, content, timestamp) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, chatId);
            stmt.setString(2, role);
            stmt.setString(3, content);
            stmt.setLong(4, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert message", e);
        }
    }

    /**
     * Updates an existing message.
     *
     * @param id      message identifier
     * @param content new content
     */
    public void updateMessage(long id, String content) {
        String sql = "UPDATE messages SET content = ? WHERE rowid = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, content);
            stmt.setLong(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update message", e);
        }
    }

    /**
     * Deletes a stored message.
     *
     * @param id message identifier
     */
    public void deleteMessage(long id) {
        String sql = "DELETE FROM messages WHERE rowid = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete message", e);
        }
    }
}