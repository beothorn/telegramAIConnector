package com.github.beothorn.telegramAIConnector.tasks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TaskCommandTest {
    @Test
    void recordStoresValues() {
        TaskCommand cmd = new TaskCommand("k",1L,"d","c");
        assertEquals("k", cmd.key());
        assertEquals(1L, cmd.chatId());
        assertEquals("d", cmd.dateTime());
        assertEquals("c", cmd.command());
    }
}
