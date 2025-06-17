package com.github.beothorn.telegramAIConnector;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ApplicationConfigurationTest {

    /**
     * Asserts that application configuration provides a bot application.
     */
    @Test
    void botsApplicationBeanCreatesInstance() {
        ApplicationConfiguration config = new ApplicationConfiguration();
        TelegramBotsLongPollingApplication app = config.botsApplication();
        assertNotNull(app, "Bots application bean should not be null");
    }
}
