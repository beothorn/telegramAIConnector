package com.github.beothorn.telegramAIConnector.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class TelegramAIFileUtilsTest {
    @TempDir
    Path folder;

    /**
     * Validates that files outside the parent folder are detected.
     */
    @Test
    void detectsOutsideFile() throws Exception {
        File parent = folder.toFile();
        File child = new File(parent, "a.txt");
        assertFalse(TelegramAIFileUtils.isNotInParentFolder(parent, child));
        File outside = folder.getParent().resolve("evil.txt").toFile();
        assertTrue(TelegramAIFileUtils.isNotInParentFolder(parent, outside));
    }
}
