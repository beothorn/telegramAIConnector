package com.github.beothorn.telegramAIConnector.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class UserRepositoryTest {
    @TempDir
    Path tempDir;

    /**
     * Inserts a user record and updates it.
     */
    @Test
    void createAndRetrieveUser() {
        UserRepository repo = new UserRepository();
        String url = "jdbc:sqlite:" + tempDir.resolve("u.db");
        repo.initDatabase(url);

        repo.createOrUpdateUser(1L, "user", "first", "last");
        UserInfo info = repo.getUser(1L);
        assertNotNull(info);
        assertEquals(1L, info.chatId());
        assertEquals("user", info.username());
        assertEquals("first", info.firstName());
        assertEquals("last", info.lastName());

        repo.createOrUpdateUser(1L, "newuser", "f", "l");
        info = repo.getUser(1L);
        assertEquals("newuser", info.username());
    }
}
