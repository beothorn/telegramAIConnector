package com.github.beothorn.telegramAIConnector;

import org.springframework.ai.tool.annotation.Tool;

public class TelegramTools {

    private final TelegramAiBot bot;
    private final Long chatId;

    public TelegramTools(
        final TelegramAiBot bot,
        final Long chatId
    ) {
        this.bot = bot;
        this.chatId = chatId;
    }

    @Tool(description = "Send a text message to the user asynchronously through telegram")
    void sendMessage(String message) {
        bot.sendMessage(chatId, message);
    }

}
