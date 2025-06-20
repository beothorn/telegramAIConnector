package com.github.beothorn.telegramAIConnector.backoffice;

import com.github.beothorn.telegramAIConnector.auth.Authentication;
import com.github.beothorn.telegramAIConnector.tasks.TaskRepository;
import com.github.beothorn.telegramAIConnector.telegram.TelegramAiBot;
import com.github.beothorn.telegramAIConnector.user.MessagesRepository;
import com.github.beothorn.telegramAIConnector.user.UserRepository;
import com.github.beothorn.telegramAIConnector.user.profile.UserProfileRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ApiTest {

    /**
     * Smoke test api, check that all conversations returns when getting them.
     * @throws Exception
     */
    @Test
    void getConversationIdsDelegates() throws Exception {
        TelegramAiBot bot = mock(TelegramAiBot.class);
        TaskRepository tasks = mock(TaskRepository.class);
        MessagesRepository messages = mock(MessagesRepository.class);
        Authentication auth = mock(Authentication.class);
        UserProfileRepository profiles = mock(UserProfileRepository.class);
        UserRepository users = mock(UserRepository.class);
        FileService files = mock(FileService.class);

        when(messages.findConversationIds()).thenReturn(List.of("1"));
        Api api = new Api(bot,tasks,messages,auth,profiles,files, users);
        assertEquals(List.of("1"), api.getConversationIds());
    }

    /**
     * Broadcasts a message to every chat id returned by the repository.
     */
    @Test
    void broadcastDelegates() {
        TelegramAiBot bot = mock(TelegramAiBot.class);
        TaskRepository tasks = mock(TaskRepository.class);
        MessagesRepository messages = mock(MessagesRepository.class);
        Authentication auth = mock(Authentication.class);
        UserProfileRepository profiles = mock(UserProfileRepository.class);
        UserRepository users = mock(UserRepository.class);
        FileService files = mock(FileService.class);

        when(messages.findConversationIds()).thenReturn(List.of("1","2"));
        Api api = new Api(bot,tasks,messages,auth,profiles,files, users);
        api.broadcast("hi");

        org.mockito.Mockito.verify(bot).sendMessage(1L, "hi");
        org.mockito.Mockito.verify(bot).sendMessage(2L, "hi");
    }

    /**
     * Sends a system message to a contact.
     */
    @Test
    void systemMessageDelegates() throws Exception {
        TelegramAiBot bot = mock(TelegramAiBot.class);
        TaskRepository tasks = mock(TaskRepository.class);
        MessagesRepository messages = mock(MessagesRepository.class);
        Authentication auth = mock(Authentication.class);
        UserProfileRepository profiles = mock(UserProfileRepository.class);
        UserRepository users = mock(UserRepository.class);
        FileService files = mock(FileService.class);

        when(bot.consumeSystemMessage(1L, "hi")).thenReturn("ok");
        Api api = new Api(bot,tasks,messages,auth,profiles,files, users);
        String resp = api.systemMessage(1L, "hi");
        assertEquals("ok", resp);
    }
}
