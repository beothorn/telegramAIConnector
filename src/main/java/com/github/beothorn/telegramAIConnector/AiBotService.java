package com.github.beothorn.telegramAIConnector;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

@Service
public class AiBotService {

    ChatClient chatClient;

    public AiBotService(ChatClient.Builder chatClientBuilder, ToolCallbackProvider tools) {
        chatClient = chatClientBuilder
                .defaultTools(tools)
                .build();
    }

    public String prompt(String prompt) {
        return chatClient.prompt(prompt).call().content();
    }

}
