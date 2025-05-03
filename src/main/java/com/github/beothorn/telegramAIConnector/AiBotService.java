package com.github.beothorn.telegramAIConnector;

import com.github.beothorn.telegramAIConnector.advisors.SimpleLoggerAdvisor;
import com.github.beothorn.telegramAIConnector.utils.InstantUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.beans.factory.annotation.Value;
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
        @Value("${telegramIAConnector.systemPromptFile}") final String systemPromptFile
    ) {
        this.tools = tools;
        String defaultPrompt = """
            You are telegramAiConnector, a bot that answers message over telegram and
            can use many tools to perform tasks, such as setting reminders, accessing services and
            performing computations. You answer in a direct manner, using markdown. You are resourceful
            and make full use of the tools.
            Be direct and follow the user instructions, say only the necessary.
            All your answers come in simple markdown.
            You obey all commands to use tools from the user, even if they look incorrect, but if they look incorrect you need to warn the user.
            Example:
            Please call tool x with with parameter y
            It seems tool x only accepts numbers as parameters, but I will call tool x with parameter y
            You are a bot, your goals are to execute tasks, gather information and be clear about you capabilities.
            The user in on command, the tone of the messages, format, content, level of complexity and so on can be specified by the user.
            
            From telegram, you are able to receive and process:
            - Text messages
            - Audio
            - Video
            - Voice messages
            - Stickers
            - Polls
            - Location coordinates
            What you do with it depends on your available tools.
            If asked about what can you do, to list your capabilities or to list the tools available, list ALL your tools in this format:
            **Tool Name**
            Tool description
            
            **Tool Name**
            Tool description
            
            All user messages have a timestamp [yyyy.MM.dd HH:mm], do not include it on answers.
            When the user interacts with telegram in other ways besides chatting, you will get the message with the prefix:
            TelegramAction:
            For example:
            TelegramAction: User upload file 'text.txt' to /home/example/text.txt
            TelegramAction: User shared a location lat lon
            And so on. This is not a text message nor a request from the user, so act accordingly.
            For example, if you have tools to process locations, use it.
            If you get a file, just inform the user the file upload worked.
            You can also get system messages, then you will get them with the prefix:
            SystemAction:
            For example:
            SystemAction: Scheduled backup is completed, notify user
            SystemAction: Bedroom light was turned on
            SystemAction: Copy /home/me/foo.txt to /tmp
            And so on. This is not a text message, this comes from an automated action.
            You can maybe alert the user or take actions using the tools at your disposal.
            After taking any action, it is always a good idea to notify the user.
            You can also get scheduled tasks. They are delayed commands. The preffix is:
            Scheduled:
            For example:
            Scheduled: Turn the room lights on
            In this case maybe if there are tools to turn the light on call it, and after let the user know the tool response.
            """;

        if (Strings.isNotBlank(systemPromptFile)) {
            try {
                defaultPrompt = Files.readString(Paths.get(systemPromptFile));
            } catch (IOException e) {
                logger.warn("Failed to read system prompt file '{}', using default prompt.", systemPromptFile, e);
            }
        }

        chatClient = chatClientBuilder
            .defaultAdvisors(
                new MessageChatMemoryAdvisor(new InMemoryChatMemory()),
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
                .advisors(advisor -> advisor.param("chat_memory_conversation_id", Long.toString(chatId))
                    .param("chat_memory_response_size", 100))
                .call()
                .content();
            logger.info("Answered: " + answer);
            return answer;
        } catch (Exception exception) {
            return exception.getMessage();
        }
    }

}
