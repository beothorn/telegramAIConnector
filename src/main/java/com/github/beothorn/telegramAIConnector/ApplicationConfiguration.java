package com.github.beothorn.telegramAIConnector;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.HiddenHttpMethodFilter;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

@Configuration
public class ApplicationConfiguration {

    /**
     * Creates the {@link TelegramBotsLongPollingApplication} used by the bot.
     *
     * @return configured Telegram bots application
     */
    @Bean
    public TelegramBotsLongPollingApplication botsApplication(){
        return new TelegramBotsLongPollingApplication();
    }

    /**
     * Registers the filter enabling HTTP method overrides used by Spring.
     *
     * @return the configured {@link HiddenHttpMethodFilter}
     */
    @Bean
    public HiddenHttpMethodFilter hiddenHttpMethodFilter() {
        return new HiddenHttpMethodFilter();
    }
}
