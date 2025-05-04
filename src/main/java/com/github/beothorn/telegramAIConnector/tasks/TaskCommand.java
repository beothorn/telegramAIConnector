package com.github.beothorn.telegramAIConnector.tasks;

public record TaskCommand(
            String key,
            Long chatId,
            String dateTime,
            String command
    ){}