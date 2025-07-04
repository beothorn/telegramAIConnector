package com.github.beothorn.telegramAIConnector.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class InstantUtils {

    public static final String DATE_TIME_FORMAT = "yyyy.MM.dd HH:mm";

    /**
     * Parses a string formatted as {@link #DATE_TIME_FORMAT} to an {@link Instant}.
     *
     * @param dateTimeStr date and time string
     * @return parsed instant
     */
    public static Instant parseToInstant(String dateTimeStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
        LocalDateTime localDateTime = LocalDateTime.parse(dateTimeStr, formatter);
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    /**
     * Formats the given instant using {@link #DATE_TIME_FORMAT}.
     *
     * @param instant instant to format
     * @return formatted string
     */
    public static String formatFromInstant(Instant instant) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        return localDateTime.format(formatter);
    }

    /**
     * Returns the current time formatted with {@link #DATE_TIME_FORMAT}.
     *
     * @return current time formatted
     */
    public static String currentTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
        return localDateTime.format(formatter);
    }

    /**
     * Returns the current time with seconds precision.
     *
     * @return current time including seconds
     */
    public static String currentTimeSeconds() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd_HH:mm_ss");
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
        return localDateTime.format(formatter);
    }
}
