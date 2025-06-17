package com.github.beothorn.telegramAIConnector.ai.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.model.ChatModel;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * The tool is used by the AI.
 */
public class AIAnalysisToolTest {
    @TempDir
    Path folder;

    /**
     * Asserts that missing file returns a message saying that the file was not found.
     */
    @Test
    void analyzeImageMissingFile() {
        ChatModel model = mock(ChatModel.class);
        AIAnalysisTool tool = new AIAnalysisTool(model, folder.toString());
        String msg = tool.analyzeImage("missing.png", "do it");
        assertTrue(msg.contains("not found"));
    }

    /**
     * Asserts that the user cannot try to escape the chat id folder by using a relative file path.
     */
    @Test
    void analyzeImageInvalidPath() {
        ChatModel model = mock(ChatModel.class);
        AIAnalysisTool tool = new AIAnalysisTool(model, folder.toString());
        String msg = tool.analyzeImage("../bad.png", "do it");
        assertEquals("Invalid file name.", msg);
    }
}
