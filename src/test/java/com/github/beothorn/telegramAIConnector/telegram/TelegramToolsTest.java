package com.github.beothorn.telegramAIConnector.telegram;

import com.github.beothorn.telegramAIConnector.tasks.TaskScheduler;
import com.github.beothorn.telegramAIConnector.utils.InstantUtils;
import com.github.beothorn.telegramAIConnector.user.MessagesRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TelegramToolsTest {

    @TempDir
    Path tempDir;

    /**
     * Registers a reminder when the given date is in the future.
     */
    @Test
    void sendReminderSchedulesWhenFutureDate() {
        TaskScheduler scheduler = mock(TaskScheduler.class);
        TelegramAiBot bot = mock(TelegramAiBot.class);
        MessagesRepository messages = mock(MessagesRepository.class);
        TelegramTools tools = new TelegramTools(bot, scheduler, 1L, tempDir.toString(), messages);

        String future = InstantUtils.formatFromInstant(Instant.now().plusSeconds(60));
        String msg = tools.sendReminder("hello", future);

        assertTrue(msg.contains("Reminder was registered"));
        verify(scheduler).schedule(eq(bot), eq(1L), eq("hello"), any(Instant.class));
    }

    /**
     * Rejects reminders scheduled for past dates.
     */
    @Test
    void sendReminderRefusesPastDate() {
        TaskScheduler scheduler = mock(TaskScheduler.class);
        TelegramAiBot bot = mock(TelegramAiBot.class);
        MessagesRepository messages = mock(MessagesRepository.class);
        TelegramTools tools = new TelegramTools(bot, scheduler, 1L, tempDir.toString(), messages);

        String past = InstantUtils.formatFromInstant(Instant.now().minusSeconds(60));
        String msg = tools.sendReminder("hi", past);

        assertTrue(msg.startsWith("Can't set reminder"));
        verifyNoInteractions(scheduler);
    }

    /**
     * Sends a markdown message using the bot instance.
     */
    @Test
    void sendMessageDelegatesToBot() throws Exception {
        TaskScheduler scheduler = mock(TaskScheduler.class);
        TelegramAiBot bot = mock(TelegramAiBot.class);
        MessagesRepository messages = mock(MessagesRepository.class);
        TelegramTools tools = new TelegramTools(bot, scheduler, 2L, tempDir.toString(), messages);

        String result = tools.sendMessage("hello");
        assertEquals("Sent message successfully", result);
        verify(bot).sendMarkdownMessage(2L, "hello");
        verify(messages).insertMessage("2", "assistant", "hello");
    }

    /**
     * Returns an error message when Telegram fails to send.
     */
    @Test
    void sendMessageHandlesException() throws Exception {
        TaskScheduler scheduler = mock(TaskScheduler.class);
        TelegramAiBot bot = mock(TelegramAiBot.class);
        doThrow(new TelegramApiException("boom")).when(bot).sendMarkdownMessage(anyLong(), anyString());

        MessagesRepository messages = mock(MessagesRepository.class);
        TelegramTools tools = new TelegramTools(bot, scheduler, 2L, tempDir.toString(), messages);

        String result = tools.sendMessage("hello");
        assertEquals("Could not send message, got error: 'boom'.", result);
        verifyNoInteractions(messages);
    }

    /**
     * Delegates file sending to the bot.
     */
    @Test
    void sendAsFileCallsBot() throws Exception {
        TaskScheduler scheduler = mock(TaskScheduler.class);
        TelegramAiBot bot = mock(TelegramAiBot.class);
        MessagesRepository messages = mock(MessagesRepository.class);
        TelegramTools tools = new TelegramTools(bot, scheduler, 3L, tempDir.toString(), messages);

        String result = tools.sendAsFile("t.txt", "content");
        assertEquals("File 't.txt' sent successfully.", result);
        verify(bot).sendFileWithCaption(eq(3L), anyString(), contains("t.txt"));
        verify(messages).insertMessage("3", "assistant", "Here is your file: t.txt");
    }

    /**
     * Saves content to a file and reads it back.
     */
    @Test
    void saveAndReadFileWorks() throws Exception {
        TaskScheduler scheduler = mock(TaskScheduler.class);
        TelegramAiBot bot = mock(TelegramAiBot.class);
        MessagesRepository messages = mock(MessagesRepository.class);
        TelegramTools tools = new TelegramTools(bot, scheduler, 4L, tempDir.toString(), messages);

        String res = tools.saveAsFile("a.txt", "data");
        assertEquals("File 'a.txt' saved successfully.", res);

        String content = tools.readFile("a.txt");
        assertEquals("data", content);
    }

    /**
     * Deletes an existing file from disk.
     */
    @Test
    void deleteFileRemovesFile() throws Exception {
        TaskScheduler scheduler = mock(TaskScheduler.class);
        TelegramAiBot bot = mock(TelegramAiBot.class);
        MessagesRepository messages = mock(MessagesRepository.class);
        TelegramTools tools = new TelegramTools(bot, scheduler, 5L, tempDir.toString(), messages);

        Path dir = tempDir.resolve("5");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("a.txt"), "x");
        String msg = tools.deleteFile("a.txt");
        assertEquals("The file 'a.txt' was deleted.", msg);
    }

    /**
     * Rejects deletion when the file path escapes the chat directory.
     */
    @Test
    void deleteFileInvalidName() {
        TaskScheduler scheduler = mock(TaskScheduler.class);
        TelegramAiBot bot = mock(TelegramAiBot.class);
        MessagesRepository messages2 = mock(MessagesRepository.class);
        TelegramTools tools = new TelegramTools(bot, scheduler, 5L, tempDir.toString(), messages2);

        String msg = tools.deleteFile("../bad.txt");
        assertEquals("'../bad.txt' is not a valid file name.", msg);
    }
}
