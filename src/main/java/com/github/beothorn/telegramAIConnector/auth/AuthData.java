package com.github.beothorn.telegramAIConnector.auth;

public record AuthData(
    Long chatId,
    String password_hash,
    boolean logged,
    String log_expiration_date
){}