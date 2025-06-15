package com.github.beothorn.telegramAIConnector.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Tasks are commands to be executed in the future.
 * They need to be persisted in case the application is turned off so they can be recovered.
 * Tasks can be, for example, reminders.
 */
@Service
public class TaskRepository {

    private final Logger logger = LoggerFactory.getLogger(TaskRepository.class);

    private String dbUrl;

    /**
     * Initializes the repository using the provided database URL.
     *
     * @param dbUrl JDBC connection string
     */
    public void initDatabase(final String dbUrl) {
        this.dbUrl = dbUrl;
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS tasks (
                    key TEXT PRIMARY KEY,
                    chatId INTEGER NOT NULL,
                    dateTime TEXT NOT NULL,
                    command TEXT NOT NULL
                )
            """);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    /**
     * Retrieves all stored tasks.
     *
     * @return list of scheduled tasks
     */
    public List<TaskCommand> getAll() {
        List<TaskCommand> tasks = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT key, chatId, dateTime, command FROM tasks")) {

            while (rs.next()) {
                tasks.add(new TaskCommand(
                        rs.getString("key"),
                        rs.getLong("chatId"),
                        rs.getString("dateTime"),
                        rs.getString("command")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch tasks", e);
        }
        return tasks;
    }

    /**
     * Retrieves tasks scheduled for a given chat.
     *
     * @param chatId chat identifier
     * @return list of tasks for the chat
     */
    public List<TaskCommand> findByChatId(long chatId) {
        List<TaskCommand> tasks = new ArrayList<>();
        String sql = "SELECT key, chatId, dateTime, command FROM tasks WHERE chatId = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, chatId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                tasks.add(new TaskCommand(
                        rs.getString("key"),
                        rs.getLong("chatId"),
                        rs.getString("dateTime"),
                        rs.getString("command")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch tasks", e);
        }
        return tasks;
    }

    /**
     * Persists a task.
     *
     * @param taskCommand task to store
     */
    public void addTask(TaskCommand taskCommand) {
        String sql = "INSERT OR REPLACE INTO tasks (key, chatId, dateTime, command) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, taskCommand.key());
            stmt.setLong(2, taskCommand.chatId());
            stmt.setString(3, taskCommand.dateTime());
            stmt.setString(4, taskCommand.command());
            stmt.executeUpdate();

        } catch (SQLException e) {
            logger.error("Error adding task to repository.", e);
            throw new RuntimeException("Failed to add task", e);
        }
    }

    /**
     * Deletes a task.
     *
     * @param key task key
     * @return {@code true} if a task was deleted
     */
    public boolean deleteTask(String key) {
        String sql = "DELETE FROM tasks WHERE key = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, key);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete task", e);
        }
    }
}