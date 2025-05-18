package com.github.beothorn.telegramAIConnector.telegram;

import com.github.beothorn.telegramAIConnector.ai.tools.SystemTools;
import com.github.beothorn.telegramAIConnector.tasks.TaskScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

@Service
public class Commands {

    private final Logger logger = LoggerFactory.getLogger(Commands.class);

    private final SystemTools systemTools;
    private final TaskScheduler taskScheduler;
    private final ToolCallbackProvider toolCallbackProvider;
    private final String uploadFolder;

    public Commands(
            final TaskScheduler taskScheduler,
            final ToolCallbackProvider toolCallbackProvider,
            @Value("${telegramIAConnector.uploadFolder}") final String uploadFolder
    ) {
        this.taskScheduler = taskScheduler;
        this.toolCallbackProvider = toolCallbackProvider;
        this.uploadFolder = uploadFolder;
        this.systemTools = new SystemTools();
    }

    public String getVersion() {
        try {
            return systemTools.getVersion();
        } catch (IOException e) {
            logger.error("Could not get version.", e);
            return "Could not get version.";
        }
    }

    public String getCurrentDateTime() {
        return systemTools.getCurrentDateTime();
    }

    public String listUploadedFiles(
        final Long chatId
    ) {
        final TelegramTools telegramTools = new TelegramTools(
                null,
                taskScheduler,
                chatId,
                uploadFolder
        );
        return telegramTools.listUploadedFiles();
    }

    public String delete(
        final Long chatId,
        final String file
    ) {
        final TelegramTools telegramTools = new TelegramTools(
                null,
                taskScheduler,
                chatId,
                uploadFolder
        );
        return telegramTools.deleteFile(file);
    }

    public String read(Long chatId, String file) {
        final TelegramTools telegramTools = new TelegramTools(
                null,
                taskScheduler,
                chatId,
                uploadFolder
        );
        return telegramTools.readFile(file);
    }

    public String download(
        final TelegramAiBot telegramAiBot,
        final Long chatId,
        final String args
    ) {
        final TelegramTools telegramTools = new TelegramTools(
                telegramAiBot,
                taskScheduler,
                chatId,
                uploadFolder
        );
        return telegramTools.sendFile(args, "");
    }

    public String listTasks(
        final Long chatId
    ) {
        return taskScheduler.listScheduledKeys(chatId);
    }

    public String listTools() {
        return Arrays.stream(toolCallbackProvider.getToolCallbacks())
                .map(ToolCallback::getToolDefinition)
                .map(t -> t.name() + "\n\t" + t.description() + "\n\t" + t.inputSchema())
                .collect(Collectors.joining("\n"));
    }
}
