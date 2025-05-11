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

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Main.class);
        app.setBannerMode(Banner.Mode.OFF);

        // Load external properties.yaml from current working directory
        String currentDir = System.getProperty("user.dir");

        File externalYaml = new File(currentDir, "properties.yaml");
        if (externalYaml.exists()) {
            logger.info("Application yaml found :'" + externalYaml.getAbsolutePath() + "'");
            app.setDefaultProperties(
                    Map.of("spring.config.additional-location", "file:" + externalYaml.getAbsolutePath())
            );
        } else {
            Optional<String> springFile = Arrays.stream(args)
                    .filter(a -> a.startsWith("--spring.config.location"))
                    .findFirst();
            springFile.ifPresentOrElse(
                sf -> logger.info("Application yaml is " + sf.substring(sf.indexOf("=") + 1)),
                () -> logger.info("Application yaml not found, using default.")
            );
        }
        app.run(args);
    }

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
