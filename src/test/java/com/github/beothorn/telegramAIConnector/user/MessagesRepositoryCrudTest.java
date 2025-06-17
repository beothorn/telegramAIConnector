package com.github.beothorn.telegramAIConnector.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MessagesRepositoryCrudTest {
    @TempDir
    Path tempDir;

    /**
     * Stores a message, updates its content and then deletes it.
     */
    @Test
    void insertUpdateDeleteMessage() {
        MessagesRepository repo = new MessagesRepository(50);
        String url = "jdbc:sqlite:" + tempDir.resolve("m.db");
        repo.initDatabase(url);

        repo.insertMessage("1", "user", "hi");
        List<StoredMessage> msgs = repo.getMessages("1", 50, 0);
        assertEquals(1, msgs.size());

        repo.updateMessage(msgs.get(0).id(), "bye");
        assertEquals("bye", repo.getMessages("1", 50, 0).get(0).content());

        repo.deleteMessage(msgs.get(0).id());
        assertTrue(repo.getMessages("1", 50, 0).isEmpty());
    }
}
