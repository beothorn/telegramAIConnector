package com.github.beothorn.telegramAIConnector.auth;

import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Component
public class Authentication {

    private final Logger logger = LoggerFactory.getLogger(Authentication.class);

    private final AuthenticationRepository authenticationRepository;
    private final String password;

    private final Set<Long> loggedChats = new HashSet<>();

    public Authentication(
        final AuthenticationRepository authenticationRepository,
        @Value("${telegram.password}") final String password
    ) {
        this.authenticationRepository = authenticationRepository;
        this.password = password;
    }

    public boolean isNotLogged(Long chatId) {
        if (loggedChats.contains(chatId)) return false;
        Optional<AuthData> authData = authenticationRepository.getAuthData(chatId);

        authData.ifPresent(a -> {
            if(a.logged()) {
                loggedChats.add(chatId);
            }
        });

        return !loggedChats.contains(chatId);
    }

    public boolean login(
        final Long chatId,
        final String passwordLogin
    ) {
        logger.info("Chat {} logging in.", chatId);
        if (Strings.isNotBlank(password) && Strings.isNotBlank(passwordLogin) && password.equals(passwordLogin)) {
            logger.info("Chat {} used master password.", chatId);
            return setLoggedInWithMasterPassword(chatId);
        }

        Optional<AuthData> authData = authenticationRepository.getAuthData(chatId);
        return authData.map(ad -> {
            String providedHash = hashPassword(passwordLogin);
            if (providedHash.equals(ad.password_hash())) {
                logger.info("Chat {} authenticated successfully.", chatId);
                setLoggedIn(chatId);
                return true;
            } else {
                return false;
            }
        }).orElseGet(() -> {
            logger.info("Chat {} failed authentication attempt.", chatId);
            return false;
        });
    }

    private boolean setLoggedInWithMasterPassword(Long chatId) {
        // If user does not exist, create one with master pass, if not, should use user password
        if(authenticationRepository.getAuthData(chatId).isPresent()) return false;
        logger.info("Setting chatId {} as logged", chatId);
        String masterPassHash = hashPassword(password);
        String expirationDate = LocalDate.now().plusMonths(2).toString();
        authenticationRepository.addAuthEntry(chatId, masterPassHash, true, expirationDate);
        loggedChats.add(chatId);
        return true;
    }

    private void setLoggedIn(Long chatId) {
        String expirationDate = LocalDate.now().plusMonths(2).toString();
        logger.info("Setting chatId {} as logged", chatId);
        authenticationRepository.setLoggedState(chatId, true, expirationDate);
        loggedChats.add(chatId);
    }

    public void setPasswordForUser(
            final Long chatId,
            final String passwordLogin
    ) {
        String passwordHash = hashPassword(passwordLogin);
        logger.info("Setting password for chatId {}", chatId);
        authenticationRepository.addAuthEntry(chatId, passwordHash, false, null);
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not supported", e);
        }
    }

    public void logout(
        final Long chatId
    ) {
        loggedChats.remove(chatId);
        authenticationRepository.setLoggedState(chatId, false, "");
    }
}
