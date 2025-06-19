package com.github.beothorn.telegramAIConnector.telegram;

import ai.fal.client.FalClient;
import com.github.beothorn.telegramAIConnector.ai.AiBotService;
import com.github.beothorn.telegramAIConnector.auth.Authentication;
import com.github.beothorn.telegramAIConnector.tasks.TaskScheduler;
import com.github.beothorn.telegramAIConnector.user.MessagesRepository;
import com.github.beothorn.telegramAIConnector.user.UserRepository;
import com.github.beothorn.telegramAIConnector.telegram.Commands;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.model.ChatModel;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.User;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TelegramAiBotMessageHistoryTest {

    @TempDir
    Path tempDir;

    private TelegramAiBot createBot(MessagesRepository messages, TelegramClient client) throws Exception {
        TelegramAiBot bot = new TelegramAiBot(
                mock(AiBotService.class),
                mock(TaskScheduler.class),
                mock(Authentication.class),
                mock(UserRepository.class),
                mock(Commands.class),
                messages,
                mock(ChatModel.class),
                mock(FalClient.class),
                new ProcessingStatus(),
                "token",
                tempDir.toString()
        );
        Field f = TelegramAiBot.class.getDeclaredField("telegramClient");
        f.setAccessible(true);
        f.set(bot, client);
        return bot;
    }

    /**
     * Asserts that plain messages are stored in history when sent.
     */
    @Test
    void sendMessageStoresHistory() throws Exception {
        MessagesRepository messages = mock(MessagesRepository.class);
        TelegramClient client = mock(TelegramClient.class);
        TelegramAiBot bot = createBot(messages, client);

        bot.sendMessage(1L, "hi");

        verify(client).execute(any(SendMessage.class));
        verify(messages).insertMessage("1", "assistant", "hi");
    }

    /**
     * Asserts that file captions are stored in history when sending files.
     */
    @Test
    void sendFileStoresCaption() throws Exception {
        MessagesRepository messages = mock(MessagesRepository.class);
        TelegramClient client = mock(TelegramClient.class);
        TelegramAiBot bot = createBot(messages, client);

        File file = File.createTempFile("tmp", ".txt", tempDir.toFile());
        bot.sendFileWithCaption(2L, file.getAbsolutePath(), "cap");

        verify(client).execute(any(SendDocument.class));
        verify(messages).insertMessage("2", "assistant", "cap");
    }

    /**
     * Commands store the command and the result in history.
     */
    @Test
    void consumeCommandStoresBothMessages() throws Exception {
        MessagesRepository messages = mock(MessagesRepository.class);
        TelegramClient client = mock(TelegramClient.class);
        Commands commands = mock(Commands.class);
        when(commands.listUploadedFiles(1L)).thenReturn("files");
        TelegramAiBot bot = new TelegramAiBot(
                mock(AiBotService.class),
                mock(TaskScheduler.class),
                mock(Authentication.class),
                mock(UserRepository.class),
                commands,
                messages,
                mock(ChatModel.class),
                mock(FalClient.class),
                new ProcessingStatus(),
                "token",
                tempDir.toString()
        );
        Field f = TelegramAiBot.class.getDeclaredField("telegramClient");
        f.setAccessible(true);
        f.set(bot, client);

        Update u = new Update();
        Message m = new Message();
        Chat chat = new Chat(1L, "private");
        m.setChat(chat);
        User user = new User(99L, "u", false);
        m.setFrom(user);
        m.setText("/list");
        u.setMessage(m);

        bot.consume(u);

        verify(client).execute(any(SendMessage.class));
        verify(messages).insertMessage("1", "user", "/list");
        verify(messages).insertMessage("1", "assistant", "files");
    }
}
