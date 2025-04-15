package com.github.beothorn.telegramAIConnector;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Bean
    public CommandLineRunner onExecute(
        final TelegramBotsLongPollingApplication botsApplication,
        final TelegramAiBot telegramAIBot,
        final @Value("${telegram.key}") String botToken
    ) {
        return args -> {
            botsApplication.registerBot(botToken, telegramAIBot);
        };
    }
}
