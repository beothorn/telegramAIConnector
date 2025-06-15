package com.github.beothorn.telegramAIConnector.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class AuthenticationRepositoryTest {
    @TempDir
    Path folder;

    @Test
    void addAndRetrieve() {
        AuthenticationRepository repo = new AuthenticationRepository();
        String url = "jdbc:sqlite:" + folder.resolve("a.db");
        repo.initDatabase(url);

        repo.addAuthEntry(1L,"hash",false,"d");
        Optional<AuthData> data = repo.getAuthData(1L);
        assertTrue(data.isPresent());
        assertEquals("hash", data.get().password_hash());

        repo.setLoggedState(1L,true,"t");
        data = repo.getAuthData(1L);
        assertTrue(data.orElseThrow().logged());
    }
}
