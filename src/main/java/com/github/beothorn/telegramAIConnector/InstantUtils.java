package com.github.beothorn.telegramAIConnector;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class InstantUtils {
    public static Instant parseToInstant(String dateTimeStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
        LocalDateTime localDateTime = LocalDateTime.parse(dateTimeStr, formatter);
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    public static String formatFromInstant(Instant instant) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        return localDateTime.format(formatter);
    }
}
