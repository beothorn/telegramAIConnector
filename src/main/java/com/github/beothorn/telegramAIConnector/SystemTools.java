package com.github.beothorn.telegramAIConnector;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.i18n.LocaleContextHolder;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

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

    @Tool(description = "Returns the list of files and directories separate by line breaks, given a path")
    public String ls(
            @ToolParam(description = "The directory to list") String path
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

    @Tool(description = "Schedule a reminder message to be sent on a date set in the format 'yyyy.MM.dd HH:mm'")
    public String sendReminder(
            @ToolParam(description = "The reminder message to be sent") String message,
            @ToolParam(description = "The time to trigger the reminder in the format 'yyyy.MM.dd HH:mm'") String dateTime
    ) {

        // test if time is in the past
        Instant reminderDateTime = InstantUtils.parseToInstant(dateTime);
        if (Instant.now().isAfter(reminderDateTime)) {
            return "Can't set reminder to the past, the time now is '" + InstantUtils.formatFromInstant(Instant.now()) + "'";
        }

        taskSchedulerService.schedule(
                message,
                () -> {
                    try {
                        bot.sendMarkdownMessage(chatId, message);
                    } catch (TelegramApiException e) {
                        // too late
                        e.printStackTrace();
                    }
                },
                reminderDateTime
        );
        return "Reminder was registered under the key '" + message + "' at '" + dateTime + "'. Please inform the user.";
    }

    @Tool(description = "Delete a reminder")
    public String deleteReminder(
            @ToolParam(description = "The reminder message to be deleted") String message
    ) {
        return taskSchedulerService.cancel(message).map(task ->
                "The reminder with key '" + task.key() + "' at '" + task.dateTime() +
                        "'. was deleted. Please inform the user."
        ).orElse("Something went wrong, maybe the key '" +
                message + "' is wrong? Check the key, it can also be something else. Please inform the user.");
    }

    @Tool(description = "List the scheduled reminders")
    public String listReminders(
            @ToolParam(description = "The reminder message to be deleted") String message
    ) {
        return taskSchedulerService.listScheduledKeys();
    }

    @Tool(description = "Schedule a command to be sent to the ai assistant after an interval in seconds.")
    public void sendCommandInSeconds(
            @ToolParam(description = "The command to be sent to the ai assistant.") String command,
            @ToolParam(description = "The amount of time to wait before sending the command") int seconds
    ) {
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            List<PersistedMessage> messages = List.of(new PersistedMessage(MessageType.USER.toString(), command));
            String response = aiBotService.prompt(messages, this);
            try {
                bot.sendMarkdownMessage(chatId, response);
            } catch (TelegramApiException e) {
                // too late
                e.printStackTrace();
            }
        }, seconds, TimeUnit.SECONDS);
    }

    @Tool(description = "Send a markdown message to the user asynchronously through telegram")
    public String sendMessage(
            @ToolParam(description = "The message in markdown format") final String message
    ) {
        try {
            bot.sendMarkdownMessage(chatId, message);
        } catch (TelegramApiException e) {

            return "Could not send messag, got error: '" + e.getMessage() + "'.";
        }
        return "Sent message successfully";
    }

    @Tool(description = "Send a file to the user with a given caption")
    public String sendFile(
            @ToolParam(description = "The absolute file path") final String filePath,
            @ToolParam(description = "The caption to show on chat below the file") final String caption
    ) {
        File file = new File(filePath);
        if (!file.exists()) {
            return "'" + file + "' does not exist.";
        }
        if (!file.isFile()) {
            return "'" + file + "' exists but is not a file.";
        }
        try {
            bot.sendFileWithCaption(chatId, filePath, caption);
            return "File '" + filePath + "' sent successfully.";
        } catch (TelegramApiException e) {
            return "Could not send '" + filePath + "' got error: '" + e.getMessage() + "'.";
        }
    }

    @Tool(description = "Returns detailed process information including PID, JVM name, Java version, start time, uptime, number of cores, and memory usage.")
    public String getMetaData() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        long uptimeMillis = runtime.getUptime();
        Instant startTime = Instant.ofEpochMilli(runtime.getStartTime());
        Duration uptime = Duration.ofMillis(uptimeMillis);

        String formattedStartTime = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(startTime);

        String pid = runtime.getName().split("@")[0];

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        long usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);

        return String.format("""
                        Process Info:
                        - PID: %s
                        - JVM Name: %s
                        - Java Version: %s
                        - Start Time: %s
                        - Uptime: %d hours, %d minutes, %d seconds
                        - Available Cores: %d
                        - Used Memory: %d MB
                        - Max Memory: %d MB
                        """,
                pid,
                runtime.getVmName(),
                System.getProperty("java.version"),
                formattedStartTime,
                uptime.toHoursPart(), uptime.toMinutesPart(), uptime.toSecondsPart(),
                availableProcessors,
                usedMemory,
                maxMemory
        );
    }
}