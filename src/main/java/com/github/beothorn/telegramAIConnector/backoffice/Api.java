package com.github.beothorn.telegramAIConnector.backoffice;

import com.github.beothorn.telegramAIConnector.auth.Authentication;
import com.github.beothorn.telegramAIConnector.tasks.TaskCommand;
import com.github.beothorn.telegramAIConnector.tasks.TaskRepository;
import com.github.beothorn.telegramAIConnector.telegram.TelegramAiBot;
import com.github.beothorn.telegramAIConnector.user.MessagesRepository;
import com.github.beothorn.telegramAIConnector.user.StoredMessage;
import com.github.beothorn.telegramAIConnector.user.UserRepository;
import com.github.beothorn.telegramAIConnector.user.profile.UserProfileRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class Api {

    private final TelegramAiBot telegramAiBot;
    private final TaskRepository taskRepository;
    private final MessagesRepository messagesRepository;
    private final Authentication authentication;
    private final UserProfileRepository userProfileRepository;
    private final FileService fileService;
    private final UserRepository userRepository;

    /**
     * Creates a REST API with required dependencies.
     *
     * @param telegramAiBot         bot instance
     * @param taskRepository        repository for tasks
     * @param messagesRepository    repository for messages
     * @param authentication        authentication service
     * @param userProfileRepository repository for user profiles
     * @param fileService           file service
     */
    public Api(
        final TelegramAiBot telegramAiBot,
        final TaskRepository taskRepository,
        final MessagesRepository messagesRepository,
        final Authentication authentication,
        final UserProfileRepository userProfileRepository,
        final FileService fileService,
        final UserRepository userRepository
    ) {
        this.telegramAiBot = telegramAiBot;
        this.taskRepository = taskRepository;
        this.messagesRepository = messagesRepository;
        this.authentication = authentication;
        this.userProfileRepository = userProfileRepository;
        this.fileService = fileService;
        this.userRepository = userRepository;
    }

    /**
     * Sends a system message to a specific chat.
     *
     * @param chatId  target chat identifier
     * @param message system message
     * @return AI response
     * @throws TelegramApiException if sending fails
     */
    @PostMapping("/systemMessage")
    public String systemMessage(
        @RequestParam("chatId") final Long chatId,
        @RequestParam("message") final String message
    ) throws TelegramApiException {
        return telegramAiBot.consumeSystemMessage(chatId, message);
    }

    /**
     * Sends a plain text message to every known conversation.
     *
     * @param message text to broadcast
     */
    @PostMapping("/broadcast")
    public void broadcast(@RequestParam("message") final String message) {
        for (String id : messagesRepository.findConversationIds()) {
            telegramAiBot.sendMessage(Long.parseLong(id), message);
        }
    }

    /**
     * Sends an anonymous prompt to the bot.
     * This will use chat id 0.
     *
     * @param message prompt text
     * @return AI response
     */
    @PostMapping("/prompt")
    public String prompt(
        @RequestParam("message") final String message
    ) {
        return telegramAiBot.consumeAnonymousMessage(message);
    }

    /**
     * Sends an anonymous prompt to the bot.
     * This will use chat id 0.
     *
     * @param message prompt text
     * @return AI response
     */
    @PostMapping("/prompt/{chatId}")
    public String prompt(
        @PathVariable final Long chatId,
        @RequestBody final String message
    ) {
        return telegramAiBot.consumeMessage(chatId, message);
    }

    /**
     * Returns all scheduled tasks.
     * The scheduled tasks are future tasks that execute a command for some chat id.
     * For example, reminders.
     *
     * @return list of tasks
     */
    @GetMapping("/tasks")
    public List<TaskCommand> getAllTasks() {
        return taskRepository.getAll();
    }

    /**
     * Adds a new scheduled task.
     *
     * @param taskCommand task to schedule
     */
    @PostMapping("/tasks")
    public void addTask(@RequestBody TaskCommand taskCommand) {
        taskRepository.addTask(taskCommand);
    }

    /**
     * Deletes a task.
     * The scheduled tasks are future tasks that execute a command for some chat id.
     * For example, reminders.
     *
     * @param key task identifier
     * @return {@code true} if task was removed
     */
    @DeleteMapping("/tasks/{key}")
    public boolean deleteTask(@PathVariable String key) {
        return taskRepository.deleteTask(key);
    }

    /**
     * Lists known conversation ids.
     * Conversations are open chats with a user.
     * Each chat has a unique id.
     * The chat id is used to identify the user, even though it is not the user id.
     * In theory the same user could have two conversation ids, but in practice this do not happen.
     *
     * @return list of conversation identifiers
     */
    @GetMapping("/conversations")
    public List<String> getConversationIds() {
        return messagesRepository.findConversationIds();
    }

    /**
     * Returns all messages of a conversation.
     * The messages are the messages exchanged inside a single chat.
     *
     * @param chatId conversation identifier
     * @return messages stored for that conversation
     */
    @GetMapping("/conversations/{chatId}")
    public List<Message> getConversationMessages(@PathVariable String chatId) {
        return messagesRepository.getConversations(chatId);
    }

    /**
     * Deletes a conversation.
     * A conversation is a single message, from the user or from the assistant.
     *
     * @param chatId conversation identifier
     */
    @DeleteMapping("/conversations/{chatId}")
    public void deleteConversation(@PathVariable String chatId) {
        messagesRepository.deleteByConversationId(chatId);
        long id = Long.parseLong(chatId);
        taskRepository.deleteByChatId(id);
        userProfileRepository.deleteProfile(id);
        authentication.deleteAuthData(id);
        userRepository.deleteUser(id);
        fileService.deleteAll(id);
    }

    /**
     * Sets a password for a chat.
     * After setting, the master password will no longer work.
     *
     * @param chatId  chat identifier
     * @param password new password
     */
    @PostMapping("/conversations/{chatId}/auth")
    public void setPassword(
        @PathVariable final Long chatId,
        @RequestParam("password") final String password
    ) {
        authentication.setPasswordForUser(chatId, password);
    }

    /**
     * Returns paginated messages for a conversation.
     *
     * @param chatId conversation identifier
     * @param page   zero-based page number
     * @return list of stored messages
     */
    @GetMapping("/conversations/{chatId}/messages")
    public List<StoredMessage> paginatedMessages(
        @PathVariable String chatId,
        @RequestParam(defaultValue = "0") int page
    ) {
        int limit = 50;
        int offset = page * limit;
        return messagesRepository.getMessages(chatId, limit, offset);
    }

    /**
     * Adds a new message to a conversation.
     *
     * @param chatId chat identifier
     * @param role   role of the message
     * @param content message content
     */
    @PostMapping("/conversations/{chatId}/messages")
    public void addMessage(
        @PathVariable String chatId,
        @RequestParam String role,
        @RequestParam String content
    ) {
        messagesRepository.insertMessage(chatId, role, content);
    }

    /**
     * Updates a stored message.
     *
     * @param id      message identifier
     * @param content new content
     */
    @PutMapping("/conversations/{chatId}/messages/{id}")
    public void updateMessage(
        @PathVariable long id,
        @RequestParam String content
    ) {
        messagesRepository.updateMessage(id, content);
    }

    /**
     * Deletes a stored message.
     *
     * @param id message identifier
     */
    @DeleteMapping("/conversations/{chatId}/messages/{id}")
    public void deleteMessage(@PathVariable long id) {
        messagesRepository.deleteMessage(id);
    }

    /**
     * Retrieves a user profile.
     * A user profile is a text appended to the prompt to customize user preferences.
     * The user or the assistant are free to edit this profile.
     *
     * @param chatId chat identifier
     * @return stored profile or empty string
     */
    @GetMapping("/profile/{chatId}")
    public String getProfile(@PathVariable long chatId) {
        return userProfileRepository.getProfile(chatId).orElse("");
    }

    /**
     * Sets a user profile.
     * A user profile is a text appended to the prompt to customize user preferences.
     * The user or the assistant are free to edit this profile.
     *
     * @param chatId  chat identifier
     * @param profile new profile text
     */
    @PostMapping("/profile/{chatId}")
    public void setProfile(
        @PathVariable long chatId,
        @RequestParam String profile
    ) {
        userProfileRepository.setProfile(chatId, profile);
    }

    /**
     * Lists tasks for a chat.
     * Tasks are commands to be executed in the future, for example a reminder.
     *
     * @param chatId chat identifier
     * @return tasks belonging to the chat
     */
    @GetMapping("/tasks/{chatId}")
    public List<TaskCommand> tasksForChat(@PathVariable long chatId) {
        return taskRepository.findByChatId(chatId);
    }

    /**
     * Lists uploaded files for a chat.
     *
     * @param chatId chat identifier
     * @return list of files
     */
    @GetMapping("/files/{chatId}")
    public List<String> listFiles(@PathVariable long chatId) {
        return fileService.list(chatId);
    }

    /**
     * Downloads an uploaded file.
     *
     * @param chatId chat identifier
     * @param name   file name
     * @return HTTP response with the file or 404 if not found
     */
    @GetMapping("/files/{chatId}/{name}")
    public ResponseEntity<Resource> download(
        @PathVariable long chatId,
        @PathVariable String name
    ) {
        Resource r = fileService.download(chatId, name);
        if (r == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
            .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=" + name)
            .body(r);
    }

    /**
     * Uploads a file to the chat folder.
     *
     * @param chatId chat identifier
     * @param file   multipart file to save
     * @throws Exception if upload fails
     */
    @PostMapping("/files/{chatId}")
    public void upload(
        @PathVariable long chatId,
        @RequestParam("file") MultipartFile file
    ) throws Exception {
        fileService.upload(chatId, file);
    }

    /**
     * Renames an uploaded file.
     *
     * @param chatId  chat identifier
     * @param oldName old file name
     * @param newName new file name
     * @throws Exception if renaming fails
     */
    @PostMapping("/files/{chatId}/rename")
    public void rename(
        @PathVariable long chatId,
        @RequestParam String oldName,
        @RequestParam String newName
    ) throws Exception {
        fileService.rename(chatId, oldName, newName);
    }

    /**
     * Deletes a file from the chat folder.
     *
     * @param chatId chat identifier
     * @param name   file name to delete
     */
    @DeleteMapping("/files/{chatId}/{name}")
    public void deleteFile(
        @PathVariable long chatId,
        @PathVariable String name
    ) {
        fileService.delete(chatId, name);
    }
}