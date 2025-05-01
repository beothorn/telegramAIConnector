package com.github.beothorn.telegramAIConnector;

import jakarta.annotation.PreDestroy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

@Service
public class TaskSchedulerService {

    public record ScheduledWithTime(
            String key,
            String dateTime,
            ScheduledFuture<?> task
    ){
        @Override
        public String toString() {
            return "ScheduledWithTime{" +
                    "dateTime='" + dateTime + '\'' +
                    ", key='" + key + '\'' +
                    '}';
        }
    }

    private final TaskScheduler scheduler;
    private final Map<String, ScheduledWithTime> tasks = new HashMap<>();

    public TaskSchedulerService() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(5);
        taskScheduler.setThreadNamePrefix("ScheduledTask-");
        taskScheduler.initialize();
        this.scheduler = taskScheduler;
    }

    public synchronized String schedule(String key, Runnable task, Instant dateTime) {
        String uniqueKey = key;
        int i = 0;
        while (tasks.containsKey(uniqueKey)) {
            uniqueKey = key + i;
            i++;
        }
        String finalKey = uniqueKey;
        Runnable wrappedTask = () -> {
            try {
                task.run();
            } finally {
                synchronized (TaskSchedulerService.this) {
                    tasks.remove(finalKey);
                }
            }
        };
        ScheduledFuture<?> future = scheduler.schedule(wrappedTask, dateTime);
        tasks.put(uniqueKey, new ScheduledWithTime(key, InstantUtils.formatFromInstant(dateTime), future));
        return uniqueKey;
    }

    public synchronized Optional<ScheduledWithTime> cancel(String key) {
        if (!tasks.containsKey(key)) {
            return Optional.empty();
        }
        ScheduledWithTime cancelledTask = tasks.get(key);
        ScheduledFuture<?> future = tasks.remove(key).task;
        if (future != null) {
            boolean cancel = future.cancel(false);
            if (!cancel) return Optional.empty();
        }
        return Optional.of(cancelledTask);
    }

    public synchronized String listScheduledKeys(Long chatId) {
        return tasks.values().stream().map(Objects::toString).collect(Collectors.joining("\n"));
    }

    @PreDestroy
    public synchronized void shutdown() {
        for (ScheduledWithTime future : tasks.values()) {
            future.task().cancel(false);
        }
        tasks.clear();
    }
}
