package com.github.beothorn.telegramAIConnector.persistence;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.*;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class MessagesRepository implements ChatMemoryRepository {

    private static final Logger logger = LoggerFactory.getLogger(MessagesRepository.class);

    private String dbUrl;

    public void initDatabase(
        final String dbUrl
    ) {
        this.dbUrl = dbUrl;
        try (Connection conn = DriverManager.getConnection(dbUrl);  Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS messages (
                    chatId TEXT NOT NULL,
                    messageIndex INTEGER NOT NULL,
                    role TEXT NOT NULL,
                    content TEXT NOT NULL,
                    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (chatId, messageIndex)
                )
            """);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize messages database", e);
        }
    }

    @Override
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
    public List<Message> findByConversationId(
        @NotNull final String conversationId
    ) {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT messageIndex, role, content FROM messages WHERE chatId = ? ORDER BY messageIndex ASC")) {

            stmt.setString(1, conversationId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {

                String role = rs.getString("role");
                String text = rs.getString("content");

                Message message = null;

                if (role.equals(MessageType.USER.getValue())) {
                    message = new UserMessage(text);
                }

                if (role.equals(MessageType.ASSISTANT.getValue())) {
                    message = new AssistantMessage(text);
                }

                if (role.equals(MessageType.SYSTEM.getValue())) {
                    message = new SystemMessage(text);
                }

                if (role.equals(MessageType.TOOL.getValue())) {
                    message = new SystemMessage(text); // for now, for using tool tables need to be modified
                }

                if (message != null) {
                    messages.add(message);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch messages", e);
        }
        return messages;
    }

    @Override
    public void saveAll(
        @NotNull final String conversationId,
        final List<Message> messages
    ) {
        final String sql = "INSERT OR REPLACE INTO messages (chatId, messageIndex, role, content, timestamp) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < messages.size(); i++) {
                Message message = messages.get(i);
                stmt.setString(1, conversationId);
                stmt.setInt(2, i);
                stmt.setString(3, message.getMessageType().getValue());
                stmt.setString(4, message.getText());
                stmt.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
                stmt.addBatch();
            }

            stmt.executeBatch();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save messages", e);
        }
    }

    @Override
    public void deleteByConversationId(
        @NotNull final String conversationId
    ) {
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM messages WHERE chatId = ?")) {

            stmt.setString(1, conversationId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete conversation messages", e);
        }
    }
}
