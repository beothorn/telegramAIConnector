package com.github.beothorn.telegramAIConnector;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

import java.io.File;
import java.util.Map;

@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Main.class);
        app.setBannerMode(Banner.Mode.OFF);

        // Load external properties.yaml from current working directory
        String currentDir = System.getProperty("user.dir");
        File externalYaml = new File(currentDir, "properties.yaml");
        if (externalYaml.exists()) {
            app.setDefaultProperties(
                Map.of("spring.config.additional-location", "file:" + externalYaml.getAbsolutePath())
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
            botsApplication.registerBot(botToken, telegramAIBot);
        };
    }
}
