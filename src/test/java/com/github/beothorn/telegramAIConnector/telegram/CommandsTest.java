package com.github.beothorn.telegramAIConnector.telegram;

import org.junit.jupiter.api.Test;
import com.github.beothorn.telegramAIConnector.tasks.TaskScheduler;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.DefaultToolDefinition;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CommandsTest {
    @Test
    void listToolsFormatsOutput() {
        ToolCallback tool1 = mock(ToolCallback.class);
        ToolCallback tool2 = mock(ToolCallback.class);
        when(tool1.getToolDefinition()).thenReturn(
                DefaultToolDefinition.builder().name("t1").description("desc1").inputSchema("input1").build());
        when(tool2.getToolDefinition()).thenReturn(
                DefaultToolDefinition.builder().name("t2").description("desc2").inputSchema("input2").build());
        ToolCallbackProvider provider = mock(ToolCallbackProvider.class);
        when(provider.getToolCallbacks()).thenReturn(new ToolCallback[]{tool1, tool2});

        Commands commands = new Commands(mock(TaskScheduler.class), provider, "folder");
        String result = commands.listTools();

        assertTrue(result.contains("t1"));
        assertTrue(result.contains("desc1"));
        assertTrue(result.contains("input1"));
        assertTrue(result.contains("t2"));
    }
}
