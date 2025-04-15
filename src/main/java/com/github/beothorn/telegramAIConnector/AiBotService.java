package com.github.beothorn.telegramAIConnector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiBotService {

    private final Logger logger = LoggerFactory.getLogger(AiBotService.class);

    private final ChatClient chatClient;

    public AiBotService(ChatClient.Builder chatClientBuilder, ToolCallbackProvider tools) {
        chatClient = chatClientBuilder
                .defaultTools(tools)
                .build();
    }

    public String prompt(
        final List<PersistedMessage> messages,
        final Object... toolObjects
    ) {
        try {
            List<Message> promptMessages = messages.stream().map(m -> {
                if (m.messageType().equals(MessageType.USER.toString())) {
                    return new UserMessage(m.message());
                }
                if (m.messageType().equals(MessageType.SYSTEM.toString())) {
                    return new SystemMessage(m.message());
                }
                return new AssistantMessage(m.message());
            }).map(m -> (Message) m).toList();

            logger.info("Got prompts");
            Prompt prompt = new Prompt(promptMessages);

            String answer = chatClient
                    .prompt(prompt)
                    .tools(toolObjects)
                    .call()
                    .content();
            logger.info("Answered: " + answer);
            return answer;
        } catch (Exception exception) {
            return exception.getMessage();
        }
    }

}
