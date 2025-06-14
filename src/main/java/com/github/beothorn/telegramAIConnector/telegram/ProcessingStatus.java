package com.github.beothorn.telegramAIConnector.telegram;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class ProcessingStatus {

    private final Map<Long, Map<Future<?>, String>> running = new ConcurrentHashMap<>();

    public void register(
        final Long chatId,
        final Future<?> future,
        final String description
    ) {
        running.computeIfAbsent(chatId, id -> new ConcurrentHashMap<>())
            .put(future, description);
    }

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
