package com.github.beothorn.telegramAIConnector.user.profile.advisors;

import com.github.beothorn.telegramAIConnector.user.profile.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.core.io.ByteArrayResource;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class UserProfileAdvisorTest {
    /**
     * Ensures the advisor stores the profile returned by the AI model.
     */
    @Test
    void adviseUpdatesProfile() {
        ChatModel model = mock(ChatModel.class);
        when(model.call(anyString())).thenReturn("new");
        UserProfileRepository repo = mock(UserProfileRepository.class);
        when(repo.getProfile(1L)).thenReturn(Optional.of("old"));
        UserProfileAdvisor advisor = new UserProfileAdvisor(model, repo, new ByteArrayResource("%s %s".getBytes()));
        ChatClientRequest request = ChatClientRequest.builder()
                .prompt(new org.springframework.ai.chat.prompt.Prompt(new SystemMessage("s"), new UserMessage("u")))
                .context(Map.of("chat_memory_conversation_id","1"))
                .build();
        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientResponse resp = mock(ChatClientResponse.class);
        when(chain.nextCall(any())).thenReturn(resp);
        ChatClientResponse r = advisor.adviseCall(request, chain);
        assertSame(resp, r);
        verify(repo).setProfile(1L, "new");
    }
}
