package com.github.beothorn.telegramAIConnector.ai.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.i18n.LocaleContextHolder;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.jar.Manifest;

public class SystemTools {

    @Tool(description = "Get the current date and time in the format Year.Month.Day Hour:Minute")
    public String getCurrentDateTime() {
        Locale locale = LocaleContextHolder.getLocale();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", locale);
        return LocalDateTime.now().format(formatter);
    }

    @Tool(description = "Get telegramAIConnector version.")
    public String getVersion() throws IOException {
        InputStream manifestStream = SystemTools.class.getClassLoader()
                .getResourceAsStream("META-INF/MANIFEST.MF");

        if (manifestStream != null) {
            Manifest manifest = new Manifest(manifestStream);
            String version = manifest.getMainAttributes().getValue("Implementation-Version");
            return("Version: " + version);
        }
        return "Could not determine version";
    }
}