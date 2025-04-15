package com.github.beothorn.telegramAIConnector;

import jakarta.annotation.PreDestroy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

@Service
public class TaskSchedulerService {

    private record ScheduledWithTime(
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

    public synchronized String schedule(String key, Runnable task, String dateTime) {
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
        ScheduledFuture<?> future = scheduler.schedule(wrappedTask, parseToInstant(dateTime));
        tasks.put(uniqueKey, new ScheduledWithTime(key, dateTime, future));
        return uniqueKey;
    }

    public synchronized boolean cancel(String key) {
        ScheduledFuture<?> future = tasks.remove(key).task;
        if (future != null) {
            return future.cancel(false); // do not interrupt if already running
        }
        return false;
    }

    public synchronized String listScheduledKeys() {
        return tasks.values().stream().map(Objects::toString).collect(Collectors.joining("\n"));
    }

    @PreDestroy
    public synchronized void shutdown() {
        for (ScheduledWithTime future : tasks.values()) {
            future.task().cancel(false);
        }
        tasks.clear();
    }

    public static Instant parseToInstant(String dateTimeStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
        LocalDateTime localDateTime = LocalDateTime.parse(dateTimeStr, formatter);
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }
}
