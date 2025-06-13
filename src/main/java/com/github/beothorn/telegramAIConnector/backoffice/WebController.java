package com.github.beothorn.telegramAIConnector.backoffice;

import com.github.beothorn.telegramAIConnector.tasks.TaskRepository;
import com.github.beothorn.telegramAIConnector.user.MessagesRepository;
import com.github.beothorn.telegramAIConnector.user.StoredMessage;
import com.github.beothorn.telegramAIConnector.user.profile.UserProfileRepository;
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

    public WebController(
        final TaskRepository taskRepository,
        final MessagesRepository messagesRepository,
        final UserProfileRepository userProfileRepository,
        final FileService fileService
    ) {
        this.taskRepository = taskRepository;
        this.messagesRepository = messagesRepository;
        this.userProfileRepository = userProfileRepository;
        this.fileService = fileService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("conversations", messagesRepository.findConversationIds());
        model.addAttribute("tasks", taskRepository.getAll());
        return "backoffice";
    }

    @GetMapping("/conversations/{chatId}")
    public String conversation(
        @PathVariable String chatId,
        @RequestParam(defaultValue = "0") int page,
        Model model
    ) {
        int limit = 50;
        model.addAttribute("chatId", chatId);
        model.addAttribute("messages", messagesRepository.getMessages(chatId, limit, page * limit));
        model.addAttribute("prevPage", Math.max(page - 1, 0));
        model.addAttribute("nextPage", page + 1);
        model.addAttribute("tasks", taskRepository.findByChatId(Long.parseLong(chatId)));
        model.addAttribute("profile", userProfileRepository.getProfile(Long.parseLong(chatId)).orElse(""));
        model.addAttribute("files", fileService.list(Long.parseLong(chatId)));
        return "conversation";
    }
}
