package com.github.beothorn.telegramAIConnector.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class SystemTools {

    @Tool(description = "Get the current date and time in the format Year.Month.Day Hour:Minute")
    public String getCurrentDateTime() {
        Locale locale = LocaleContextHolder.getLocale();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", locale);
        return LocalDateTime.now().format(formatter);
    }
}