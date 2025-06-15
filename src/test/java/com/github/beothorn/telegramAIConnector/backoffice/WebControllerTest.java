package com.github.beothorn.telegramAIConnector.backoffice;

import com.github.beothorn.telegramAIConnector.tasks.TaskRepository;
import com.github.beothorn.telegramAIConnector.telegram.TelegramAiBot;
import com.github.beothorn.telegramAIConnector.user.MessagesRepository;
import com.github.beothorn.telegramAIConnector.user.UserRepository;
import com.github.beothorn.telegramAIConnector.user.profile.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class WebControllerTest {
    @Test
    void indexAddsAttributes() {
        TaskRepository tasks = mock(TaskRepository.class);
        MessagesRepository messages = mock(MessagesRepository.class);
        UserProfileRepository profiles = mock(UserProfileRepository.class);
        FileService files = mock(FileService.class);
        UserRepository users = mock(UserRepository.class);
        TelegramAiBot bot = mock(TelegramAiBot.class);
        when(bot.getBotName()).thenReturn("bot");
        WebController controller = new WebController(tasks,messages,profiles,files,users,bot);
        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.index(model);
        assertEquals("backoffice", view);
        assertEquals("bot", model.get("botName"));
    }
}
