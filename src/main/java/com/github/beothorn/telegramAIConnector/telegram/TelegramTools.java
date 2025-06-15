package com.github.beothorn.telegramAIConnector.telegram;

import com.github.beothorn.telegramAIConnector.tasks.TaskScheduler;
import com.github.beothorn.telegramAIConnector.utils.InstantUtils;
import com.github.beothorn.telegramAIConnector.utils.TelegramAIFileUtils;
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

    /**
     * Creates a helper bound to a specific chat.
     *
     * @param telegramAiBot  telegram bot instance
     * @param taskScheduler  scheduler to use for reminders
     * @param chatId         chat identifier
     * @param uploadFolder   base folder for uploaded files
     */
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

    /**
     * Schedules a reminder message for the given date.
     *
     * @param message  reminder text
     * @param dateTime date and time formatted as {@code yyyy.MM.dd HH:mm}
     * @return confirmation string to show the user
     */
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

    /**
     * Deletes a previously scheduled reminder.
     *
     * @param message key of the reminder to remove
     * @return status message for the user
     */
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

    /**
     * Lists all reminders for this chat.
     *
     * @return human-readable descriptions of scheduled reminders
     */
    @Tool(description = "List the scheduled reminders")
    public String listReminders() {
        return taskScheduler.listScheduledKeys(chatId);
    }

    /**
     * Sends a markdown message asynchronously to the chat.
     *
     * @param message markdown text to send
     * @return status of the send operation
     */
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

    /**
     * Sends an uploaded file with a caption to the user.
     *
     * @param fileName name of the file in the chat upload folder
     * @param caption  caption to send along the file
     * @return operation result message
     */
    @Tool(description = "Send a file from the Telegram upload folder to the user with a given caption")
    public String sendFile(
        @ToolParam(description = "The file name") final String fileName,
        @ToolParam(description = "The caption to show on chat below the file") final String caption
    ) {
        File file = new File(uploadFolder + "/" + fileName);
        File parent = new File(uploadFolder);
        if (TelegramAIFileUtils.isNotInParentFolder(parent, file)) {
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

    /**
     * Lists files uploaded by this chat.
     *
     * @return newline separated list of files
     */
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

    /**
     * Resolves the absolute path for a file inside the chat upload folder.
     *
     * @param fileName file name to resolve
     * @return absolute file path or an error message
     */
    @Tool(description = "Returns the full path for a file inside Telegram upload folder.")
    public String getFileFullPath(
        @ToolParam(description = "The file name to return the full path.") final String fileName
    ) {
        final File file = new File(uploadFolder + "/" + fileName);
        File parent = new File(uploadFolder);
        if (TelegramAIFileUtils.isNotInParentFolder(parent, file)) {
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

    /**
     * Deletes an uploaded file.
     *
     * @param fileName name of the file to delete
     * @return result of the deletion attempt
     */
    @Tool(description = "Deletes a file inside Telegram upload folder")
    public String deleteFile(
        @ToolParam(description = "The file name to delete. Make sure it is the right file.") final String fileName
    ) {
        final File toBeDeleted = new File(uploadFolder + "/" + fileName);
        File parent = new File(uploadFolder);
        if (TelegramAIFileUtils.isNotInParentFolder(parent, toBeDeleted)) {
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

    /**
     * Renames an uploaded file.
     *
     * @param currentName current file name
     * @param newName     desired new file name
     * @return result message
     */
    @Tool(description = "Rename a file inside Telegram upload folder")
    public String renameFile(
        @ToolParam(description = "The current file name") final String currentName,
        @ToolParam(description = "The new file name") final String newName
    ) {
        final File fromFile = new File(uploadFolder + "/" + currentName);
        final File toFile = new File(uploadFolder + "/" + newName);
        File parent = new File(uploadFolder);

        if (TelegramAIFileUtils.isNotInParentFolder(parent, fromFile) || TelegramAIFileUtils.isNotInParentFolder(parent, toFile)) {
            return "Invalid file name.";
        }

        if (!fromFile.exists() || fromFile.isDirectory()) {
            return "The file '" + currentName + "' does not exist.";
        }

        if (toFile.exists()) {
            return "The file '" + newName + "' already exists.";
        }

        try {
            Files.move(fromFile.toPath(), toFile.toPath());
            return "The file '" + currentName + "' was renamed to '" + newName + "'.";
        } catch (IOException e) {
            return "Failed to rename file '" + currentName + "' to '" + newName + "' because " + e.getMessage() + ".";
        }
    }

    /**
     * Reads the text contents of an uploaded file.
     *
     * @param fileName name of the file to read
     * @return file contents or error message
     */
    @Tool(description = "Reads the text contents of a file inside Telegram upload folder")
    public String readFile(
        @ToolParam(description = "The file name to be read.") final String fileName
    ) {
        final File toBeRead = new File(uploadFolder + "/" + fileName);
        File parent = new File(uploadFolder);
        if (TelegramAIFileUtils.isNotInParentFolder(parent, toBeRead)) {
            return "'" + fileName + "' is not a valid file name.";
        }
        try {
            return Files.readString(toBeRead.toPath());
        } catch (IOException e) {
            return "Failed to read file '" + fileName + "' because " + e.getMessage() + ".";
        }
    }

    /**
     * Saves the given content as a file in the upload folder.
     *
     * @param fileName    name of the destination file
     * @param fileContents text to store
     * @return operation status message
     */
    @Tool(description = "Saves string content as file inside Telegram upload folder")
    public String saveAsFile(
            @ToolParam(description = "The file name to be used.") final String fileName,
            @ToolParam(description = "The file contents.") final String fileContents
    ) {
        final File file = new File(uploadFolder + "/" + fileName);
        File parent = new File(uploadFolder);
        if (TelegramAIFileUtils.isNotInParentFolder(parent, file)) {
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

    /**
     * Sends the provided text as a temporary file.
     *
     * @param fileName    name of the temporary file
     * @param fileContents contents to send
     * @return result of the send operation
     */
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
