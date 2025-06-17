package com.github.beothorn.telegramAIConnector.tasks;

import com.github.beothorn.telegramAIConnector.telegram.TelegramAiBot;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

public class TaskSchedulerTest {

    /**
     * Assert scheduling a task works.
     */
    @Test
    void scheduledTaskRuns() {
        TaskRepository repo = mock(TaskRepository.class);
        TaskScheduler scheduler = new TaskScheduler(repo);
        TelegramAiBot bot = mock(TelegramAiBot.class);
        scheduler.schedule(bot,1L,"cmd", Instant.now().plusMillis(100));
        Awaitility.await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> verify(bot).execute(1L,"cmd"));
    }

    /**
     * Assert cancelling a task works and the task is deleted.
     */
    @Test
    void cancelRemovesTask() {
        TaskRepository repo = mock(TaskRepository.class);
        TaskScheduler scheduler = new TaskScheduler(repo);
        TelegramAiBot bot = mock(TelegramAiBot.class);
        scheduler.schedule(bot,1L,"cmd", Instant.now().plusSeconds(10));
        scheduler.cancel(1L,"cmd");
        verify(repo).deleteTask(anyString());
    }
}
