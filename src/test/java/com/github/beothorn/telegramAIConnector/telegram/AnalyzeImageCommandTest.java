package com.github.beothorn.telegramAIConnector.telegram;

import com.github.beothorn.telegramAIConnector.ai.AiBotService;
import com.github.beothorn.telegramAIConnector.ai.tools.AIAnalysisTool;
import com.github.beothorn.telegramAIConnector.auth.Authentication;
import com.github.beothorn.telegramAIConnector.tasks.TaskScheduler;
import com.github.beothorn.telegramAIConnector.user.UserRepository;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

public class AnalyzeImageCommandTest {
    @Test
    void analyzeImageCommandCallsTool() throws Exception {
        Unsafe unsafe = getUnsafe();
        TelegramAiBot bot = (TelegramAiBot) unsafe.allocateInstance(TelegramAiBot.class);
        set(bot, "telegramClient", mock(org.telegram.telegrambots.meta.generics.TelegramClient.class));
        set(bot, "aiBotService", mock(AiBotService.class));
        set(bot, "taskScheduler", mock(TaskScheduler.class));
        set(bot, "authentication", mock(Authentication.class));
        set(bot, "userRepository", mock(UserRepository.class));
        set(bot, "commands", mock(Commands.class));
        AIAnalysisTool tool = mock(AIAnalysisTool.class);
        when(tool.analyzeImage("img.png", "Describe the image.")).thenReturn("ok");
        set(bot, "aiAnalysisTool", tool);
        set(bot, "uploadFolder", "u");
        set(bot, "processingStatus", new ProcessingStatus());
        ExecutorService exec = Executors.newSingleThreadExecutor();
        ScheduledExecutorService sched = Executors.newSingleThreadScheduledExecutor();
        set(bot, "executor", exec);
        set(bot, "typingScheduler", sched);
        set(bot, "logger", org.slf4j.LoggerFactory.getLogger(TelegramAiBot.class));

        Method m = TelegramAiBot.class.getDeclaredMethod("consumeCommand", Long.class, String.class, String.class);
        m.setAccessible(true);
        m.invoke(bot, 1L, "analyzeImage", "img.png");

        exec.shutdown();
        exec.awaitTermination(1, TimeUnit.SECONDS);
        sched.shutdownNow();

        verify(tool).analyzeImage("img.png", "Describe the image.");
    }

    private static Unsafe getUnsafe() throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (Unsafe) f.get(null);
    }

    private static void set(Object o, String field, Object value) throws Exception {
        Field f = TelegramAiBot.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(o, value);
    }
}
