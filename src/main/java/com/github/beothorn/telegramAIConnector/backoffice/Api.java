package com.github.beothorn.telegramAIConnector.backoffice;

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

    public Api(
            final TelegramAiBot telegramAiBot,
            final TaskRepository taskRepository,
            final MessagesRepository messagesRepository
    ) {
        this.telegramAiBot = telegramAiBot;
        this.taskRepository = taskRepository;
        this.messagesRepository = messagesRepository;
    }

    @PostMapping("/systemMessage")
    public String systemMessage(
        @RequestParam("chatId") final Long chatId,
        @RequestParam("message") final String message
    ) throws TelegramApiException {
        return telegramAiBot.consumeSystemMessage(chatId, message);
    }

    @PostMapping("/chat")
    public String systemMessage(
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
        return messagesRepository.findByConversationId(chatId);
    }

    @DeleteMapping("/conversations/{chatId}")
    public void deleteConversation(@PathVariable String chatId) {
        messagesRepository.deleteByConversationId(chatId);
    }
}