package com.github.beothorn.telegramAIConnector.ai.tools;

import com.github.beothorn.telegramAIConnector.utils.TelegramAIFileUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.MimeTypeUtils;

import java.io.File;
import java.nio.file.Files;

public class AIAnalysisTool {

    private final ChatModel chatModel;
    private final String uploadFolder;

    public AIAnalysisTool(
            final ChatModel chatModel,
            final String uploadFolder
    ) {
        this.chatModel = chatModel;
        this.uploadFolder = uploadFolder;
    }

    @Tool(description = "AI to analyze or transcribe an image, returns a string with the result.")
    public String analyzeImage(
        @ToolParam(description = "Name of the source image located in the Telegram upload folder") final String fileName,
        @ToolParam(description = "Prompt with the command to analyze or process the image") final String prompt
    ) {
        return analyzeImageOnFolder(fileName, prompt, uploadFolder);
    }

    @Nullable
    public String analyzeImageForChatId(
            final String fileName,
            final String prompt,
            final long chatId
    ) {
        return analyzeImageOnFolder(fileName, prompt, uploadFolder + "/" + chatId);
    }

    @Nullable
    private String analyzeImageOnFolder(
        final String fileName,
        final String prompt,
        final String currentUploadFolder
    ) {
        try {
            File parent = new File(currentUploadFolder);
            File source = new File(parent, fileName);
            if (TelegramAIFileUtils.isNotInParentFolder(parent, source)) {
                return "Invalid file name.";
            }
            if (!source.isFile()) {
                return "File '" + fileName + "' not found.";
            }

            var mime = MimeTypeUtils.parseMimeType(Files.probeContentType(source.toPath()));
            var media = new Media(mime, new FileSystemResource(source));
            var userMessage = UserMessage.builder()
                    .text(prompt)
                    .media(media)
                    .build();

            ChatResponse response = chatModel.call(new Prompt(userMessage));
            return response.getResult().getOutput().getText();
        } catch (Exception e) {
            return "Failed to analyze image: " + e.getMessage();
        }
    }
}
