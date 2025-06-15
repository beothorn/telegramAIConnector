package com.github.beothorn.telegramAIConnector.persistence;

import com.github.beothorn.telegramAIConnector.auth.AuthenticationRepository;
import com.github.beothorn.telegramAIConnector.tasks.TaskRepository;
import com.github.beothorn.telegramAIConnector.user.MessagesRepository;
import com.github.beothorn.telegramAIConnector.user.UserRepository;
import com.github.beothorn.telegramAIConnector.user.profile.UserProfileRepository;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;

public class SQLiteTest {
    @Test
    void constructorInitializesRepositories() {
        MessagesRepository m = mock(MessagesRepository.class);
        TaskRepository t = mock(TaskRepository.class);
        UserRepository u = mock(UserRepository.class);
        AuthenticationRepository a = mock(AuthenticationRepository.class);
        UserProfileRepository p = mock(UserProfileRepository.class);
        new SQLite("folder", m, t, u, a, p);
        verify(m).initDatabase(startsWith("jdbc:sqlite:"));
        verify(t).initDatabase(anyString());
        verify(u).initDatabase(anyString());
        verify(a).initDatabase(anyString());
        verify(p).initDatabase(anyString());
    }
}
