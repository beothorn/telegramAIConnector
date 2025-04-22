package com.github.beothorn.telegramAIConnector.tools;

import com.github.beothorn.telegramAIConnector.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TelegramTools {

    private final TelegramAiBot bot;
    private final Long chatId;
    private final AiBotService aiBotService;
    private final TaskSchedulerService taskSchedulerService;
    private final String uploadFolder;

    public TelegramTools(
            final TelegramAiBot bot,
            final AiBotService aiBotService,
            final TaskSchedulerService taskSchedulerService,
            final Long chatId,
            final String uploadFolder
    ) {
        this.bot = bot;
        this.aiBotService = aiBotService;
        this.taskSchedulerService = taskSchedulerService;
        this.chatId = chatId;
        this.uploadFolder = uploadFolder;
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
            String response = aiBotService.prompt(chatId, "Scheduled: " + command, this, new SystemTools());
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

    @Tool(description = "Get the folder where the user uploads their files")
    public String getUploadFolder() {
        return uploadFolder;
    }
}
