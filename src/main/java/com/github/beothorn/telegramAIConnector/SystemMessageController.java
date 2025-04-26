package com.github.beothorn.telegramAIConnector;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@RestController
@RequestMapping("/")
public class SystemMessageController {

    private final TelegramAiBot telegramAiBot;

    public SystemMessageController(
        final TelegramAiBot telegramAiBot
    ) {
        this.telegramAiBot = telegramAiBot;
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
}