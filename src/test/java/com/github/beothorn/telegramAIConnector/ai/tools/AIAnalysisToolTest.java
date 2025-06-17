package com.github.beothorn.telegramAIConnector.ai.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.model.ChatModel;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AIAnalysisToolTest {
    @TempDir
    Path folder;

    @Test
    void analyzeImageMissingFile() {
        ChatModel model = mock(ChatModel.class);
        AIAnalysisTool tool = new AIAnalysisTool(model, folder.toString());
        String msg = tool.analyzeImage("missing.png", "do it");
        assertTrue(msg.contains("not found"));
    }

    @Test
    void analyzeImageInvalidPath() {
        ChatModel model = mock(ChatModel.class);
        AIAnalysisTool tool = new AIAnalysisTool(model, folder.toString());
        String msg = tool.analyzeImage("../bad.png", "do it");
        assertEquals("Invalid file name.", msg);
    }
}
