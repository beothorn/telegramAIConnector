package com.github.beothorn.telegramAIConnector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

@Service
public class AiBotService {

    Logger logger = LoggerFactory.getLogger(AiBotService.class);

    ChatClient chatClient;

    public AiBotService(ChatClient.Builder chatClientBuilder, ToolCallbackProvider tools) {
        chatClient = chatClientBuilder
                .defaultTools(tools)
                .build();
    }

    public String prompt(String prompt) {
        try {
            logger.info("Got prompt: " + prompt);
            String answer = chatClient.prompt(prompt).call().content();
            logger.info("Answered: " + answer);
            return answer;
        } catch (Exception exception) {
            return exception.getMessage();
        }
    }

}
