package com.github.beothorn.telegramAIConnector.tasks;

import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class TaskRepository {

    private final String dbUrl;

    public TaskRepository() {
        // TODO: Single db with message history
        String userDir = System.getProperty("user.dir");
        this.dbUrl = "jdbc:sqlite:" + userDir + "/tasks.db";
        initDatabase();
    }

    private void initDatabase() {
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS tasks (
                    key TEXT PRIMARY KEY,
                    dateTime TEXT NOT NULL,
                    command TEXT NOT NULL
                )
            """);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    public List<TaskCommand> getAll() {
        List<TaskCommand> tasks = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT key, dateTime, command FROM tasks")) {

            while (rs.next()) {
                tasks.add(new TaskCommand(
                        rs.getString("key"),
                        rs.getString("dateTime"),
                        rs.getString("command")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch tasks", e);
        }
        return tasks;
    }

    public void addTask(TaskCommand taskCommand) {
        String sql = "INSERT OR REPLACE INTO tasks (key, dateTime, command) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, taskCommand.key());
            stmt.setString(2, taskCommand.dateTime());
            stmt.setString(3, taskCommand.command());
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to add task", e);
        }
    }

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