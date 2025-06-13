package com.github.beothorn.telegramAIConnector.backoffice;

import com.github.beothorn.telegramAIConnector.auth.Authentication;
import com.github.beothorn.telegramAIConnector.tasks.TaskCommand;
import com.github.beothorn.telegramAIConnector.tasks.TaskRepository;
import com.github.beothorn.telegramAIConnector.telegram.TelegramAiBot;
import com.github.beothorn.telegramAIConnector.user.MessagesRepository;
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

    public Api(
        final TelegramAiBot telegramAiBot,
        final TaskRepository taskRepository,
        final MessagesRepository messagesRepository,
        final Authentication authentication
    ) {
        this.telegramAiBot = telegramAiBot;
        this.taskRepository = taskRepository;
        this.messagesRepository = messagesRepository;
        this.authentication = authentication;
    }

    @PostMapping("/systemMessage")
    public String systemMessage(
        @RequestParam("chatId") final Long chatId,
        @RequestParam("message") final String message
    ) throws TelegramApiException {
        return telegramAiBot.consumeSystemMessage(chatId, message);
    }

    @PostMapping("/prompt")
    public String prompt(
        @RequestParam("message") final String message
    ) throws TelegramApiException {
        return telegramAiBot.consumeAnonymousMessage(message);
    }

    @GetMapping("/tasks")
    public List<TaskCommand> getAllTasks() {
        return taskRepository.getAll();
    }

    @PostMapping("/tasks")
    public void addTask(@RequestBody TaskCommand taskCommand) {
        taskRepository.addTask(taskCommand);
    }

    @DeleteMapping("/tasks/{key}")
    public boolean deleteTask(@PathVariable String key) {
        return taskRepository.deleteTask(key);
    }

    @GetMapping("/conversations")
    public List<String> getConversationIds() {
        return messagesRepository.findConversationIds();
    }

    @GetMapping("/conversations/{chatId}")
    public List<Message> getConversationMessages(@PathVariable String chatId) {
        return messagesRepository.getConversations(chatId);
    }

    @DeleteMapping("/conversations/{chatId}")
    public void deleteConversation(@PathVariable String chatId) {
        messagesRepository.deleteByConversationId(chatId);
    }

    @PostMapping("/conversations/{chatId}/auth")
    public void setPassword(
        @PathVariable final Long chatId,
        @RequestParam("password") final String password
    ) {
        authentication.setPasswordForUser(chatId, password);
    }
}