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

    /**
     * Checks whether a chat is not logged in.
     *
     * @param chatId chat identifier
     * @return {@code true} if the chat is not logged in
     */
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

    /**
     * Attempts to authenticate a chat using the provided password.
     * If user has never changed their password, only the master password works.
     * If the user already changed the password, this will check if the password hash matches the db.
     *
     * @param chatId        chat identifier
     * @param passwordLogin password provided by the user
     * @return {@code true} if authentication succeeded
     */
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

    /**
     * Sets or updates the password for a chat.
     * When this is set, master password will no longer works.
     *
     * @param chatId        chat identifier
     * @param passwordLogin raw password to store
     */
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

    /**
     * Logs out the given chat.
     * This is global, the user will be logged from the chat no matter which device they are in.
     *
     * @param chatId chat identifier
     */
    public void logout(
        final Long chatId
    ) {
        loggedChats.remove(chatId);
        authenticationRepository.setLoggedState(chatId, false, "");
    }
}
