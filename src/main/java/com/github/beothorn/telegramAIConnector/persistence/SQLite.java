package com.github.beothorn.telegramAIConnector.persistence;

import com.github.beothorn.telegramAIConnector.auth.AuthenticationRepository;
import com.github.beothorn.telegramAIConnector.tasks.TaskRepository;
import com.github.beothorn.telegramAIConnector.user.MessagesRepository;
import com.github.beothorn.telegramAIConnector.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SQLite {

    private static final Logger logger = LoggerFactory.getLogger(SQLite.class);

    private final String dbUrl;

    public SQLite(
        @Value("${telegramIAConnector.dbFilesFolder}")  final String dbFolder,
        final MessagesRepository messagesRepository,
        final TaskRepository taskRepository,
        final UserRepository userRepository,
        final AuthenticationRepository authenticationRepository
    ) {
        this.dbUrl = "jdbc:sqlite:" + dbFolder + "/telegramAIConnector.db";
        logger.info("Connection string is '{}'", dbUrl);
        messagesRepository.initDatabase(dbUrl);
        taskRepository.initDatabase(dbUrl);
        userRepository.initDatabase(dbUrl);
        authenticationRepository.initDatabase(dbUrl);
    }

}
