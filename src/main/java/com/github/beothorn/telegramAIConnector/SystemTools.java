package com.github.beothorn.telegramAIConnector;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.i18n.LocaleContextHolder;

class SystemTools {

    private final TelegramAiBot bot;
    private final Long chatId;
    private final AiBotService aiBotService;
    private final TaskSchedulerService taskSchedulerService;

    public SystemTools(
        final TelegramAiBot bot,
        final AiBotService aiBotService,
        final TaskSchedulerService taskSchedulerService,
        final Long chatId
    ) {
        this.bot = bot;
        this.aiBotService = aiBotService;
        this.taskSchedulerService = taskSchedulerService;
        this.chatId = chatId;
    }

    @Tool(description = "Get the current date and time in the format Year.Month.Day Hour:Minute")
    public String getCurrentDateTime() {
        Locale locale = LocaleContextHolder.getLocale();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", locale);
        return LocalDateTime.now().format(formatter);
    }

    @Tool(description ="Returns the list of files and directories separate by line breaks, given a path")
    public String ls(
        @ToolParam(description ="The directory to list") String path
    ) {
        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) {
            return "Invalid directory: " + path;
        }
        StringBuilder result = new StringBuilder();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                result.append(file.getName()).append("\n");
            }
        }
        return result.toString().trim();
    }

    @Tool(description ="Schedule a reminder message to be sent on a date set in the format 'yyyy.MM.dd HH:mm'")
    public void sendReminder(
        @ToolParam(description ="The reminder message to be sent") String message,
        @ToolParam(description ="The time to trigger the reminder in the format 'yyyy.MM.dd HH:mm'") String dateTime
    ) {
        taskSchedulerService.schedule(
            message,
            () -> bot.sendMarkdownMessage(chatId, message),
            dateTime
        );
    }

    @Tool(description ="Delete a reminder")
    public void deleteReminder(
        @ToolParam(description ="The reminder message to be deleted") String message
    ) {
        taskSchedulerService.cancel(message);
    }

    @Tool(description ="List the scheduled reminders")
    public String listReminders(
            @ToolParam(description ="The reminder message to be deleted") String message
    ) {
        return taskSchedulerService.listScheduledKeys();
    }

    @Tool(description ="Schedule a command to be sent to the ai assistant after an interval in seconds.")
    public void sendCommandInSeconds(
            @ToolParam(description ="The command to be sent to the ai assistant.") String command,
            @ToolParam(description ="The amount of time to wait before sending the command") int seconds
    ) {
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            List<PersistedMessage> messages = List.of(new PersistedMessage(MessageType.USER.toString(), command));
            String response = aiBotService.prompt(messages, this);
            bot.sendMarkdownMessage(chatId, response);
        }, seconds, TimeUnit.SECONDS);
    }

    @Tool(description = "Send a markdown message to the user asynchronously through telegram")
    void sendMessage(
        @ToolParam(description = "The message in markdown format") final String message
    ) {
        bot.sendMarkdownMessage(chatId, message);
    }

    @Tool(description = "Send a file to the user with a given caption")
    void sendFile(
            @ToolParam(description = "The absolute file path") final String filePath,
            @ToolParam(description = "The caption to show on chat below the file") final String caption
    ) {
        bot.sendFileWithCaption(chatId, filePath, caption);
    }
}