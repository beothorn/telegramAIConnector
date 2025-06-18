package com.github.beothorn.telegramAIConnector.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class CommandParserTest {

    @Test
    void parsesQuotedArguments() {
        String[] res = CommandParser.parseTwoArguments("\"foo bar.png\" \"bar baz.png\"");
        assertArrayEquals(new String[]{"foo bar.png", "bar baz.png"}, res);
    }

    @Test
    void handlesSingleQuotedArg() {
        String[] res = CommandParser.parseTwoArguments("\"foo bar.png\" bar");
        assertArrayEquals(new String[]{"foo bar.png", "bar"}, res);
    }

    @Test
    void handlesMissingSecondArg() {
        String[] res = CommandParser.parseTwoArguments("onearg");
        assertArrayEquals(new String[]{"onearg", ""}, res);
    }

    @Test
    void handlesEmptyInput() {
        String[] res = CommandParser.parseTwoArguments("");
        assertArrayEquals(new String[]{"", ""}, res);
    }
}
