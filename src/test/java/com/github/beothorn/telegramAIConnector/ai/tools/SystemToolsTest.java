package com.github.beothorn.telegramAIConnector.ai.tools;

import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SystemToolsTest {

    /**
     * Ensures current date is parsed successfully.
     */
    @Test
    void getCurrentDateTimeMatchesFormat() {
        LocaleContextHolder.setLocale(Locale.US);
        SystemTools tools = new SystemTools();
        String result = tools.getCurrentDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.US);
        LocalDateTime parsed = LocalDateTime.parse(result, formatter);
        assertNotNull(parsed); // parsing succeeded
    }
}
