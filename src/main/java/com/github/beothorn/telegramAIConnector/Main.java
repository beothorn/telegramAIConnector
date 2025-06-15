package com.github.beothorn.telegramAIConnector;

import com.github.beothorn.telegramAIConnector.telegram.TelegramAiBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@SpringBootApplication
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    /**
     * Application entry point.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Main.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }

    /**
     * Registers the bot with Telegram when the application starts.
     *
     * @param botsApplication telegram bots application
     * @param telegramAIBot    the bot instance
     * @param botToken         bot authentication token
     * @return command line runner executing the registration
     */
    @Bean
    public CommandLineRunner onExecute(
        final TelegramBotsLongPollingApplication botsApplication,
        final TelegramAiBot telegramAIBot,
        final @Value("${telegram.key}") String botToken
    ) {
        return args -> {
            logger.info("Bot will be registered.");
            botsApplication.registerBot(botToken, telegramAIBot);
            logger.info("Bots application is running: " + botsApplication.isRunning());
        };
    }
}
