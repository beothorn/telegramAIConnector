package com.github.beothorn.telegramAIConnector.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuthenticationTest {
    static class InMemoryRepo extends AuthenticationRepository {
        Map<Long, AuthData> data = new HashMap<>();
        @Override
        public void initDatabase(String dbUrl) {}
        @Override
        public AuthData addAuthEntry(long chatId, String passwordHash, boolean logged, String logExpirationDate) {
            AuthData ad = new AuthData(chatId, passwordHash, logged, logExpirationDate);
            data.put(chatId, ad);
            return ad;
        }
        @Override
        public void setLoggedState(long chatId, boolean logged, String logExpirationDate) {
            AuthData ad = data.get(chatId);
            if(ad!=null) data.put(chatId,new AuthData(chatId, ad.password_hash(), logged, logExpirationDate));
        }
        @Override
        public Optional<AuthData> getAuthData(long chatId) {
            return Optional.ofNullable(data.get(chatId));
        }
    }

    InMemoryRepo repo;

    @BeforeEach
    void setup() {
        repo = new InMemoryRepo();
    }

    @Test
    void loginWithMasterPasswordCreatesEntry() {
        Authentication auth = new Authentication(repo, "master");
        assertTrue(auth.login(10L, "master"));
        assertFalse(auth.isNotLogged(10L));
        assertTrue(repo.getAuthData(10L).isPresent());
    }

    @Test
    void loginWithUserPasswordChecksHash() {
        Authentication auth = new Authentication(repo, "master");
        auth.setPasswordForUser(20L, "pw");
        assertTrue(auth.login(20L, "pw"));
        assertFalse(auth.isNotLogged(20L));
    }

    @Test
    void loginWithMasterOnlyWorksForUserWithNoPass() {
        Authentication auth = new Authentication(repo, "master");
        assertTrue(auth.login(10L, "master"));
        auth.setPasswordForUser(10L, "pw");
        assertFalse(auth.login(10L, "master"));
    }
}
