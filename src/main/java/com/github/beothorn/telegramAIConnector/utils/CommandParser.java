package com.github.beothorn.telegramAIConnector.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses command arguments supporting quoted strings.
 */
public class CommandParser {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\"([^\"]*)\"|(\\S+)");

    /**
     * Splits an argument string into at most two tokens. Tokens can be quoted
     * with double quotes to include spaces.
     *
     * @param args the raw arguments string
     * @return an array with exactly two elements. Missing elements are empty strings.
     */
    public static String[] parseTwoArguments(final String args) {
        String[] result = new String[]{"", ""};
        if (args == null) {
            return result;
        }
        Matcher matcher = TOKEN_PATTERN.matcher(args);
        int i = 0;
        while (matcher.find() && i < 2) {
            String token = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            if (token == null) {
                token = "";
            }
            result[i++] = token;
        }
        return result;
    }
}
