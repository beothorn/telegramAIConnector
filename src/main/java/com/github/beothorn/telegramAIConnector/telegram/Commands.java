package com.github.beothorn.telegramAIConnector.telegram;

import com.github.beothorn.telegramAIConnector.ai.tools.SystemTools;
import com.github.beothorn.telegramAIConnector.tasks.TaskScheduler;
import com.github.beothorn.telegramAIConnector.user.profile.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Executes commands using tools.
 * This can be called directly, bypassing the llm.
 */
@Service
public class Commands {

    private final Logger logger = LoggerFactory.getLogger(Commands.class);

    private final SystemTools systemTools;
    private final TaskScheduler taskScheduler;
    private final ToolCallbackProvider toolCallbackProvider;
    private final String uploadFolder;
    private final UserProfileRepository userProfileRepository;

    /**
     * Constructs a helper with the provided dependencies.
     *
     * @param taskScheduler        scheduler used for reminders
     * @param toolCallbackProvider provider of extra tool callbacks
     * @param userProfileRepository repository for user profiles
     * @param uploadFolder         base folder for uploads
     */
    public Commands(
            final TaskScheduler taskScheduler,
            final ToolCallbackProvider toolCallbackProvider,
            final UserProfileRepository userProfileRepository,
            @Value("${telegramIAConnector.uploadFolder}") final String uploadFolder
    ) {
        this.taskScheduler = taskScheduler;
        this.toolCallbackProvider = toolCallbackProvider;
        this.userProfileRepository = userProfileRepository;
        this.uploadFolder = uploadFolder;
        this.systemTools = new SystemTools();
    }

    /**
     * Returns the application version.
     *
     * @return application version string
     */
    public String getVersion() {
        try {
            return systemTools.getVersion();
        } catch (IOException e) {
            logger.error("Could not get version.", e);
            return "Could not get version.";
        }
    }

    /**
     * Returns the current date and time using {@link SystemTools}.
     *
     * @return formatted date and time
     */
    public String getCurrentDateTime() {
        return systemTools.getCurrentDateTime();
    }

    /**
     * Lists uploaded files for the given chat.
     *
     * @param chatId chat identifier
     * @return newline separated list of files
     */
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

    /**
     * Deletes an uploaded file.
     *
     * @param chatId chat identifier
     * @param file   file name to delete
     * @return operation result
     */
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

    /**
     * Reads a file contents.
     *
     * @param chatId chat identifier
     * @param file   file name to read
     * @return file contents or error message
     */
    public String read(Long chatId, String file) {
        final TelegramTools telegramTools = new TelegramTools(
                null,
                taskScheduler,
                chatId,
                uploadFolder
        );
        return telegramTools.readFile(file);
    }

    /**
     * Sends a file to the user.
     *
     * @param telegramAiBot bot instance to use
     * @param chatId        chat identifier
     * @param args          file name
     * @return operation status
     */
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

    /**
     * Lists scheduled tasks for a chat.
     *
     * @param chatId chat identifier
     * @return human-readable list of tasks
     */
    public String listTasks(
        final Long chatId
    ) {
        return taskScheduler.listScheduledKeys(chatId);
    }

    /**
     * Lists the available tool callbacks.
     * This will only list MCPs.
     * The format is json and it is not ver human-readable.
     *
     * @return descriptions of available tools
     */
    public String listTools() {
        return Arrays.stream(toolCallbackProvider.getToolCallbacks())
                .map(ToolCallback::getToolDefinition)
                .map(t -> t.name() + "\n\t" + t.description() + "\n\t" + t.inputSchema())
                .collect(Collectors.joining("\n"));
    }

    /**
     * Gets the stored profile for a chat.
     *
     * @param chatId chat identifier
     * @return stored profile or {@code "No profile."}
     */
    public String getProfile(long chatId) {
        return userProfileRepository.getProfile(chatId).orElse("No profile.");
    }

    /**
     * Updates the user profile for a chat.
     * With this, the user can fully customize the profile, maybe even turn it into a custom prompt.
     *
     * @param chatId  chat identifier
     * @param profile new profile text
     * @return confirmation message
     */
    public String setProfile(long chatId, String profile) {
        userProfileRepository.setProfile(chatId, profile);
        return "Profile updated.";
    }
}
