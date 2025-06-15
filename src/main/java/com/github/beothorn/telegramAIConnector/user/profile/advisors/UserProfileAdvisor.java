package com.github.beothorn.telegramAIConnector.user.profile.advisors;

import com.github.beothorn.telegramAIConnector.user.profile.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Appends a user profile to the prompt.
 * This way, in theory, answers are better tailored to the user.
 */
@Service
public class UserProfileAdvisor implements CallAdvisor {

    private final ChatModel chatModel;
    private final UserProfileRepository userProfileRepository;
    private final String prompt;
    private final Logger logger = LoggerFactory.getLogger(UserProfileAdvisor.class);

    /**
     * Creates the advisor using the given chat model and repository.
     */
    public UserProfileAdvisor(
        final ChatModel chatModel,
        final UserProfileRepository userProfileRepository,
        @Value("classpath:profilePrompt.txt") final Resource defaultPromptResource
    ) {
        this.chatModel = chatModel;
        this.userProfileRepository = userProfileRepository;
        try {
            prompt = new String(defaultPromptResource.getInputStream().readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Updates the user profile using the last message before invoking the chain.
     */
    @Override
    public ChatClientResponse adviseCall(
        final ChatClientRequest chatClientRequest,
        final CallAdvisorChain callAdvisorChain
    ) {

        UserMessage currentUserMessage = chatClientRequest.prompt().getUserMessage();

        logger.debug("Chat id is {}", chatClientRequest.context().get("chat_memory_conversation_id"));

        final long chatId = Long.parseLong((String) chatClientRequest.context().get("chat_memory_conversation_id"));

        // given the current user profile and the last message, ask the AI to
        // update the profile if we found the user skill level in a subject
        // example, speaks native spanish (so we can switch to spanish)
        // is a nurse (so we can use professional medical language) and so on
        // also the opposite, ex does not speak english (so avoid it)

        final String userProfile = userProfileRepository.getProfile(chatId).orElse("");
        final String profilePrompt = String.format(prompt, userProfile, currentUserMessage.getText());

        final String newProfile = chatModel.call(profilePrompt);
        userProfileRepository.setProfile(chatId, newProfile);
        logger.debug("Profile updated to '{}'", newProfile);

        final SystemMessage systemMessage = chatClientRequest.prompt().getSystemMessage();
        ChatClientRequest processedChatClientRequest = chatClientRequest.mutate()
                .prompt(chatClientRequest.prompt().augmentSystemMessage(systemMessage + "\nThis is the profile of the user you are talking to." +
                        "\nUse it to give the best, most personalized answer possible:\n" +
                        newProfile))
                .build();

        return callAdvisorChain.nextCall(processedChatClientRequest);
    }

    /**
     * Returns the advisor name.
     */
    @Override
    public String getName() {
        return "UserProfileAdvisor";
    }

    /**
     * Advisors are executed in default order 0.
     */
    @Override
    public int getOrder() {
        return 0;
    }
}
