package com.github.beothorn.telegramAIConnector.tasks;

import com.github.beothorn.telegramAIConnector.persistence.TaskRepository;
import com.github.beothorn.telegramAIConnector.telegram.TelegramAiBot;
import com.github.beothorn.telegramAIConnector.utils.InstantUtils;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
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
public class TaskScheduler {

    private final TaskRepository taskRepository;

    public record ScheduledWithTime(
            TaskCommand taskCommand,
            ScheduledFuture<?> task
    ){
        @NotNull
        @Override
        public String toString() {
            return "There is a task with key '" + taskCommand.key() + "' scheduled for '" + taskCommand.dateTime() + "'.";
        }
    }

    private final org.springframework.scheduling.TaskScheduler scheduler;
    private final Map<Long, Map<String, ScheduledWithTime>> tasksPerChat = new HashMap<>();

    public TaskScheduler(
        final TaskRepository taskRepository
    ) {
        this.taskRepository = taskRepository;

        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(5);
        taskScheduler.setThreadNamePrefix("ScheduledTask-");
        taskScheduler.initialize();
        this.scheduler = taskScheduler;
    }

    public void restoreTasksFromDatabase(
        final TelegramAiBot telegramAiBot
    ) {
        for (TaskCommand task : taskRepository.getAll()) {
            Instant dateTime = InstantUtils.parseToInstant(task.dateTime());
            if (dateTime.isAfter(Instant.now())) {
                schedule(telegramAiBot,task.chatId(), task.command(), dateTime, task.key(), false);
            } else {
                taskRepository.deleteTask(task.key()); // Clean up expired tasks
            }
        }
    }

    public synchronized void schedule(
        final TelegramAiBot telegramAiBot,
        final Long chatId,
        final String command,
        final Instant dateTime
    ) {
        Map<String, ScheduledWithTime> tasksForChatId = tasksPerChat.getOrDefault(chatId, new HashMap<>());
        String finalKey = createUniqueKey(command, tasksForChatId);
        schedule(telegramAiBot, chatId, command, dateTime, finalKey, true);
    }

    // Overloaded method used for both new and restored tasks
    private void schedule(
        final TelegramAiBot telegramAiBot,
        final Long chatId,
        final String command,
        final Instant dateTime,
        final String key,
        final boolean persist
    ) {
        Map<String, ScheduledWithTime> tasksForChatId = tasksPerChat.getOrDefault(chatId, new HashMap<>());

        TaskCommand taskCommand = new TaskCommand(key, chatId, InstantUtils.formatFromInstant(dateTime), command);

        Runnable wrappedTask = () -> {
            try {
                telegramAiBot.execute(chatId, command);
            } finally {
                synchronized (TaskScheduler.this) {
                    tasksForChatId.remove(key);
                    taskRepository.deleteTask(key); // Remove after execution
                }
            }
        };

        ScheduledFuture<?> future = scheduler.schedule(wrappedTask, dateTime);
        tasksForChatId.put(key, new ScheduledWithTime(taskCommand, future));
        tasksPerChat.put(chatId, tasksForChatId);

        if (persist) {
            taskRepository.addTask(taskCommand);
        }
    }

    private static String createUniqueKey(String key, Map<String, ScheduledWithTime> tasksForChatId) {
        String uniqueKey = key;
        int i = 0;
        while (tasksForChatId.containsKey(uniqueKey)) {
            uniqueKey = key + i;
            i++;
        }
        return uniqueKey;
    }

    public synchronized Optional<TaskCommand> cancel(Long chatId, String key) {
        Map<String, ScheduledWithTime> tasksForChatId = tasksPerChat.getOrDefault(chatId, new HashMap<>());

        if (!tasksForChatId.containsKey(key)) {
            return Optional.empty();
        }

        ScheduledWithTime cancelledTask = tasksForChatId.remove(key);
        ScheduledFuture<?> future = cancelledTask.task;

        if (future != null) {
            boolean cancelled = future.cancel(false);
            if (!cancelled) return Optional.empty();
        }

        taskRepository.deleteTask(key);
        return Optional.of(cancelledTask.taskCommand());
    }

    public synchronized String listScheduledKeys(Long chatId) {
        Map<String, ScheduledWithTime> tasksForChatId = tasksPerChat.getOrDefault(chatId, new HashMap<>());
        return tasksForChatId.values().stream().map(Objects::toString).collect(Collectors.joining("\n"));
    }

    @PreDestroy
    public synchronized void shutdown() {
        tasksPerChat.values().forEach(map ->
                map.values().forEach(t -> t.task.cancel(false)));
        tasksPerChat.clear();
    }
}
