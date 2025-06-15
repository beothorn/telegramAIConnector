package com.github.beothorn.telegramAIConnector.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UserInfoTest {
    @Test
    void recordValues() {
        UserInfo info = new UserInfo(1L,"u","f","l");
        assertEquals(1L, info.chatId());
        assertEquals("u", info.username());
        assertEquals("f", info.firstName());
        assertEquals("l", info.lastName());
    }
}
