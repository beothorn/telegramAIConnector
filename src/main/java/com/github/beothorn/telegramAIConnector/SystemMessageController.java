package com.github.beothorn.telegramAIConnector;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@RestController
@RequestMapping("/")
public class SystemMessageController {


    private final TelegramAiBot telegramAiBot;
    private final Long adminChatId;

    public SystemMessageController(
        final TelegramAiBot telegramAiBot,
        @Value("${telegramIAConnector.adminChatId}") final String adminChatId
    ) {
        this.telegramAiBot = telegramAiBot;
        this.adminChatId = Long.parseLong(adminChatId);
    }

    @PostMapping("/")
    public String systemMessage(
        @RequestParam("message") final String message
    ) throws TelegramApiException {
        telegramAiBot.consumeSystemMessage(adminChatId, message);
        return "Hello from Telegram AI Connector!";
    }
}