package com.github.beothorn.telegramAIConnector;

import ai.fal.client.ClientConfig;
import ai.fal.client.CredentialsResolver;
import ai.fal.client.FalClient;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
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

    @Bean
    public FalClient client(
        @Value("${fal.key:}") final String falKey
    ) {
        if (Strings.isNotBlank(falKey)) {
            return FalClient.withConfig(ClientConfig.withCredentials(CredentialsResolver.fromApiKey(falKey)));
        }
        return null;
    }
}
