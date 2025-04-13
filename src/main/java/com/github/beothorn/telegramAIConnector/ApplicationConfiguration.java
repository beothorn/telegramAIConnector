package com.github.beothorn.telegramAIConnector;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

@Configuration
public class ApplicationConfiguration {

    @Bean
    public TelegramBotsLongPollingApplication botsApplication(){
        return new TelegramBotsLongPollingApplication();
    }
}
