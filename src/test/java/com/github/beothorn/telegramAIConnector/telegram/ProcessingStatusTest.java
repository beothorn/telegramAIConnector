package com.github.beothorn.telegramAIConnector.telegram;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ProcessingStatusTest {

    @Test
    void registerShowsStatusUntilThreadEnds() {
        final ProcessingStatus status = new ProcessingStatus();
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Future<?> future = executor.submit(() -> {
            try {
                Thread.sleep(200);
            } catch (final InterruptedException ignored) {
            }
        });

        status.register(1L, future, "working");

        assertTrue(status.status(1L).contains("working"));

        Awaitility.await().atMost(Duration.ofSeconds(1)).until(future::isDone);
        status.unregister(1L, future);

        assertEquals("I'm not doing anything.", status.status(1L));
        executor.shutdown();
    }

    @Test
    void multipleThreadsKeepIndependentStatuses() {
        final ProcessingStatus status = new ProcessingStatus();
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        final Future<?> f1 = executor.submit(() -> {
            try {
                Thread.sleep(200);
            } catch (final InterruptedException ignored) {
            }
        });
        final Future<?> f2 = executor.submit(() -> {
            try {
                Thread.sleep(400);
            } catch (final InterruptedException ignored) {
            }
        });

        status.register(1L, f1, "one");
        status.register(1L, f2, "two");

        assertTrue(status.status(1L).contains("one"));
        assertTrue(status.status(1L).contains("two"));

        Awaitility.await().atMost(Duration.ofSeconds(1)).until(f1::isDone);
        status.unregister(1L, f1);
        final String current = status.status(1L);
        assertFalse(current.contains("one"));
        assertTrue(current.contains("two"));

        Awaitility.await().atMost(Duration.ofSeconds(1)).until(f2::isDone);
        status.unregister(1L, f2);

        assertEquals("I'm not doing anything.", status.status(1L));
        executor.shutdown();
    }
}
