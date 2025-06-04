package com.github.beothorn.telegramAIConnector;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

import static org.junit.jupiter.api.Assertions.*;

public class ApplicationConfigurationTest {
    @Test
    void botsApplicationBeanCreatesInstance() {
        ApplicationConfiguration config = new ApplicationConfiguration();
        TelegramBotsLongPollingApplication app = config.botsApplication();
        assertNotNull(app, "Bots application bean should not be null");
    }
}
