package com.github.beothorn.telegramAIConnector.telegram;

import com.github.beothorn.telegramAIConnector.ai.AiBotService;
import com.github.beothorn.telegramAIConnector.auth.Authentication;
import com.github.beothorn.telegramAIConnector.tasks.TaskScheduler;
import com.github.beothorn.telegramAIConnector.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.Executors;

import static org.mockito.Mockito.*;

public class TelegramAiBotSendMessageTest {
    @Test
    void blankMessageIsIgnored() throws Exception {
        Unsafe unsafe = getUnsafe();
        TelegramAiBot bot = (TelegramAiBot) unsafe.allocateInstance(TelegramAiBot.class);
        set(bot, "telegramClient", mock(org.telegram.telegrambots.meta.generics.TelegramClient.class));
        set(bot, "aiBotService", mock(AiBotService.class));
        set(bot, "taskScheduler", mock(TaskScheduler.class));
        set(bot, "authentication", mock(Authentication.class));
        set(bot, "userRepository", mock(UserRepository.class));
        set(bot, "commands", mock(Commands.class));
        set(bot, "uploadFolder", "u");
        set(bot, "logger", org.slf4j.LoggerFactory.getLogger(TelegramAiBot.class));
        set(bot, "processingStatus", mock(ProcessingStatus.class));
        set(bot, "executor", Executors.newSingleThreadExecutor());
        set(bot, "typingScheduler", Executors.newSingleThreadScheduledExecutor());

        bot.sendMessage(1L, "");
        Object client = getField(bot, "telegramClient");
        verifyNoInteractions((org.telegram.telegrambots.meta.generics.TelegramClient) client);
    }

    private static Unsafe getUnsafe() throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (Unsafe) f.get(null);
    }

    private static Object getField(Object o, String name) throws Exception {
        Field f = TelegramAiBot.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }

    private static void set(Object o, String field, Object value) throws Exception {
        Field f = TelegramAiBot.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(o, value);
    }
}
