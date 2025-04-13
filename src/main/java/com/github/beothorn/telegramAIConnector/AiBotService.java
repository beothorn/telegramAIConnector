package com.github.beothorn.telegramAIConnector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;

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
        final String promptText,
        final Object... toolObjects
    ) {
        try {
            logger.info("Got prompt: " + promptText);
            String answer = chatClient
                    .prompt()
                    .user(promptText)
                    .tools(new DateTimeTools())
                    .tools(toolObjects)
                    .call()
                    .content();
            logger.info("Answered: " + answer);
            return answer;
        } catch (Exception exception) {
            return exception.getMessage();
        }
    }

    public String prompt2(String promptText) {
        try {
            logger.info("Got prompt: " + promptText);
            final UserMessage userMessage = new UserMessage(promptText);
            final Prompt prompt = new Prompt(userMessage);
            ChatClient.CallResponseSpec call = chatClient.prompt(prompt).call();
            final String answer = call.content();
            logger.info("Answered: " + answer);
            return answer;
        } catch (Exception exception) {
            return exception.getMessage();
        }
    }

    public String prompts(String prompt) {
        try {
            logger.info("Got prompt: " + prompt);
            ArrayList<Message> messages = new ArrayList<>();
            new Prompt(messages);
            String answer = chatClient.prompt(prompt).call().content();
            logger.info("Answered: " + answer);
            return answer;
        } catch (Exception exception) {
            return exception.getMessage();
        }
    }

}
