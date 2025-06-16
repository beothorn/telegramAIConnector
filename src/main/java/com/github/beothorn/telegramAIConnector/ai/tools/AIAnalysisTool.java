package com.github.beothorn.telegramAIConnector.ai.tools;

import com.github.beothorn.telegramAIConnector.utils.TelegramAIFileUtils;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import java.io.File;
import java.nio.file.Files;

@Component
public class AIAnalysisTool {

    private final ChatModel chatModel;
    private final String uploadFolder;

    public AIAnalysisTool(ChatModel chatModel,
                          @Value("${telegramIAConnector.uploadFolder}") String uploadFolder) {
        this.chatModel = chatModel;
        this.uploadFolder = uploadFolder;
    }

    @Tool(description = "Analyze or transcribe an image.")
    public String analyzeImage(
            @ToolParam(description = "Name of the file inside Telegram upload folder") String fileName,
            @ToolParam(description = "Prompt describing what to do with the image") String prompt) {
        try {
            File parent = new File(uploadFolder);
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
