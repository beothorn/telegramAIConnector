package com.github.beothorn.telegramAIConnector.ai;

import com.github.beothorn.telegramAIConnector.user.MessagesRepository;
import com.github.beothorn.telegramAIConnector.utils.InstantUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class AiBotService {

    private final Logger logger = LoggerFactory.getLogger(AiBotService.class);

    private final ChatClient chatClient;
    private final ToolCallbackProvider tools;

    public AiBotService(
        final ChatClient.Builder chatClientBuilder,
        final ToolCallbackProvider tools,
        final MessagesRepository messagesRepository,
        @Value("${telegramIAConnector.systemPromptFile}") final String systemPromptFile,
        @Value("classpath:prompt.txt") final Resource defaultPromptResource
    ) {
        this.tools = tools;
        String defaultPrompt = "";
        try {
            defaultPrompt = new String(defaultPromptResource.getInputStream().readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (Strings.isNotBlank(systemPromptFile)) {
            try {
                defaultPrompt = Files.readString(Paths.get(systemPromptFile));
            } catch (IOException e) {
                logger.warn("Failed to read system prompt file '{}', using default prompt.", systemPromptFile, e);
            }
        }

        final MessageChatMemoryAdvisor messageChatMemoryAdvisor = MessageChatMemoryAdvisor.builder(
            MessageWindowChatMemory.builder()
                .chatMemoryRepository(
                    messagesRepository
                )
                .maxMessages(10)
            .build()
        ).build();
        chatClient = chatClientBuilder
            .defaultAdvisors(
                messageChatMemoryAdvisor,
                new SimpleLoggerAdvisor()
            )
            .defaultSystem(defaultPrompt)
            .build();
    }

    public String prompt(
        final Long chatId,
        final String message,
        final Object... toolObjects
    ) {
        try {
            logger.info("Got prompts");

            final String prompt = "[" + InstantUtils.currentTime() + "] " + message;

            ToolCallback[] toolCallbacks = tools.getToolCallbacks();
            List<ToolCallback> toolCallbackList = new ArrayList<>(Arrays.stream(toolObjects)
                    .map(ToolCallbacks::from)
                    .flatMap(Arrays::stream)
                    .toList());

            toolCallbackList.addAll(Arrays.asList(toolCallbacks));

            final String answer = chatClient
                .prompt(prompt)
                .toolCallbacks( toolCallbackList)
                .advisors(advisor -> advisor.param("chat_memory_conversation_id", Long.toString(chatId)))
                .call()
                .content();
            logger.info("Answered: '{}'", answer);
            return answer;
        } catch (Exception exception) {
            return exception.getMessage();
        }
    }

}
