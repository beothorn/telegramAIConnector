package com.github.beothorn.telegramAIConnector.ai.advisors;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;

// TODO: This will inject the user profile to customize responses based on user knowledge level
public class UserProfileAdvisor implements CallAdvisor {

    @Override
    public ChatClientResponse adviseCall(
        final ChatClientRequest chatClientRequest,
        final CallAdvisorChain callAdvisorChain
    ) {
        return null;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
