package com.github.beothorn.telegramAIConnector.utils;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class InstantUtilsTest {
    @Test
    void formatAndParse() {
        Instant now = Instant.now();
        String str = InstantUtils.formatFromInstant(now);
        Instant parsed = InstantUtils.parseToInstant(str);
        assertEquals(str, InstantUtils.formatFromInstant(parsed));
    }
}
