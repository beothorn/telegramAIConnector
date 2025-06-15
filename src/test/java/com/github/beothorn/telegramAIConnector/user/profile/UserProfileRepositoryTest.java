package com.github.beothorn.telegramAIConnector.user.profile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class UserProfileRepositoryTest {
    @TempDir
    Path folder;

    @Test
    void setAndGet() {
        UserProfileRepository repo = new UserProfileRepository();
        String url = "jdbc:sqlite:" + folder.resolve("p.db");
        repo.initDatabase(url);
        repo.setProfile(1L, "profile");
        assertEquals("profile", repo.getProfile(1L).orElse(""));
    }
}
