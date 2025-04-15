package com.github.beothorn.telegramAIConnector;

import org.springframework.ai.chat.messages.MessageType;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class Messages {

    private final Map<Long, List<PersistedMessage>> messages = new HashMap<>();

    public void addUserMessage(
            final Long chatId,
            final String text
    ) {
        addMessage(chatId, MessageType.USER, text);
    }

    public void addAssistantMessage(
            final Long chatId,
            final String text
    ) {
        addMessage(chatId, MessageType.ASSISTANT, text);
    }

    private void addMessage(
        final Long chatId,
        final MessageType messageType,
        final String text
    ) {
        List<PersistedMessage> persistedMessages = messages.getOrDefault(chatId, new ArrayList<>());
        persistedMessages.add(new PersistedMessage(messageType.toString(), text));
        messages.put(chatId, persistedMessages);
    }

    public List<PersistedMessage> getMessages(
        final Long chatId
    ) {
        return messages.getOrDefault(chatId, List.of());
    }

}
