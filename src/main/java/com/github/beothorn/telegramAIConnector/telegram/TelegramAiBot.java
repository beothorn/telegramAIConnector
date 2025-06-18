package com.github.beothorn.telegramAIConnector.telegram;

import ai.fal.client.FalClient;
import com.github.beothorn.telegramAIConnector.ai.AiBotService;
import com.github.beothorn.telegramAIConnector.ai.tools.AIAnalysisTool;
import com.github.beothorn.telegramAIConnector.ai.tools.FalAiTools;
import com.github.beothorn.telegramAIConnector.auth.Authentication;
import com.github.beothorn.telegramAIConnector.tasks.TaskScheduler;
import com.github.beothorn.telegramAIConnector.user.UserRepository;
import com.github.beothorn.telegramAIConnector.utils.InstantUtils;
import jakarta.annotation.PreDestroy;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.polls.Poll;
import org.telegram.telegrambots.meta.api.objects.polls.PollOption;
import org.telegram.telegrambots.meta.api.objects.stickers.Sticker;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Receives and send messages, files and any other media types to Telegram.
 */
@Component
public class TelegramAiBot implements LongPollingSingleThreadUpdateConsumer {

    private final AiBotService aiBotService;
    private final TelegramClient telegramClient;
    private final TaskScheduler taskScheduler;
    private final Authentication authentication;
    private final UserRepository userRepository;
    private final Commands commands;
    private final AIAnalysisTool aiAnalysisTool;
    private final FalClient falClient;
    private final String uploadFolder;
    private final ProcessingStatus processingStatus;
    private String botName = "";
    private final ExecutorService executor;
    private final ScheduledExecutorService typingScheduler;

    private final Logger logger = LoggerFactory.getLogger(TelegramAiBot.class);

    public TelegramAiBot(
        final AiBotService aiBotService,
        final TaskScheduler taskScheduler,
        final Authentication authentication,
        final UserRepository userRepository,
        final Commands commands,
        final ChatModel chatModel,
        final FalClient falClient,
        final ProcessingStatus processingStatus,
        @Value("${telegram.key}") final String botToken,
        @Value("${telegramIAConnector.uploadFolder}") final String uploadFolder
    ) {
        this.aiBotService = aiBotService;
        this.falClient = falClient;
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.taskScheduler = taskScheduler;
        this.authentication = authentication;
        this.userRepository = userRepository;
        this.commands = commands;
        this.aiAnalysisTool = new AIAnalysisTool(chatModel, uploadFolder);
        this.uploadFolder = uploadFolder;
        this.processingStatus = processingStatus;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.typingScheduler = Executors.newScheduledThreadPool(1);

        try {
            final User userBot = telegramClient.execute(new GetMe());
            this.botName = userBot.getUserName();
            logger.info("¨Bot username: '{}'", botName);
        } catch (TelegramApiException e) {
            logger.error("Could not get bot info." ,e);
        }
    }

    @Override
    public void consume(final List<Update> updates) {
        logger.debug("Got {} updates.", updates.size());
        updates.forEach(this::consume);
    }

    @Override
    public void consume(final Update update) {
        logger.debug("Received update {}", update);
        logger.debug("Update hasMessage is {}", update.hasMessage());
        if (!update.hasMessage()) {
            logger.info("Update with no message, skipping");
            return;
        }
        final Message message = update.getMessage();
        logger.debug("Received message {}", message);
        final Long chatId = message.getChatId();
        logger.debug("Received update from chatId {}", chatId);

        final User from = message.getFrom();

        logger.debug("Will create user {}", from);
        userRepository.createOrUpdateUser(
            chatId,
            from.getUserName(),
            from.getFirstName(),
            from.getLastName()
        );

        // If not logged in, only respond to login attempt
        if (authentication.isNotLogged(chatId)) {
            if (!update.hasMessage()) return;
            if (!message.hasText()) return;
            final String text = message.getText();
            consumeLogin(chatId, text);
            return;
        }

        // This should be unreachable if not logged in
        try {
            consumeMessage(chatId, update);
        } catch (TelegramApiException e) {
            logger.error("Could not consume message", e);
            sendMessage(chatId, "Something went wrong '" + e.getMessage() + "'" );
        }
    }

    /**
     * Sends a plain text message to a chat.
     *
     * @param chatId   target chat identifier
     * @param response message text
     */
    public void sendMessage(
        final Long chatId,
        final String response
    ) {
        if (Strings.isBlank(response)) {
            logger.warn("Refused empty message to {}", chatId);
            return;
        }
        logger.info("Send message to {}: {}", chatId, response);
        final SendMessage sendMessage = new SendMessage(Long.toString(chatId), response);
        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            if (e.getMessage().contains("message is too long")) {
                sendMessage(chatId, "Response was too long and got rejected by telegram. I will send it as a file");
                try {
                    Path telegram = Files.createTempFile("telegram", "");
                    Files.writeString(telegram, response);
                    sendFileWithCaption(chatId, telegram.toAbsolutePath().toString(), "");
                } catch (IOException | TelegramApiException ex) {
                    sendMessage(chatId, "Error creating file.");
                    return;
                }
            }
            e.printStackTrace();
        }
    }

    /**
     * Sets the typing status for the chat.
     *
     * @param chatId chat identifier
     * @throws TelegramApiException if the request fails
     */
    public void setTyping(
        final Long chatId
    ) throws TelegramApiException {
        logger.info("Set typing to {}", chatId);
        final String chatIdAsString = Long.toString(chatId);
        final SendChatAction typingAction = SendChatAction.builder()
                .chatId(chatIdAsString)
                .action(ActionType.TYPING.toString())
                .build();

        telegramClient.execute(typingAction);
    }

    /**
     * Sends a markdown formatted message to a chat.
     *
     * @param chatId  target chat identifier
     * @param message markdown message text
     * @throws TelegramApiException if sending fails
     */
    public void sendMarkdownMessage(
        final Long chatId,
        final String message
    ) throws TelegramApiException {
        if (Strings.isBlank(message)) {
            logger.warn("Refused empty message to {}", chatId);
            return;
        }
        logger.info("Send markdown message to {}: {}", chatId, message);
        final String chatIdAsString = Long.toString(chatId);
        final SendMessage sendMessage = SendMessage.builder()
            .parseMode(ParseMode.MARKDOWN)
            .chatId(chatIdAsString)
            .text(message)
            .build();
        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            // maybe it is a bad formatted markdown
            logger.warn("Could not send markdown message '{}'", message, e);
            sendMessage(chatId, message);
        }
    }

    /**
     * Sends a file with an optional caption to a chat.
     *
     * @param chatId   chat identifier
     * @param filePath path to the file on disk
     * @param caption  caption to display with the file
     * @throws TelegramApiException if the request fails
     */
    public void sendFileWithCaption(
        final Long chatId,
        final String filePath,
        final String caption
    ) throws TelegramApiException {
        logger.info("Sending file to {}: {}", chatId, filePath);
        final SendDocument sendDocument = SendDocument.builder()
            .chatId(chatId.toString())
            .document(new InputFile(new File(filePath)))
            .caption(caption)
            .build();

        telegramClient.execute(sendDocument);
    }

    /**
     * Processes an anonymous prompt not linked to a chat id.
     * Chat id 0 is used.
     *
     * @param message prompt text
     * @return AI response to the prompt
     */
    public String consumeAnonymousMessage(
            final String message
    ) {
        logger.info("Consume anonymous message: {}", message);
        final TelegramTools telegramTools = getTelegramTools(0L);
        return aiBotService.prompt(0L, message, telegramTools);
    }

    /**
     * Sends a system message to a chat and returns the AI response.
     *
     * @param chatId  chat identifier
     * @param message system message text
     * @return AI response generated for the system message
     * @throws TelegramApiException if sending fails
     */
    public String consumeSystemMessage(
        final Long chatId,
        final String message
    ) throws TelegramApiException {
        logger.info("Consume system message: {}", message);

        final String text = "SystemAction: " + message;

        final TelegramTools telegramTools = getTelegramTools(chatId);
        final String response = aiBotService.prompt(chatId, text, telegramTools);

        logger.info("Response to " + chatId + ": " + text);
        sendMarkdownMessage(chatId, response);
        return response;
    }

    /**
     * Executes a text command by sending it as a markdown message.
     *
     * @param chatId  chat identifier
     * @param command command to execute
     */
    public void execute(
        final Long chatId,
        final String command
    ) {
        try {
            sendMarkdownMessage(chatId, command);
        } catch (TelegramApiException e) {
            logger.info("Could not send message '{}' to '{}'", command, chatId, e);
        }
    }

    private void consumeLogin(
        final Long chatId,
        final String loginCommand
    ) {
        logger.info("Consume login from {}: {}", chatId, loginCommand);

        final String[] loginWithArgs = loginCommand.split("\\s+", 2);
        if (!loginWithArgs[0].equals("/login")) {
            sendMessage(chatId, "Use /login PASSWORD to log in. Your chat id is " + chatId + "." +
                    "You are talking to TelegramAIConnector. " +
                    "Check instructions at https://github.com/beothorn/telegramAIConnector");
            return;
        }
        if (loginWithArgs.length != 2){
            sendMessage(chatId, "Invalid log in.");
            return;
        }
        String passwordLogin = loginWithArgs[1];
        final boolean loggedSuccessfully = authentication.login(chatId, passwordLogin);
        if (loggedSuccessfully) {
            logger.info("Logged in");
            sendMessage(chatId, "You are logged in.");
        } else {
            logger.info("Bad login attempt {}", chatId);
            sendMessage(chatId, "You are NOT logged in.");
        }
    }

    private void consumeMessage(
        final Long chatId,
        final Update update
    ) throws TelegramApiException {

        logger.info("Consume message from {}", chatId);

        if (update.getMessage().hasText()) {
            final String text = update.getMessage().getText();

            if (text.startsWith("/")) {
                final String[] commandWithArgs = text.split("\\s+", 2);
                String command = commandWithArgs[0].substring(1);
                if (commandWithArgs.length == 1) {
                    consumeCommand(chatId, command, "");
                } else if (commandWithArgs.length == 2) {
                    consumeCommand(chatId, command, commandWithArgs[1]);
                } else {
                    throw new IllegalArgumentException("Bad command " + text);
                }
            } else {
                String username = update.getMessage().getFrom().getUserName();
                consumeText(chatId, username + ": " + text);
            }
        }

        if (update.getMessage().getDocument() != null) {
            // TODO: analyze document?

            final Document document = update.getMessage().getDocument();
            final String fileId = document.getFileId();
            final String fileName = document.getFileName();

            downloadAndConsumeFile(chatId, fileId, fileName);
        }

        if (update.getMessage().hasPhoto()) {
            try {
                final List<PhotoSize> photos = update.getMessage().getPhoto();
                final PhotoSize largestPhoto = photos.getLast();
                final String fileId = largestPhoto.getFileId();
                String stored = downloadAndConsumeFile(chatId, fileId);

                String caption = update.getMessage().getCaption();
                if (stored != null && caption != null && !caption.isBlank()) {
                    final String fileName = Paths.get(stored).getFileName().toString();
                    runAsync(
                        chatId,
                        "photo" + InstantUtils.currentTimeSeconds(),
                        () -> aiAnalysisTool.analyzeImageForChatId(fileName, caption, chatId)
                    );
                }
            } catch (Exception e) {
                logger.error("Failed to process photo", e);
                sendMessage(chatId, "Failed to process photo: " + e.getMessage());
            }
        }

        if (update.getMessage().hasVideo()) {
            try {
                final Video video = update.getMessage().getVideo();
                final String fileId = video.getFileId();

                downloadAndConsumeFile(chatId, fileId);
            } catch (Exception e) {
                logger.error("Failed to process video", e);
                sendMessage(chatId, "Failed to process video: " + e.getMessage());
            }
            return;
        }

        if (update.getMessage().hasLocation()) {
            final double lat = update.getMessage().getLocation().getLatitude();
            final double lon = update.getMessage().getLocation().getLongitude();
            consumeLocation(chatId, lat, lon);
        }

        if (update.getMessage().hasAudio()) {
            // TODO: Use audio model

            final Audio audio = update.getMessage().getAudio();
            downloadAndConsumeFile(chatId, audio.getFileId(), audio.getFileName());
        }

        if (update.getMessage().hasVoice()) {
            // TODO: Use audio model to transcribe

            final Voice voice = update.getMessage().getVoice();
            downloadAndConsumeFile(chatId, voice.getFileId());
        }

        if (update.getMessage().hasSticker()) {
            final Sticker sticker = update.getMessage().getSticker();

            final String emoji = "TelegramAction: User sent a sticker '" + sticker.getEmoji() + "'";
            consumeText(chatId, emoji);
        }

        if (update.getMessage().hasContact()) {
            // TODO: What should be done?
            final Contact contact = update.getMessage().getContact();
            final String name = contact.getFirstName() + " " + (contact.getLastName() != null ? contact.getLastName() : "");
            consumeText(chatId, "TelegramAction: User sent a contact\n" + name + "\nPhone number:(" + contact.getPhoneNumber() + ")");
        }

        if (update.getMessage().getPoll() != null) {
            // TODO: What should be done?
            final Poll poll = update.getMessage().getPoll();
            final String question = poll.getQuestion();
            final String explanation = poll.getExplanation();
            final String options = poll.getOptions().stream().map(PollOption::getText).collect(Collectors.joining("\n"));
            consumeText(chatId, "Answer this poll: " + question + "\n" + explanation + "\n" + options);
        }
    }

    private String downloadAndConsumeFile(
        final Long chatId,
        final String fileId
    ) throws TelegramApiException {
        return downloadAndConsumeFile(chatId, fileId, null);
    }

    private String downloadAndConsumeFile(Long chatId, String fileId, String fileNameMaybe) throws TelegramApiException {
        final GetFile getFileMethod = new GetFile(fileId);
        final org.telegram.telegrambots.meta.api.objects.File telegramFile = telegramClient.execute(getFileMethod);

        final String originalFilePath = telegramFile.getFilePath();
        final String originalFileName = Paths.get(originalFilePath).getFileName().toString();

        final String fileName = (fileNameMaybe != null) ? fileNameMaybe : System.currentTimeMillis() + "_" + originalFileName;

        final File downloadedFile = telegramClient.downloadFile(telegramFile);
        logger.info("Downloaded file {} to {}", fileName, downloadedFile.getAbsolutePath());
        final Path outputDir = Paths.get(uploadFolder + "/" + chatId);
        try {
            Files.createDirectories(outputDir);
            final Path destinationPath = outputDir.resolve(fileName);
            Files.copy(downloadedFile.toPath(), destinationPath, StandardCopyOption.REPLACE_EXISTING);
            sendMessage(chatId, "File saved: " + fileName);
            consumeFile(chatId, destinationPath);
            return destinationPath.toString();
        } catch (IOException e) {
            logger.error("Error saving file", e);
            sendMessage(chatId, "Error saving file: " + e.getMessage());
            return null;
        }
    }

    private void consumeCommand(
        final Long chatId,
        final String command,
        final String args
    ) {
        logger.info("Consume command from {}: {} {}", chatId, command, args);

        String availableCommands = """
                    /help
                    /chatId
                    /version
                    /datetime
                    /list
                    /delete file
                    /read file
                    /rename old new
                    /download file
                    /analyzeImage fileName [prompt]
                    /listTasks
                    /listTools
                    /profile
                    /newProfile profile text
                    /logout
                    /changePassword newPass
                    /doing""";
        if (falClient != null) {
            availableCommands += """
                    
                    /generateImage fileName [prompt]
                    """;
        }

        if (command.equalsIgnoreCase("help")) {
            sendMessage(chatId, availableCommands);
            return;
        }
        if (command.equalsIgnoreCase("chatId")) {
            sendMessage(chatId, "Your chat id is " + chatId);
            return;
        }
        if (command.equalsIgnoreCase("version")) {
            sendMessage(chatId, commands.getVersion());
            return;
        }
        if (command.equalsIgnoreCase("datetime")) {
            sendMessage(chatId, commands.getCurrentDateTime());
            return;
        }
        if (command.equalsIgnoreCase("list")) {
            sendMessage(chatId, commands.listUploadedFiles(chatId));
            return;
        }
        if (command.equalsIgnoreCase("delete")) {
            sendMessage(chatId, commands.delete(chatId, args));
            return;
        }
        if (command.equalsIgnoreCase("read")) {
            sendMessage(chatId, commands.read(chatId, args));
            return;
        }
        if (command.equalsIgnoreCase("rename")) {
            String[] tokens = args.split("\\s+", 2);
            String firstArg = tokens[0];
            String secondArg = tokens.length > 1 ? tokens[1] : firstArg;
            sendMessage(chatId, commands.rename(chatId, firstArg, secondArg));
            return;
        }
        if (command.equalsIgnoreCase("download")) {
            sendMessage(chatId, commands.download(this, chatId, args));
            return;
        }
        if (command.equalsIgnoreCase("analyzeImage")) {
            if (Strings.isBlank(args)) {
                sendMessage(chatId, "Usage: /analyzeImage fileName [prompt]");
            } else {
                String[] tokens = args.split("\\s+", 2);
                String firstArg = tokens[0];
                String secondArg = tokens.length > 1 ? tokens[1] : "Describe the image.";
                runAsync(
                    chatId,
                    "analyzeImage" + InstantUtils.currentTimeSeconds(),
                    () -> aiAnalysisTool.analyzeImageForChatId(firstArg, secondArg, chatId)
                );
            }
            return;
        }
        if (falClient != null) {
            final TelegramTools telegramTools = new TelegramTools(
                    this,
                    taskScheduler,
                    chatId,
                    uploadFolder
            );
            FalAiTools falAiTools = new FalAiTools(falClient, uploadFolder + "/" + chatId, telegramTools);
            if (command.equalsIgnoreCase("generateImage")) {
                if (Strings.isBlank(args)) {
                    sendMessage(chatId, "Usage: /generateImage fileName [prompt]");
                } else {
                    String[] tokens = args.split("\\s+", 2);
                    String firstArg = tokens[0];
                    String secondArg = tokens.length > 1 ? tokens[1] : "";
                    runAsync(
                            chatId,
                            "generateImage" + InstantUtils.currentTimeSeconds(),
                            () -> falAiTools.generateImage(secondArg, firstArg)
                    );
                }
                return;
            }
        }
        if (command.equalsIgnoreCase("listTasks")) {
            sendMessage(chatId, commands.listTasks(chatId));
            return;
        }
        if (command.equalsIgnoreCase("listTools")) {
            sendMessage(chatId, commands.listTools());
            return;
        }
        if (command.equalsIgnoreCase("profile")) {
            sendMessage(chatId, commands.getProfile(chatId));
            return;
        }
        if (command.equalsIgnoreCase("newProfile")) {
            if (Strings.isNotBlank(args)) {
                sendMessage(chatId, commands.setProfile(chatId, args));
            } else {
                sendMessage(chatId, "Profile can't be empty.");
            }
            return;
        }
        if (command.equalsIgnoreCase("doing")) {
            sendMessage(chatId, processingStatus.status(chatId));
            return;
        }
        if (command.equalsIgnoreCase("logout")) {
            authentication.logout(chatId);
            sendMessage(chatId, "You were logged out.");
            return;
        }
        if (command.equalsIgnoreCase("changePassword")) {
            if(Strings.isNotBlank(args)) {
                authentication.setPasswordForUser(chatId ,args);
                sendMessage(chatId, "You password was changed.");
                if(args.length() < 10) {
                    sendMessage(
                        chatId,
                        "It is recommended that you chose a password with minimum 10 characters."
                    );
                }
            } else {
                sendMessage(chatId, "Password must not be empty.");
            }
            return;
        }
        sendMessage(chatId, "Unknown command '"+ command +"'. Available commands: \n" + availableCommands);
    }

    private void consumeText(
        final Long chatId,
        final String text
    ) {
        logger.info("Consume text from {}: {}", chatId, text);
        runAsync(
            chatId,
            "message" + InstantUtils.currentTimeSeconds(),
            () -> {
                final TelegramTools telegramTools = getTelegramTools(chatId);
                return aiBotService.prompt(chatId, text, telegramTools);
            }
        );
    }

    private void consumeFile(
        final Long chatId,
        final Path uploadedFile
    ) {
        final String uploadedFileString = uploadedFile.toString();
        logger.info("Consume file from {}: {}", chatId, uploadedFileString);
        final String text = "SystemAction: User upload file to '" + uploadedFileString + "'.";
        runAsync(
            chatId,
            "file" + InstantUtils.currentTimeSeconds(),
            () -> {
                final TelegramTools telegramTools = getTelegramTools(chatId);
                return aiBotService.prompt(chatId, text, telegramTools);
            }
        );
    }

    private void consumeLocation(
        final Long chatId,
        final double latitude,
        final double longitude
    ) {
        logger.info("Consume location from {}: {}, {}", chatId, latitude, longitude);
        final String locationMessage = String.format("TelegramAction: User shared a location %f %f", latitude, longitude);
        runAsync(
            chatId,
            "location" + InstantUtils.currentTimeSeconds(),
            () -> {
                final TelegramTools telegramTools = getTelegramTools(chatId);
                return aiBotService.prompt(chatId, locationMessage, telegramTools);
            }
        );
    }

    private void sendTypingCommand(
        final Long chatId
    ) {
        try {
            setTyping(chatId);
        } catch (TelegramApiException e) {
            // Not important if it fails
            logger.error("Could not set status to typing");
        }
    }

    private void startTypingThread(
        final Long chatId,
        final CompletableFuture<?> processingFuture
    ) {
        final ScheduledFuture<?> typingFuture = typingScheduler.scheduleAtFixedRate(
            () -> sendTypingCommand(chatId),
            0,
            5,
            TimeUnit.SECONDS
        );
        processingFuture.whenComplete((r, t) -> typingFuture.cancel(false));
    }

    private void runAsync(
        final Long chatId,
        final String description,
        final Supplier<String> work
    ) {
        final CompletableFuture<String> future = CompletableFuture.supplyAsync(work, executor);
        processingStatus.register(chatId, future, description);
        future.whenComplete((response, throwable) -> {
            try {
                if (throwable == null) {
                    try {
                        sendMarkdownMessage(chatId, response);
                    } catch (final TelegramApiException e) {
                        logger.error("Could not send result", e);
                        sendMessage(chatId, "Failed sending result '" + e.getMessage() + "'");
                    }
                } else {
                    logger.error("Failed async work", throwable);
                    sendMessage(chatId, "Something went wrong '" + throwable.getMessage() + "'");
                }
            } finally {
                processingStatus.unregister(chatId, future);
            }
        });
        startTypingThread(chatId, future);
    }

    @NotNull
    private TelegramTools getTelegramTools(
            final Long chatId
    ) {
        return new TelegramTools(
            this,
                taskScheduler,
            chatId,
            uploadFolder
        );
    }

    /**
     * Returns the configured bot name.
     *
     * @return bot name as defined by Telegram
     */
    public String getBotName() {
        return botName;
    }

    /**
     * Stops internal executors and releases resources.
     */
    @PreDestroy
    public void shutdown() {
        executor.shutdown();
        typingScheduler.shutdown();
    }
}
