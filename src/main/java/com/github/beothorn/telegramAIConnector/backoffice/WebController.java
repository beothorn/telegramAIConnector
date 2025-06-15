package com.github.beothorn.telegramAIConnector.backoffice;

import com.github.beothorn.telegramAIConnector.tasks.TaskRepository;
import com.github.beothorn.telegramAIConnector.user.MessagesRepository;
import com.github.beothorn.telegramAIConnector.user.StoredMessage;
import com.github.beothorn.telegramAIConnector.user.profile.UserProfileRepository;
import com.github.beothorn.telegramAIConnector.user.UserRepository;
import com.github.beothorn.telegramAIConnector.telegram.TelegramAiBot;
import com.github.beothorn.telegramAIConnector.backoffice.FileService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/backoffice")
public class WebController {

    private final TaskRepository taskRepository;
    private final MessagesRepository messagesRepository;
    private final UserProfileRepository userProfileRepository;
    private final FileService fileService;
    private final UserRepository userRepository;
    private final TelegramAiBot telegramAiBot;

    public WebController(
        final TaskRepository taskRepository,
        final MessagesRepository messagesRepository,
        final UserProfileRepository userProfileRepository,
        final FileService fileService,
        final UserRepository userRepository,
        final TelegramAiBot telegramAiBot
    ) {
        this.taskRepository = taskRepository;
        this.messagesRepository = messagesRepository;
        this.userProfileRepository = userProfileRepository;
        this.fileService = fileService;
        this.userRepository = userRepository;
        this.telegramAiBot = telegramAiBot;
    }

    @GetMapping({"", "/"})
    public String index(Model model) {
        model.addAttribute("botName", telegramAiBot.getBotName());
        var conversationIds = messagesRepository.findConversationIds();
        var conversationInfos = conversationIds.stream().map(id -> {
            var user = userRepository.getUser(Long.parseLong(id));
            if (user == null) {
                return new com.github.beothorn.telegramAIConnector.user.UserInfo(Long.parseLong(id), "", "", "");
            }
            return user;
        }).toList();
        model.addAttribute("conversations", conversationInfos);
        model.addAttribute("tasks", taskRepository.getAll());
        return "backoffice";
    }

    @GetMapping({"/conversations/{chatId}", "/conversations/{chatId}/"})
    public String conversation(
        @PathVariable String chatId,
        @RequestParam(defaultValue = "0") int page,
        Model model
    ) {
        int limit = 50;
        model.addAttribute("chatId", chatId);
        model.addAttribute("user", userRepository.getUser(Long.parseLong(chatId)));
        model.addAttribute("messages", messagesRepository.getMessages(chatId, limit, page * limit));
        model.addAttribute("prevPage", Math.max(page - 1, 0));
        model.addAttribute("nextPage", page + 1);
        model.addAttribute("page", page);
        model.addAttribute("tasks", taskRepository.findByChatId(Long.parseLong(chatId)));
        model.addAttribute("profile", userProfileRepository.getProfile(Long.parseLong(chatId)).orElse(""));
        model.addAttribute("files", fileService.list(Long.parseLong(chatId)));
        return "conversation";
    }
}
