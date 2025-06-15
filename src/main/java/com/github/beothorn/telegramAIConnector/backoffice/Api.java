package com.github.beothorn.telegramAIConnector.backoffice;

import com.github.beothorn.telegramAIConnector.auth.Authentication;
import com.github.beothorn.telegramAIConnector.tasks.TaskCommand;
import com.github.beothorn.telegramAIConnector.tasks.TaskRepository;
import com.github.beothorn.telegramAIConnector.telegram.TelegramAiBot;
import com.github.beothorn.telegramAIConnector.user.MessagesRepository;
import com.github.beothorn.telegramAIConnector.user.StoredMessage;
import com.github.beothorn.telegramAIConnector.user.profile.UserProfileRepository;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.ai.chat.messages.Message;
import org.springframework.web.bind.annotation.*;
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

    public Api(
        final TelegramAiBot telegramAiBot,
        final TaskRepository taskRepository,
        final MessagesRepository messagesRepository,
        final Authentication authentication,
        final UserProfileRepository userProfileRepository,
        final FileService fileService
    ) {
        this.telegramAiBot = telegramAiBot;
        this.taskRepository = taskRepository;
        this.messagesRepository = messagesRepository;
        this.authentication = authentication;
        this.userProfileRepository = userProfileRepository;
        this.fileService = fileService;
    }

    /**
     * Sends a system message to a specific chat.
     */
    @PostMapping("/systemMessage")
    public String systemMessage(
        @RequestParam("chatId") final Long chatId,
        @RequestParam("message") final String message
    ) throws TelegramApiException {
        return telegramAiBot.consumeSystemMessage(chatId, message);
    }

    /**
     * Sends an anonymous prompt to the bot.
     */
    @PostMapping("/prompt")
    public String prompt(
        @RequestParam("message") final String message
    ) throws TelegramApiException {
        return telegramAiBot.consumeAnonymousMessage(message);
    }

    /**
     * Returns all scheduled tasks.
     */
    @GetMapping("/tasks")
    public List<TaskCommand> getAllTasks() {
        return taskRepository.getAll();
    }

    /**
     * Adds a new scheduled task.
     */
    @PostMapping("/tasks")
    public void addTask(@RequestBody TaskCommand taskCommand) {
        taskRepository.addTask(taskCommand);
    }

    /**
     * Deletes a task.
     */
    @DeleteMapping("/tasks/{key}")
    public boolean deleteTask(@PathVariable String key) {
        return taskRepository.deleteTask(key);
    }

    /**
     * Lists known conversation ids.
     */
    @GetMapping("/conversations")
    public List<String> getConversationIds() {
        return messagesRepository.findConversationIds();
    }

    /**
     * Returns all messages of a conversation.
     */
    @GetMapping("/conversations/{chatId}")
    public List<Message> getConversationMessages(@PathVariable String chatId) {
        return messagesRepository.getConversations(chatId);
    }

    /**
     * Deletes a conversation.
     */
    @DeleteMapping("/conversations/{chatId}")
    public void deleteConversation(@PathVariable String chatId) {
        messagesRepository.deleteByConversationId(chatId);
    }

    /**
     * Sets a password for a chat.
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
     */
    @DeleteMapping("/conversations/{chatId}/messages/{id}")
    public void deleteMessage(@PathVariable long id) {
        messagesRepository.deleteMessage(id);
    }

    /**
     * Retrieves a user profile.
     */
    @GetMapping("/profile/{chatId}")
    public String getProfile(@PathVariable long chatId) {
        return userProfileRepository.getProfile(chatId).orElse("");
    }

    /**
     * Sets a user profile.
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
     */
    @GetMapping("/tasks/{chatId}")
    public List<TaskCommand> tasksForChat(@PathVariable long chatId) {
        return taskRepository.findByChatId(chatId);
    }

    /**
     * Lists uploaded files for a chat.
     */
    @GetMapping("/files/{chatId}")
    public List<String> listFiles(@PathVariable long chatId) {
        return fileService.list(chatId);
    }

    /**
     * Downloads an uploaded file.
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
     */
    @DeleteMapping("/files/{chatId}/{name}")
    public void deleteFile(
        @PathVariable long chatId,
        @PathVariable String name
    ) {
        fileService.delete(chatId, name);
    }
}