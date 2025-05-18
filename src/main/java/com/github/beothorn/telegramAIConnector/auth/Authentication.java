package com.github.beothorn.telegramAIConnector.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class Authentication {

    private final Logger logger = LoggerFactory.getLogger(Authentication.class);

    private final String password;

    private final Set<Long> loggedChats = new HashSet<>();

    public Authentication(
        @Value("${telegram.password}") final String password
    ) {
        this.password = password;
    }

    public boolean isNotLogged(Long chatId) {
        return !loggedChats.contains(chatId);
    }

    public boolean login(
        final Long chatId,
        final String passwordLogin
    ) {
        // TODO: Check if ever tried to login, if not create a user entry
        if (passwordLogin.equals(password)) {
            logger.info("Chat {} is authenticated.", chatId);
            loggedChats.add(chatId);
            return true;
        }
        logger.info("Chat Bad login attempt {}", chatId);
        return false;
    }

    public void setPasswordForUser(
        final Long chatId,
        final String passwordLogin
    ) {
        // TODO
    }
}
