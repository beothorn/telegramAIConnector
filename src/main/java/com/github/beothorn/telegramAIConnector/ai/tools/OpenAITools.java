package com.github.beothorn.telegramAIConnector.ai.tools;

import com.github.beothorn.telegramAIConnector.utils.TelegramAIFileUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.File;

public class OpenAITools {

    private final String uploadFolder;

    public OpenAITools(String uploadFolder) {
        this.uploadFolder = uploadFolder;
    }

    @Tool(description = "AI to create images from a prompt. Returns the generated file path.")
    public String editImage(
        @ToolParam(description = "The prompt for the image creation") String prompt,
        @ToolParam(description = "Simple file name to be used as output") String fileName
    ) {
        File parent = new File(uploadFolder);
        File out = new File(parent, fileName);

        if (TelegramAIFileUtils.isNotInParentFolder(parent, out)) {
            return "Invalid file name.";
        }
        int i = 0;
        while (out.exists()) {
            out = new File(parent, fileName + i++);
        }
        return "";
    }
}
