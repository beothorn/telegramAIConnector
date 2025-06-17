package com.github.beothorn.telegramAIConnector;

import com.github.beothorn.telegramAIConnector.telegram.TelegramAiBot;
import org.junit.jupiter.api.Test;
import org.springframework.boot.CommandLineRunner;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MainOnExecuteTest {

    /**
     * Asserts that on start, the bot gets registered.
     * If it didn`t, no message would be processed.
     * @throws Exception
     */
    @Test
    void runnerRegistersBotWithToken() throws Exception {
        TelegramBotsLongPollingApplication botsApp = mock(TelegramBotsLongPollingApplication.class);
        TelegramAiBot bot = mock(TelegramAiBot.class);

        Main main = new Main();
        CommandLineRunner runner = main.onExecute(botsApp, bot, "123");

        runner.run();

        verify(botsApp).registerBot("123", bot);
    }
}
