package com.github.beothorn.telegramAIConnector;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

@Component
public class TelegramAiBot implements LongPollingSingleThreadUpdateConsumer {

    private final AiBotService aiBotService;
    private TelegramClient telegramClient;

    public TelegramAiBot(
        AiBotService aiBotService,
        @Value("${telegram.key}") String botToken
    ) {
        this.aiBotService = aiBotService;
        telegramClient = new OkHttpTelegramClient(botToken);
    }

    @Override
    public void consume(final List<Update> updates) {
        System.out.println("Consume list");
        updates.forEach(this::consume);
    }

    @Override
    public void consume(final Update update) {
        System.out.println("Consume");
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            System.out.println(text);

            String chatId = update.getMessage().getChatId() + "";

            // TODO: append history here
            String response = aiBotService.prompt(text);

            try {
                SendMessage sendMessage = new SendMessage(chatId, response);
                try {
                    telegramClient.execute(sendMessage);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }catch (Exception e) {
                System.out.println(e.getMessage());
                SendMessage sendMessage = new SendMessage(chatId, e.getMessage());
                try {
                    telegramClient.execute(sendMessage);
                } catch (TelegramApiException te) {
                    te.printStackTrace();
                }
            }
        }
    }
}
