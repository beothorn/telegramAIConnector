package com.github.beothorn.telegramAIConnector.tasks;

import com.github.beothorn.telegramAIConnector.telegram.TelegramAiBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
public class TaskExecutorService {

    private final Logger logger = LoggerFactory.getLogger(TaskExecutorService.class);

    private final TelegramAiBot telegramAiBot;

    public TaskExecutorService(
        final TelegramAiBot telegramAiBot
    ) {
        this.telegramAiBot = telegramAiBot;
    }

    public void execute(
        final Long chatId,
        final String command
    ) {
        try {
            telegramAiBot.sendMarkdownMessage(chatId, command);
        } catch (TelegramApiException e) {
            logger.info("Could not send message '{}' to '{}'", command, chatId, e);
        }
    }
}
