package com.github.beothorn.telegramAIConnector.ai;

import ai.fal.client.ClientConfig;
import ai.fal.client.CredentialsResolver;
import ai.fal.client.FalClient;
import com.github.beothorn.telegramAIConnector.ai.tools.AIAnalysisTool;
import com.github.beothorn.telegramAIConnector.ai.tools.FalAiTools;
import com.github.beothorn.telegramAIConnector.ai.tools.SystemTools;
import com.github.beothorn.telegramAIConnector.telegram.TelegramTools;
import com.github.beothorn.telegramAIConnector.user.MessagesRepository;
import com.github.beothorn.telegramAIConnector.user.profile.advisors.UserProfileAdvisor;
import com.github.beothorn.telegramAIConnector.utils.InstantUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
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
import java.util.function.Consumer;

/**
 * Central point for the AI bot.
 * Responsible for getting user input and processing using various tools.
 */
@Service
public class AiBotService {

    private final Logger logger = LoggerFactory.getLogger(AiBotService.class);

    private final ChatClient chatClient;
    private final ToolCallbackProvider tools;
    private final UserProfileAdvisor userProfileAdvisor;
    private final FalClient falClient;
    private final ChatModel chatModel;
    private final String uploadFolder;

    public AiBotService(
        final ChatClient.Builder chatClientBuilder,
        final ToolCallbackProvider tools,
        final MessagesRepository messagesRepository,
        final UserProfileAdvisor userProfileAdvisor,
        final ChatModel chatModel,
        @Value("${telegramIAConnector.systemPromptFile}") final String systemPromptFile,
        @Value("classpath:prompt.txt") final Resource defaultPromptResource,
        @Value("${telegramIAConnector.messagesOnConversation}") final int messagesOnConversation,
        @Value("${fal.key:}") final String falKey,
        @Value("${telegramIAConnector.uploadFolder}") final String uploadFolder
    ) {
        this.tools = tools;
        this.userProfileAdvisor = userProfileAdvisor;
        this.chatModel = chatModel;
        this.uploadFolder = uploadFolder;
        if (Strings.isNotBlank(falKey)) {
            this.falClient = FalClient.withConfig(ClientConfig.withCredentials(CredentialsResolver.fromApiKey(falKey)));
        } else {
            this.falClient = null;
        }
        String defaultPrompt;
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
                .maxMessages(messagesOnConversation)
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

    /**
     * Sends a prompt to the underlying chat client using the provided tools and
     * returns the AI answer.
     * Using the base llm and tools provided, give returns a useful response.
     *
     * @param chatId      the conversation identifier
     * @param message     the message from the user
     * @param telegramTools telegram tools to send messages and files
     * @return the AI response or the error message if something fails
     */
    public String prompt(
        final Long chatId,
        final String message,
        final TelegramTools telegramTools
    ) {
        try {
            logger.debug("Got prompts");

            final String prompt = "[" + InstantUtils.currentTime() + "] " + message;

            ToolCallback[] toolCallbacks = tools.getToolCallbacks();
            List<ToolCallback> toolCallbackList = new ArrayList<>();

            final String uploadFolderForCurrentChat = uploadFolder + "/" + chatId;
            if (falClient != null) {
                FalAiTools falAiTools = new FalAiTools(falClient, uploadFolderForCurrentChat, telegramTools);
                toolCallbackList.addAll(Arrays.asList(ToolCallbacks.from(falAiTools)));
            }

            final AIAnalysisTool aiAnalysisTool = new AIAnalysisTool(chatModel, uploadFolderForCurrentChat);
            final SystemTools systemTools = new SystemTools();
            toolCallbackList.addAll(Arrays.asList(ToolCallbacks.from(systemTools, aiAnalysisTool)));

            toolCallbackList.addAll(Arrays.asList(toolCallbacks));

            Consumer<ChatClient.AdvisorSpec> chatMemoryConversationId = advisor ->
                    advisor.param("chat_memory_conversation_id", Long.toString(chatId));
            final String answer = chatClient
                .prompt(prompt)
                .toolCallbacks(toolCallbackList)
                .advisors(
                    chatMemoryConversationId
                )
                .advisors(userProfileAdvisor)
                .call()
                .content();
            logger.info("Answered: '{}'", answer);
            return answer;
        } catch (Exception exception) {
            logger.error("Failed prompt", exception);
            return exception.getMessage();
        }
    }

}
