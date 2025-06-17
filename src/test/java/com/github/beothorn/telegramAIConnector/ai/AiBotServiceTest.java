package com.github.beothorn.telegramAIConnector.ai;

import com.github.beothorn.telegramAIConnector.user.MessagesRepository;
import com.github.beothorn.telegramAIConnector.user.profile.advisors.UserProfileAdvisor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.core.io.ByteArrayResource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AiBotServiceTest {

    /**
     * Asserts that the prompt gets processed.
     * This is a wiring test and very prone to be changed as it tests internal structure.
     * If spring changes, we will probably need to change this test.
     */
    @Test
    void promptDelegatesToChatClient() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient chatClient = mock(ChatClient.class, Mockito.RETURNS_DEEP_STUBS);
        when(builder.defaultAdvisors(any(Advisor.class), any())).thenReturn(builder);
        when(builder.defaultSystem(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(chatClient);

        ChatClient.ChatClientRequestSpec spec = mock(ChatClient.ChatClientRequestSpec.class, Mockito.RETURNS_DEEP_STUBS);
        when(chatClient.prompt(anyString())).thenReturn(spec);
        when(spec.toolCallbacks(anyList())).thenReturn(spec);
        when(spec.advisors(any(java.util.function.Consumer.class))).thenReturn(spec);
        when(spec.advisors(any(Advisor.class))).thenReturn(spec);
        ChatClient.CallResponseSpec call = mock(ChatClient.CallResponseSpec.class);
        when(spec.call()).thenReturn(call);
        when(call.content()).thenReturn("answer");

        ToolCallbackProvider provider = mock(ToolCallbackProvider.class);
        when(provider.getToolCallbacks()).thenReturn(new org.springframework.ai.tool.ToolCallback[]{});
        MessagesRepository messagesRepository = mock(MessagesRepository.class);
        UserProfileAdvisor advisor = mock(UserProfileAdvisor.class);

        AiBotService service = new AiBotService(
                builder,
                provider,
                messagesRepository,
                advisor,
                null,
                null,
                "",
                new ByteArrayResource("def".getBytes()),
                1,
                "upload"
        );

        String result = service.prompt(1L, "hi", null);
        assertEquals("answer", result);
    }
}
