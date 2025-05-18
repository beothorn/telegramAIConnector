package com.github.beothorn.telegramAIConnector.user.profile.advisors;

import com.github.beothorn.telegramAIConnector.ai.AiBotService;
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
import org.springframework.stereotype.Service;

@Service
public class UserProfileAdvisor implements CallAdvisor {

    private final ChatModel chatModel;
    private final UserProfileRepository userProfileRepository;

    private final Logger logger = LoggerFactory.getLogger(UserProfileAdvisor.class);

    public UserProfileAdvisor(
        final ChatModel chatModel,
        final UserProfileRepository userProfileRepository
    ) {
        this.chatModel = chatModel;
        this.userProfileRepository = userProfileRepository;
    }

    @Override
    public ChatClientResponse adviseCall(
        final ChatClientRequest chatClientRequest,
        final CallAdvisorChain callAdvisorChain
    ) {
        final SystemMessage systemMessage = chatClientRequest.prompt().getSystemMessage();
        String augmentedSystemText = systemMessage.getText() + "\nStart the message with Heyooooo .";
        ChatClientRequest processedChatClientRequest = chatClientRequest.mutate()
                .prompt(chatClientRequest.prompt().augmentSystemMessage(augmentedSystemText))
                .build();
        UserMessage currentUserMessage = chatClientRequest.prompt().getUserMessage();

        logger.debug("Chat id is {}", chatClientRequest.context().get("chat_memory_conversation_id"));

        Long chatId = Long.getLong((String) chatClientRequest.context().get("chat_memory_conversation_id"));

        if (chatId == null) {
            logger.error("Chat id is null???");
            return callAdvisorChain.nextCall(chatClientRequest);
        }


        // TODO: given the current user profile and the last message, ask the AI to
        // update the profile if we found the user skill level in a subject
        // example, speaks native spanish (so we can switch to spanish)
        // is a nurse (so we can use professional medical language) and so on
        // also the opposite, ex does not speak english (so avoid it)

        String profilePrompt = String.format("""
                Given this profile:
                %s
                
                Given this message:
                %s
                
                Extract user preferences and expertise:
                - Preferred name and pronoun
                - Age, family status
                - Family and friends
                - Preferred language (e.g., English, Spanish)
                - Profession and profession level
                - Hobbies
                - Dislikes
                - Location (where user lives and works)
                - General skills with level
                - Custom preferences such as request on how to format the answer
                - Skill level in domain-specific terms (e.g., beginner, expert)
                Return only the updated profile.
                Your answer will be used as the new profile, so don`t add any explanation.
                If no new information is on the message, just repeat the old profile.
                """, userProfileRepository.getProfile(chatId) , currentUserMessage.getText());

        String call = chatModel.call(profilePrompt);
        userProfileRepository.setProfile(chatId, call);
        logger.debug("Profile updated to '{}'", call);

        return callAdvisorChain.nextCall(processedChatClientRequest);
    }

    @Override
    public String getName() {
        return "UserProfileAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
