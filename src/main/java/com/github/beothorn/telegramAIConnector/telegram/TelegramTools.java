package com.github.beothorn.telegramAIConnector.telegram;

import com.github.beothorn.telegramAIConnector.tasks.TaskScheduler;
import com.github.beothorn.telegramAIConnector.utils.InstantUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;

public class TelegramTools {

    private final TelegramAiBot telegramAiBot;
    private final Long chatId;
    private final TaskScheduler taskScheduler;
    private final String uploadFolder;

    public TelegramTools(
            final TelegramAiBot telegramAiBot,
            final TaskScheduler taskScheduler,
            final Long chatId,
            final String uploadFolder
    ) {
        this.telegramAiBot = telegramAiBot;
        this.taskScheduler = taskScheduler;
        this.chatId = chatId;
        this.uploadFolder = uploadFolder + "/" + chatId;
    }

    public static boolean isInvalid(
            final File parentFolder,
            final File fileToCreate
    ) {
        try {
            String parentPath = parentFolder.getCanonicalPath();
            String filePath = fileToCreate.getCanonicalPath();
            return !filePath.startsWith(parentPath + File.separator);
        } catch (IOException e) {
            return true;
        }
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

        taskScheduler.schedule(
                telegramAiBot,
            chatId,
            message,
            reminderDateTime
        );
        return "Reminder was registered under the key '" + message + "' at '" + dateTime + "'. Please inform the user.";
    }

    @Tool(description = "Delete a reminder")
    public String deleteReminder(
        @ToolParam(description = "The reminder message to be deleted") final String message
    ) {
        return taskScheduler.cancel(chatId, message).map(task ->
                "The reminder with key '" + task.key() + "' at '" + task.dateTime() +
                        "'. was deleted. Please inform the user."
        ).orElse("Something went wrong, maybe the key '" +
                message + "' is wrong? Check the key, it can also be something else. Please inform the user.");
    }

    @Tool(description = "List the scheduled reminders")
    public String listReminders() {
        return taskScheduler.listScheduledKeys(chatId);
    }

    @Tool(description = "Send a markdown message to the user asynchronously through telegram")
    public String sendMessage(
        @ToolParam(description = "The message in markdown format") final String message
    ) {
        try {
            telegramAiBot.sendMarkdownMessage(chatId, message);
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
        if (isInvalid(parent, file)) {
            return "'" + fileName + "' is not a valid file name.";
        }
        if (!file.exists()) {
            return "'" + fileName + "' does not exist.";
        }
        if (!file.isFile()) {
            return "'" + fileName + "' exists but is not a file.";
        }
        try {
            telegramAiBot.sendFileWithCaption(chatId, file.getAbsolutePath(), caption);
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

    @Tool(description = "Returns the full path for a file inside Telegram upload folder.")
    public String getFileFullPath(
        @ToolParam(description = "The file name to return the full path.") final String fileName
    ) {
        final File file = new File(uploadFolder + "/" + fileName);
        File parent = new File(uploadFolder);
        if (isInvalid(parent, file)) {
            return "'" + fileName + "' is not a valid file name.";
        }
        if (!file.exists() || file.isDirectory()) {
            return "This file does not exist.";
        }
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return "Something went wrong getting the file full path: " + e.getMessage();
        }
    }

    @Tool(description = "Deletes a file inside Telegram upload folder")
    public String deleteFile(
        @ToolParam(description = "The file name to delete. Make sure it is the right file.") final String fileName
    ) {
        final File toBeDeleted = new File(uploadFolder + "/" + fileName);
        File parent = new File(uploadFolder);
        if (isInvalid(parent, toBeDeleted)) {
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
        if (isInvalid(parent, toBeRead)) {
            return "'" + fileName + "' is not a valid file name.";
        }
        try {
            return Files.readString(toBeRead.toPath());
        } catch (IOException e) {
            return "Failed to read file '" + fileName + "' because " + e.getMessage() + ".";
        }
    }

    @Tool(description = "Saves string content as file inside Telegram upload folder")
    public String saveAsFile(
            @ToolParam(description = "The file name to be used.") final String fileName,
            @ToolParam(description = "The file contents.") final String fileContents
    ) {
        final File file = new File(uploadFolder + "/" + fileName);
        File parent = new File(uploadFolder);
        if (isInvalid(parent, file)) {
            return "'" + fileName + "' is not a valid file name.";
        }
        try {
            parent.mkdirs(); // ensure folder exists
            Files.writeString(file.toPath(), fileContents);
            return "File '" + fileName + "' saved successfully.";
        } catch (IOException e) {
            return "Failed to save file '" + fileName + "' because " + e.getMessage() + ".";
        }
    }

    @Tool(description = "Send string as file")
    public String sendAsFile(
            @ToolParam(description = "The file name to be used.") final String fileName,
            @ToolParam(description = "The file contents.") final String fileContents
    ) {
        try {
            File tempFile = File.createTempFile("telegram-temp-", "-" + fileName);
            Files.writeString(tempFile.toPath(), fileContents);
            telegramAiBot.sendFileWithCaption(chatId, tempFile.getAbsolutePath(), "Here is your file: " + fileName);
            boolean deleted = tempFile.delete(); // cleanup
            if (!deleted) {
                tempFile.deleteOnExit(); // ensure deletion on exit if immediate deletion fails
            }
            return "File '" + fileName + "' sent successfully.";
        } catch (IOException | TelegramApiException e) {
            return "Could not send file '" + fileName + "' due to error: " + e.getMessage();
        }
    }
}
