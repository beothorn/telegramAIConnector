package com.github.beothorn.telegramAIConnector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.*;

@Component
public class TelegramAiBot implements LongPollingSingleThreadUpdateConsumer {

    private final AiBotService aiBotService;
    private final TelegramClient telegramClient;
    private final String password;

    private final Set<Long> loggedChats = new HashSet<>();

    private final Logger logger = LoggerFactory.getLogger(TelegramAiBot.class);

    public TelegramAiBot(
        final AiBotService aiBotService,
        @Value("${telegram.key}") final String botToken,
        @Value("${telegram.password}") final String password
    ) {
        this.aiBotService = aiBotService;
        telegramClient = new OkHttpTelegramClient(botToken);
        this.password = password;
    }

    @Override
    public void consume(final List<Update> updates) {
        updates.forEach(this::consume);
    }

    @Override
    public void consume(final Update update) {
        Long chatId = update.getMessage().getChatId();

        // If not logged in, only respond to login attempt
        if (!loggedChats.contains(chatId)) {
            if (!update.hasMessage()) return;
            if (!update.getMessage().hasText()) return;
            String text = update.getMessage().getText();
            consumeLogin(chatId, text);
            logger.info("Bad login attempt {}: {}", chatId, text);
            sendMessage(chatId, "You are talking to TelegramAIConnector.");
            return;
        }

        if (update.hasMessage()) {
            consumeMessage(chatId, update);
        }
    }

    private void sendMessage(
        final Long chatId,
        final String response
    ) {
        logger.info("Send message to {}: {}", chatId, response);
        SendMessage sendMessage = new SendMessage(Long.toString(chatId), response);
        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void consumeLogin(
        final Long chatId,
        final String loginCommand
    ) {
        logger.info("Consume login from {}: {}", chatId, loginCommand);

        String[] loginWithArgs = loginCommand.split("\\s+", 2);
        if (!loginWithArgs[0].equals("/login")) return;
        if (loginWithArgs[1].equals(password)) {
            logger.info("Logged in");
            loggedChats.add(chatId);
            sendMessage(chatId, "You are logged in.");
        }
    }

    private void consumeMessage(final Long chatId, final Update update) {
        logger.info("Consume message from {}", chatId);
        if (update.getMessage().hasText()) {
            String text = update.getMessage().getText();

            if (text.startsWith("/")) {
                String[] commandWithArgs = text.split("\\s+", 2);
                if (commandWithArgs.length == 1) {
                    consumeCommand(chatId, commandWithArgs[0], "");
                } else if (commandWithArgs.length == 2) {
                    consumeCommand(chatId, commandWithArgs[0], commandWithArgs[1]);
                } else {
                    throw new IllegalArgumentException("Bad command " + text);
                }
            } else {
                consumeText(chatId, text);
            }
        }
    }

    private void consumeCommand(
        final Long chatId,
        final String command,
        final String args
    ) {
        logger.info("Consume command from {}: {} {}", chatId, command, args);

        if (command.equalsIgnoreCase("chatId")) {
            sendMessage(chatId, "Your chat id is " + chatId);
        }
    }

    private void consumeText(
        final Long chatId,
        final String text
    ) {
        logger.info("Consume text from {}: {}", chatId, text);
        // TODO: append history here
        String response = aiBotService.prompt(text);

        logger.info("Response to " + chatId + ": " + text);
        sendMessage(chatId, response);
    }
}
