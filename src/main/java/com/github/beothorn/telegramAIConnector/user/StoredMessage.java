package com.github.beothorn.telegramAIConnector.user;

public record StoredMessage(
    long id,
    String role,
    String content,
    String timestamp
) {}
