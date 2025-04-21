package com.github.beothorn.telegramAIConnector;

import com.github.beothorn.telegramAIConnector.tools.SystemTools;
import com.github.beothorn.telegramAIConnector.tools.TelegramTools;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class TelegramAiBot implements LongPollingSingleThreadUpdateConsumer {

    private final AiBotService aiBotService;
    private final TelegramClient telegramClient;
    private final TaskSchedulerService taskSchedulerService;
    private final String password;
    private final String uploadFolder;

    private final Set<Long> loggedChats = new HashSet<>();

    private final Logger logger = LoggerFactory.getLogger(TelegramAiBot.class);

    public TelegramAiBot(
        final AiBotService aiBotService,
        final TaskSchedulerService taskSchedulerService,
        @Value("${telegram.key}") final String botToken,
        @Value("${telegram.password}") final String password,
        @Value("${telegramIAConnector.uploadFolder}") final String uploadFolder
    ) {
        this.aiBotService = aiBotService;
        telegramClient = new OkHttpTelegramClient(botToken);
        this.password = password;
        this.taskSchedulerService = taskSchedulerService;
        this.uploadFolder = uploadFolder;
    }

    @Override
    public void consume(final List<Update> updates) {
        updates.forEach(this::consume);
    }

    @Override
    public void consume(final Update update) {
        Long chatId = update.getMessage().getChatId();

        // If not logged in, only respond to login attempt
        if (!loggedChats.contains(chatId)) {
            if (!update.hasMessage()) return;
            if (!update.getMessage().hasText()) return;
            String text = update.getMessage().getText();
            consumeLogin(chatId, text);
            if (!loggedChats.contains(chatId)) {
                logger.info("Bad login attempt {}: {}", chatId, text);
                sendMessage(chatId, "Your chat id is " + chatId + ".You are talking to TelegramAIConnector. check instructions at https://github.com/beothorn/telegramAIConnector");
            }
            return;
        }

        if (update.hasMessage()) {
            try {
                consumeMessage(chatId, update);
            } catch (TelegramApiException e) {
                logger.error("Could not consume message", e);
                sendMessage(chatId, "Something went wrong '" + e.getMessage() + "'" );
            }
        }
    }

    public void sendMessage(
        final Long chatId,
        final String response
    ) {
        logger.info("Send message to {}: {}", chatId, response);
        SendMessage sendMessage = new SendMessage(Long.toString(chatId), response);
        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void setTyping(
            final Long chatId
    ) throws TelegramApiException {
        logger.info("Set typing to {}", chatId);
        String chatIdAsString = Long.toString(chatId);
        SendChatAction typingAction = SendChatAction.builder()
                .chatId(chatIdAsString)
                .action(ActionType.TYPING.toString())
                .build();

        telegramClient.execute(typingAction);
    }

    public void sendMarkdownMessage(
            final Long chatId,
            final String response
    ) throws TelegramApiException {
        logger.info("Send markdown message to {}: {}", chatId, response);
        String chatIdAsString = Long.toString(chatId);
        SendMessage sendMessage = SendMessage.builder()
            .parseMode(ParseMode.MARKDOWN)
            .chatId(chatIdAsString)
            .text(response)
            .build();
        telegramClient.execute(sendMessage);
    }

    public void sendFileWithCaption(
        final Long chatId,
        final String filePath,
        final String caption
    ) throws TelegramApiException {
        logger.info("Sending file to {}: {}", chatId, filePath);
        SendDocument sendDocument = SendDocument.builder()
            .chatId(chatId.toString())
            .document(new InputFile(new File(filePath)))
            .caption(caption)
            .build();

        telegramClient.execute(sendDocument);
    }

    public void consumeSystemMessage(
        final Long chatId,
        final String message
    ) throws TelegramApiException {
        logger.info("Consume system message: {}", message);

        final String text = "SystemAction: " + message;

        TelegramTools telegramTools = getTelegramTools(chatId);
        final String response = aiBotService.prompt(chatId, text, telegramTools, new SystemTools());

        logger.info("Response to " + chatId + ": " + text);
        sendMarkdownMessage(chatId, response);
    }

    private void consumeLogin(
        final Long chatId,
        final String loginCommand
    ) {
        logger.info("Consume login from {}: {}", chatId, loginCommand);

        String[] loginWithArgs = loginCommand.split("\\s+", 2);
        if (!loginWithArgs[0].equals("/login")) return;
        if (loginWithArgs.length != 2){
            sendMessage(chatId, "Invalid logged in.");
            return;
        }
        if (loginWithArgs[1].equals(password)) {
            logger.info("Logged in");
            loggedChats.add(chatId);
            sendMessage(chatId, "You are logged in.");
        } else {
            sendMessage(chatId, "You are NOT logged in.");
        }
    }

    private void consumeMessage(
        final Long chatId,
        final Update update
    ) throws TelegramApiException {
        logger.info("Consume message from {}", chatId);
        if (update.getMessage().hasText()) {
            String text = update.getMessage().getText();

            if (text.startsWith("/")) {
                String[] commandWithArgs = text.split("\\s+", 2);
                if (commandWithArgs.length == 1) {
                    consumeCommand(chatId, commandWithArgs[0], "");
                } else if (commandWithArgs.length == 2) {
                    consumeCommand(chatId, commandWithArgs[0], commandWithArgs[1]);
                } else {
                    throw new IllegalArgumentException("Bad command " + text);
                }
            } else {
                consumeText(chatId, text);
            }
        }
        if (update.getMessage().getDocument() != null) {
            Document document = update.getMessage().getDocument();
            String fileId = document.getFileId();
            String fileName = document.getFileName();

            downloadAndConsumeFile(chatId, fileId, fileName);
        }
        if (update.getMessage().hasPhoto()) {
            try {
                List<PhotoSize> photos = update.getMessage().getPhoto();
                PhotoSize largestPhoto = photos.getLast(); // largest version
                String fileId = largestPhoto.getFileId();

                downloadAndConsumeFile(chatId, fileId);
            } catch (Exception e) {
                logger.error("Failed to process photo", e);
                sendMessage(chatId, "Failed to process photo: " + e.getMessage());
            }
        }
        if (update.getMessage().hasVideo()) {
            try {
                var video = update.getMessage().getVideo();
                var fileId = video.getFileId();

                downloadAndConsumeFile(chatId, fileId);
            } catch (Exception e) {
                logger.error("Failed to process video", e);
                sendMessage(chatId, "Failed to process video: " + e.getMessage());
            }
            return;
        }
        if (update.getMessage().hasLocation()) {
            double lat = update.getMessage().getLocation().getLatitude();
            double lon = update.getMessage().getLocation().getLongitude();
            try {
                consumeLocation(chatId, lat, lon);
            } catch (TelegramApiException e) {
                logger.error("Failed to process location", e);
                sendMessage(chatId, "Failed to process location: " + e.getMessage());
            }
        }
    }

    private void downloadAndConsumeFile(Long chatId, String fileId) throws TelegramApiException {
        downloadAndConsumeFile(chatId, fileId, null);
    }

    private void downloadAndConsumeFile(Long chatId, String fileId, String fileNameMaybe) throws TelegramApiException {
        GetFile getFileMethod = new GetFile(fileId);
        org.telegram.telegrambots.meta.api.objects.File telegramFile = telegramClient.execute(getFileMethod);

        String originalFilePath = telegramFile.getFilePath();
        String originalFileName = Paths.get(originalFilePath).getFileName().toString();

        String fileName = (fileNameMaybe != null) ? fileNameMaybe : System.currentTimeMillis() + "_" + originalFileName;

        File downloadedFile = telegramClient.downloadFile(telegramFile);
        logger.info("Downloaded file {} to {}", fileName, downloadedFile.getAbsolutePath());
        Path outputDir = Paths.get(uploadFolder);
        try {
            Files.createDirectories(outputDir);
            Path destinationPath = outputDir.resolve(fileName);
            Files.copy(downloadedFile.toPath(), destinationPath, StandardCopyOption.REPLACE_EXISTING);
            sendMessage(chatId, "File saved to: " + destinationPath);
            consumeFile(chatId, destinationPath);
        } catch (IOException e) {
            logger.error("Error saving file", e);
            sendMessage(chatId, "Error saving file: " + e.getMessage());
        }
    }

    private void consumeCommand(
        final Long chatId,
        final String command,
        final String args
    ) {
        logger.info("Consume command from {}: {} {}", chatId, command, args);

        if (command.equalsIgnoreCase("chatId")) {
            sendMessage(chatId, "Your chat id is " + chatId);
        }
    }

    private void consumeText(
        final Long chatId,
        final String text
    ) throws TelegramApiException {
        logger.info("Consume text from {}: {}", chatId, text);
        sendTypingCommand(chatId);

        TelegramTools telegramTools = getTelegramTools(chatId);
        final String response = aiBotService.prompt(chatId, text, telegramTools, new SystemTools());

        logger.info("Response to " + chatId + ": " + text);
        sendMarkdownMessage(chatId, response);
    }

    private void consumeFile(
        final Long chatId,
        final Path uploadedFile
    ) throws TelegramApiException {
        String uploadedFileString = uploadedFile.toString();
        logger.info("Consume file from {}: {}", chatId, uploadedFileString);
        sendTypingCommand(chatId);
        String text = "SystemAction: User upload file to '" + uploadedFileString + "'.";
        TelegramTools telegramTools = getTelegramTools(chatId);
        final String response = aiBotService.prompt(chatId, text, telegramTools, new SystemTools());
        logger.info("Response to " + chatId + ": " + text);
        sendMarkdownMessage(chatId, response);
    }

    private void consumeLocation(
            final Long chatId,
            final double latitude,
            final double longitude
    ) throws TelegramApiException {
        logger.info("Consume location from {}: {}, {}", chatId, latitude, longitude);
        sendTypingCommand(chatId);

        String locationMessage = String.format("TelegramAction: User shared a location %f %f", latitude, longitude);

        TelegramTools telegramTools = getTelegramTools(chatId);
        final String response = aiBotService.prompt(chatId, locationMessage, telegramTools, new SystemTools());

        logger.info("Response to {} for location: {}", chatId, locationMessage);
        sendMarkdownMessage(chatId, response);
    }

    private void sendTypingCommand(Long chatId) {
        try {
            setTyping(chatId);
        } catch (TelegramApiException e) {
            // Not important if it fails
            logger.error("Could not set status to typing");
        }
    }

    @NotNull
    private TelegramTools getTelegramTools(Long chatId) {
        return new TelegramTools(this, aiBotService, taskSchedulerService, chatId, uploadFolder);
    }
}
