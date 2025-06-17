package com.github.beothorn.telegramAIConnector.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StoredMessageTest {
    /**
     * Ensures the record accessors return the stored values.
     */
    @Test
    void recordValues() {
        StoredMessage m = new StoredMessage(1L,"user","hi","ts");
        assertEquals(1L,m.id());
        assertEquals("user", m.role());
        assertEquals("hi", m.content());
        assertEquals("ts", m.timestamp());
    }
}
