package com.github.beothorn.telegramAIConnector.tasks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TaskRepositoryTest {
    @TempDir
    Path folder;

    /**
     * Assert a task can be added, that after added it is present, and that it can be deleted.
     */
    @Test
    void addAndDeleteTask() {
        TaskRepository repo = new TaskRepository();
        String url = "jdbc:sqlite:" + folder.resolve("t.db");
        repo.initDatabase(url);

        TaskCommand cmd = new TaskCommand("k",1L,"2000.01.01 00:00","cmd");
        repo.addTask(cmd);
        List<TaskCommand> all = repo.getAll();
        assertEquals(1, all.size());
        assertTrue(repo.deleteTask("k"));
    }
}
