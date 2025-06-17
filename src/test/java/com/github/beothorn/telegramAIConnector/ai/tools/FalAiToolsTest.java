package com.github.beothorn.telegramAIConnector.ai.tools;

import ai.fal.client.FalClient;
import com.github.beothorn.telegramAIConnector.telegram.TelegramTools;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class FalAiToolsTest {
    @TempDir
    Path folder;

    @Test
    void editImageReturnsNotFoundForMissingFile() {
        FalAiTools tools = new FalAiTools(mock(FalClient.class), folder.toString(), mock(TelegramTools.class));
        String msg = tools.editImage("missing.png","p","out.png");
        assertTrue(msg.contains("not found"));
    }
}
