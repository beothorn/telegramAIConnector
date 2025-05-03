package com.github.beothorn.telegramAIConnector.tasks;

public record TaskCommand(
            String key,
            String dateTime,
            String command
    ){}