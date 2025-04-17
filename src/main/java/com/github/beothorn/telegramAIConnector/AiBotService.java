package com.github.beothorn.telegramAIConnector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
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
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(new InMemoryChatMemory()),
                        new SimpleLoggerAdvisor()
                )
                .defaultTools(tools)
                .defaultSystem("""
                        You are telegramAiConnector, a bot that answers message over telegram and 
                        can use many tools to perform tasks, such as setting reminders, accessing services and 
                        performing computations. You answer in a direct manner, using markdown. You are resourceful 
                        and make full use of the tools. 
                        Refrain from asking if the user needs more assistance. It is a chat, if the user needs more 
                        they will tell. 
                        Be direct and follow the user instructions say the necessary.
                        """)
                .build();
    }

    public String prompt(
        final Long chatId,
        final String message,
        final Object... toolObjects
    ) {
        try {
            logger.info("Got prompts");

            String answer = chatClient
                    .prompt(message)
                    .advisors(advisor -> advisor.param("chat_memory_conversation_id", Long.toString(chatId))
                            .param("chat_memory_response_size", 100))
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
