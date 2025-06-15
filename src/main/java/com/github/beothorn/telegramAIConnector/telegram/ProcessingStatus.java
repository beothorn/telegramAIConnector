package com.github.beothorn.telegramAIConnector.telegram;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class ProcessingStatus {

    private final Map<Long, Map<Future<?>, String>> running = new ConcurrentHashMap<>();

    /**
     * Registers a new running asynchronous task for a chat.
     */
    public void register(
        final Long chatId,
        final Future<?> future,
        final String description
    ) {
        running.computeIfAbsent(chatId, id -> new ConcurrentHashMap<>())
            .put(future, description);
    }

    /**
     * Removes a finished task from the registry.
     */
    public void unregister(
        final Long chatId,
        final Future<?> future
    ) {
        final Map<Future<?>, String> futures = running.get(chatId);
        if (futures == null) {
            return;
        }
        futures.remove(future);
        if (futures.isEmpty()) {
            running.remove(chatId);
        }
    }

    /**
     * Returns a human readable description of running tasks for a chat.
     */
    public String status(
        final Long chatId
    ) {
        final Map<Future<?>, String> futures = running.get(chatId);
        if (futures == null || futures.isEmpty()) {
            return "I'm not doing anything.";
        }

        futures.entrySet().removeIf(entry -> entry.getKey().isDone());
        if (futures.isEmpty()) {
            running.remove(chatId);
            return "I'm not doing anything.";
        }

        return futures.values().stream()
            .map(desc -> "Processing: " + desc)
            .collect(Collectors.joining("\n"));
    }
}
