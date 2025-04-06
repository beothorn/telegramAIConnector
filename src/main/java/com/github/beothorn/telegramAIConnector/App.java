package com.github.beothorn.telegramAIConnector;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

@SpringBootApplication
public class App {

    @Value("${telegram.key}")
    private String botToken;

    @Value("${spring.ai.openai.api-key}")
    private String openAI;

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Bean
    public CommandLineRunner startBot(
        TelegramBotsLongPollingApplication botsApplication,
        TelegramAiBot telegramAiBot
    ) {
        return args -> {
            botsApplication.registerBot(botToken, telegramAiBot);
        };
    }

    @Bean
    public TelegramBotsLongPollingApplication botsApplication(){
        return new TelegramBotsLongPollingApplication();
    }
}
