package com.github.beothorn.telegramAIConnector.tasks;

import com.github.beothorn.telegramAIConnector.utils.InstantUtils;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
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

    private final TaskExecutorService taskExecutorService;

    public record ScheduledWithTime(
            TaskCommand taskCommand,
            ScheduledFuture<?> task
    ){
        @NotNull
        @Override
        public String toString() {
            return "There is a task with key '" + taskCommand.key() + " scheduled for '" + taskCommand.dateTime() + "'.";
        }
    }

    private final TaskScheduler scheduler;
    private final Map<Long, Map<String, ScheduledWithTime>> tasksPerChat = new HashMap<>();

    public TaskSchedulerService(
        final TaskExecutorService taskExecutorService
    ) {
        this.taskExecutorService = taskExecutorService;
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(5);
        taskScheduler.setThreadNamePrefix("ScheduledTask-");
        taskScheduler.initialize();
        this.scheduler = taskScheduler;
    }

    public synchronized void schedule(
        final Long chatId,
        final String command,
        final Instant dateTime
    ) {
        String dateTimeAsString = InstantUtils.formatFromInstant(dateTime);
        Map<String, ScheduledWithTime> tasksForChatId = tasksPerChat.getOrDefault(chatId, new HashMap<>());
        String finalKey = createUniqueKey(command, tasksForChatId);

        TaskCommand taskCommand = new TaskCommand(
                finalKey,
                dateTimeAsString,
                command
        );

        schedule(chatId, command, dateTime, tasksForChatId, finalKey, taskCommand);
    }

    private void schedule(Long chatId, String command, Instant dateTime, Map<String, ScheduledWithTime> tasksForChatId, String finalKey, TaskCommand taskCommand) {
        Runnable wrappedTask = () -> {
            try {
                taskExecutorService.execute(chatId, command);
            } finally {
                synchronized (TaskSchedulerService.this) {
                    tasksForChatId.remove(finalKey);
                }
            }
        };
        ScheduledFuture<?> future = scheduler.schedule(wrappedTask, dateTime);
        tasksForChatId.put(
                finalKey,
            new ScheduledWithTime(
                    taskCommand,
                future
            )
        );
        tasksPerChat.put(chatId, tasksForChatId);
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

    public synchronized Optional<TaskCommand> cancel(
        final Long chatId,
        final String key
    ) {
        Map<String, ScheduledWithTime> tasksForChatId = tasksPerChat.getOrDefault(chatId, new HashMap<>());

        if (!tasksForChatId.containsKey(key)) {
            return Optional.empty();
        }
        ScheduledWithTime cancelledTask = tasksForChatId.get(key);
        ScheduledFuture<?> future = tasksForChatId.remove(key).task;
        if (future != null) {
            boolean cancel = future.cancel(false);
            if (!cancel) return Optional.empty();
        }
        return Optional.of(cancelledTask.taskCommand());
    }

    public synchronized String listScheduledKeys(
        final Long chatId
    ) {
        Map<String, ScheduledWithTime> tasksForChatId = tasksPerChat.getOrDefault(chatId, new HashMap<>());
        return tasksForChatId.values().stream().map(Objects::toString).collect(Collectors.joining("\n"));
    }

    @PreDestroy
    public synchronized void shutdown() {
        tasksPerChat.values().forEach(t ->
                t.values().forEach(f -> f.task.cancel(false)));
        tasksPerChat.clear();
    }
}
