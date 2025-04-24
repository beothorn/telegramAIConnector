package com.github.beothorn.telegramAIConnector.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.i18n.LocaleContextHolder;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class SystemTools {

    @Tool(description = "Get the current date and time in the format Year.Month.Day Hour:Minute")
    public String getCurrentDateTime() {
        Locale locale = LocaleContextHolder.getLocale();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", locale);
        return LocalDateTime.now().format(formatter);
    }

    @Tool(description = "Returns detailed process information including PID, JVM name, Java version, start time, uptime, number of cores, and memory usage.")
    public String getMetaData() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        long uptimeMillis = runtime.getUptime();
        Instant startTime = Instant.ofEpochMilli(runtime.getStartTime());
        Duration uptime = Duration.ofMillis(uptimeMillis);

        String formattedStartTime = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(startTime);

        String pid = runtime.getName().split("@")[0];

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        long usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);

        return String.format("""
                        Process Info:
                        - PID: %s
                        - JVM Name: %s
                        - Java Version: %s
                        - Start Time: %s
                        - Uptime: %d hours, %d minutes, %d seconds
                        - Available Cores: %d
                        - Used Memory: %d MB
                        - Max Memory: %d MB
                        """,
                pid,
                runtime.getVmName(),
                System.getProperty("java.version"),
                formattedStartTime,
                uptime.toHoursPart(), uptime.toMinutesPart(), uptime.toSecondsPart(),
                availableProcessors,
                usedMemory,
                maxMemory
        );
    }


}