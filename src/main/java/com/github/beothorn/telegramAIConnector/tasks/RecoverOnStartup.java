package com.github.beothorn.telegramAIConnector.tasks;

import com.github.beothorn.telegramAIConnector.telegram.TelegramAiBot;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class RecoverOnStartup implements ApplicationListener<ApplicationReadyEvent> {

    private final TaskScheduler taskScheduler;
    private final TelegramAiBot telegramAiBot;

    public RecoverOnStartup(
        final TaskScheduler taskScheduler,
        final TelegramAiBot telegramAiBot
    ) {
        this.taskScheduler = taskScheduler;
        this.telegramAiBot = telegramAiBot;
    }


    /**
     * Restores tasks from the database once the application is ready.
     * This way, all tasks (such as reminders) will still be triggered.
     *
     * @param event event signaling the application is ready
     */
    @Override
    public void onApplicationEvent(@NotNull ApplicationReadyEvent event) {
        taskScheduler.restoreTasksFromDatabase(telegramAiBot);
    }
}
