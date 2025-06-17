package com.github.beothorn.telegramAIConnector.tasks;

import com.github.beothorn.telegramAIConnector.telegram.TelegramAiBot;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class RecoverOnStartupTest {

    /**
     * Make sure that when restarting, all tasks are recovered (so the futures can be recreated)
     */
    @Test
    void callsRestoreOnEvent() {
        TaskScheduler scheduler = mock(TaskScheduler.class);
        TelegramAiBot bot = mock(TelegramAiBot.class);
        RecoverOnStartup r = new RecoverOnStartup(scheduler, bot);
        r.onApplicationEvent(new ApplicationReadyEvent(
                new org.springframework.boot.SpringApplication(Object.class),
                new String[]{},
                new AnnotationConfigApplicationContext(),
                java.time.Duration.ZERO));
        verify(scheduler).restoreTasksFromDatabase(bot);
    }
}
