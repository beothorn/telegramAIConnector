package com.github.beothorn.telegramAIConnector.tools;

import com.github.beothorn.telegramAIConnector.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
        this.uploadFolder = uploadFolder + "/" + chatId;
    }

    @Tool(description = "Schedule a reminder message to be sent on a date set in the format 'yyyy.MM.dd HH:mm'.")
    public String sendReminder(
        @ToolParam(description = "The reminder message to be sent") final String message,
        @ToolParam(description = "The time to trigger the reminder in the format 'yyyy.MM.dd HH:mm'") final String dateTime
    ) {

        // test if time is in the past
        final Instant reminderDateTime = InstantUtils.parseToInstant(dateTime);
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
        @ToolParam(description = "The reminder message to be deleted") final String message
    ) {
        return taskSchedulerService.cancel(message).map(task ->
                "The reminder with key '" + task.key() + "' at '" + task.dateTime() +
                        "'. was deleted. Please inform the user."
        ).orElse("Something went wrong, maybe the key '" +
                message + "' is wrong? Check the key, it can also be something else. Please inform the user.");
    }

    @Tool(description = "List the scheduled reminders")
    public String listReminders() {
        return taskSchedulerService.listScheduledKeys(chatId);
    }

    @Tool(description = "Schedule a command to be sent to the ai assistant after an interval in minutes.")
    public void sendCommandInMinutes(
        @ToolParam(description = "The command to be sent to the ai assistant.") final String command,
        @ToolParam(description = "The amount of minutes to wait before sending the command") final int minutes
    ) {
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            final String response = aiBotService.prompt(chatId, "Scheduled: " + command, this, new SystemTools());
            try {
                bot.sendMarkdownMessage(chatId, response);
            } catch (TelegramApiException e) {
                // too late
                e.printStackTrace();
            }
        }, minutes, TimeUnit.MINUTES);
    }

    @Tool(description = "Send a markdown message to the user asynchronously through telegram")
    public String sendMessage(
        @ToolParam(description = "The message in markdown format") final String message
    ) {
        try {
            bot.sendMarkdownMessage(chatId, message);
        } catch (TelegramApiException e) {
            return "Could not send message, got error: '" + e.getMessage() + "'.";
        }
        return "Sent message successfully";
    }

    @Tool(description = "Send a file from the Telegram upload folder to the user with a given caption")
    public String sendFile(
        @ToolParam(description = "The file name") final String fileName,
        @ToolParam(description = "The caption to show on chat below the file") final String caption
    ) {
        File file = new File(uploadFolder + "/" + fileName);
        File parent = new File(uploadFolder);
        if (FileUtils.isInvalid(parent, file)) {
            return "'" + fileName + "' is not a valid file name.";
        }
        if (!file.exists()) {
            return "'" + fileName + "' does not exist.";
        }
        if (!file.isFile()) {
            return "'" + fileName + "' exists but is not a file.";
        }
        try {
            bot.sendFileWithCaption(chatId, file.getAbsolutePath(), caption);
            return "File '" + fileName + "' sent successfully.";
        } catch (TelegramApiException e) {
            return "Could not send '" + fileName + "' got error: '" + e.getMessage() + "'.";
        }
    }

    @Tool(description = "Returns the list of files inside Telegram upload folder")
    public String listUploadedFiles() {
        File dir = new File(uploadFolder);
        if (!dir.exists() || !dir.isDirectory()) {
            return "No file was uploaded yet.";
        }
        StringBuilder result = new StringBuilder();
        File[] files = dir.listFiles();
        if (files == null) {
            return "There are no files on Telegram upload folder.";
        }
        if (files.length == 0) {
            return "There are no files on Telegram upload folder.";
        }
        for (File file : files) {
            result.append(file.getName()).append("\n");
        }
        return "The files are:\n" + result;
    }

    @Tool(description = "Deletes a file inside Telegram upload folder")
    public String deleteFile(
        @ToolParam(description = "The file name to delete. Make sure it is the right file.") final String fileName
    ) {
        final File toBeDeleted = new File(uploadFolder + "/" + fileName);
        File parent = new File(uploadFolder);
        if (FileUtils.isInvalid(parent, toBeDeleted)) {
            return "'" + fileName + "' is not a valid file name.";
        }
        if (!toBeDeleted.exists() || toBeDeleted.isDirectory()) {
            return "This file does not exist.";
        }
        boolean deleted = toBeDeleted.delete();
        if (deleted) {
            return "The file '" + fileName + "' was deleted.";
        } else {
            return "The file '" + fileName + "' was not deleted.";
        }
    }

    @Tool(description = "Reads the text contents of a file inside Telegram upload folder")
    public String readFile(
        @ToolParam(description = "The file name to be read.") final String fileName
    ) {
        final File toBeRead = new File(uploadFolder + "/" + fileName);
        File parent = new File(uploadFolder);
        if (FileUtils.isInvalid(parent, toBeRead)) {
            return "'" + fileName + "' is not a valid file name.";
        }
        try {
            return Files.readString(toBeRead.toPath());
        } catch (IOException e) {
            return "Failed to read file '" + fileName + "' because " + e.getMessage() + ".";
        }
    }
}
