package com.github.beothorn.telegramAIConnector.telegram;

import com.github.beothorn.telegramAIConnector.tasks.TaskScheduler;
import com.github.beothorn.telegramAIConnector.utils.InstantUtils;
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

    @Test
    void sendReminderSchedulesWhenFutureDate() {
        TaskScheduler scheduler = mock(TaskScheduler.class);
        TelegramAiBot bot = mock(TelegramAiBot.class);
        TelegramTools tools = new TelegramTools(bot, scheduler, 1L, tempDir.toString());

        String future = InstantUtils.formatFromInstant(Instant.now().plusSeconds(60));
        String msg = tools.sendReminder("hello", future);

        assertTrue(msg.contains("Reminder was registered"));
        verify(scheduler).schedule(eq(bot), eq(1L), eq("hello"), any(Instant.class));
    }

    @Test
    void sendReminderRefusesPastDate() {
        TaskScheduler scheduler = mock(TaskScheduler.class);
        TelegramAiBot bot = mock(TelegramAiBot.class);
        TelegramTools tools = new TelegramTools(bot, scheduler, 1L, tempDir.toString());

        String past = InstantUtils.formatFromInstant(Instant.now().minusSeconds(60));
        String msg = tools.sendReminder("hi", past);

        assertTrue(msg.startsWith("Can't set reminder"));
        verifyNoInteractions(scheduler);
    }

    @Test
    void sendMessageDelegatesToBot() throws Exception {
        TaskScheduler scheduler = mock(TaskScheduler.class);
        TelegramAiBot bot = mock(TelegramAiBot.class);
        TelegramTools tools = new TelegramTools(bot, scheduler, 2L, tempDir.toString());

        String result = tools.sendMessage("hello");
        assertEquals("Sent message successfully", result);
        verify(bot).sendMarkdownMessage(2L, "hello");
    }

    @Test
    void sendMessageHandlesException() throws Exception {
        TaskScheduler scheduler = mock(TaskScheduler.class);
        TelegramAiBot bot = mock(TelegramAiBot.class);
        doThrow(new TelegramApiException("boom")).when(bot).sendMarkdownMessage(anyLong(), anyString());

        TelegramTools tools = new TelegramTools(bot, scheduler, 2L, tempDir.toString());

        String result = tools.sendMessage("hello");
        assertEquals("Could not send message, got error: 'boom'.", result);
    }

    @Test
    void sendAsFileCallsBot() throws Exception {
        TaskScheduler scheduler = mock(TaskScheduler.class);
        TelegramAiBot bot = mock(TelegramAiBot.class);

        TelegramTools tools = new TelegramTools(bot, scheduler, 3L, tempDir.toString());

        String result = tools.sendAsFile("t.txt", "content");
        assertEquals("File 't.txt' sent successfully.", result);
        verify(bot).sendFileWithCaption(eq(3L), anyString(), contains("t.txt"));
    }

    @Test
    void saveAndReadFileWorks() throws Exception {
        TaskScheduler scheduler = mock(TaskScheduler.class);
        TelegramAiBot bot = mock(TelegramAiBot.class);
        TelegramTools tools = new TelegramTools(bot, scheduler, 4L, tempDir.toString());

        String res = tools.saveAsFile("a.txt", "data");
        assertEquals("File 'a.txt' saved successfully.", res);

        String content = tools.readFile("a.txt");
        assertEquals("data", content);
    }

    @Test
    void deleteFileRemovesFile() throws Exception {
        TaskScheduler scheduler = mock(TaskScheduler.class);
        TelegramAiBot bot = mock(TelegramAiBot.class);
        TelegramTools tools = new TelegramTools(bot, scheduler, 5L, tempDir.toString());

        Path dir = tempDir.resolve("5");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("a.txt"), "x");
        String msg = tools.deleteFile("a.txt");
        assertEquals("The file 'a.txt' was deleted.", msg);
    }

    @Test
    void deleteFileInvalidName() {
        TaskScheduler scheduler = mock(TaskScheduler.class);
        TelegramAiBot bot = mock(TelegramAiBot.class);
        TelegramTools tools = new TelegramTools(bot, scheduler, 5L, tempDir.toString());

        String msg = tools.deleteFile("../bad.txt");
        assertEquals("'../bad.txt' is not a valid file name.", msg);
    }
}
