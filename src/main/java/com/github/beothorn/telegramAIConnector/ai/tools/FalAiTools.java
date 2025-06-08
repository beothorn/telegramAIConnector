package com.github.beothorn.telegramAIConnector.ai.tools;

import ai.fal.client.FalClient;
import ai.fal.client.Output;
import ai.fal.client.SubscribeOptions;
import ai.fal.client.queue.QueueStatus;
import com.google.gson.JsonObject;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;

public class FalAiTools {

    private final FalClient falClient;
    private final String uploadFolder;

    public FalAiTools(FalClient falClient, String uploadFolder) {
        this.falClient = falClient;
        this.uploadFolder = uploadFolder;
    }

    private static boolean isInvalid(File parentFolder, File fileToCreate) {
        try {
            String parentPath = parentFolder.getCanonicalPath();
            String filePath = fileToCreate.getCanonicalPath();
            return !filePath.startsWith(parentPath + File.separator);
        } catch (Exception e) {
            return true;
        }
    }

    private static String toDataUri(Path file) throws Exception {
        String mime = Files.probeContentType(file);
        if (mime == null) {
            mime = "image/png";
        }
        String base64 = Base64.getEncoder().encodeToString(Files.readAllBytes(file));
        return "data:" + mime + ";base64," + base64;
    }

    @Tool(description = "AI to edit images with an image and a text describing the transformation as input and a transformed image as output.")
    public String kontext(
            @ToolParam(description = "Name of the source image located in the Telegram upload folder") String fileName,
            @ToolParam(description = "Instruction describing the desired modification") String prompt,
            @ToolParam(description = "Name of the output image to be created") String outputFileName
    ) {
        try {
            File parent = new File(uploadFolder);
            File source = new File(parent, fileName);
            File dest = new File(parent, outputFileName);

            if (isInvalid(parent, source) || isInvalid(parent, dest)) {
                return "Invalid file name.";
            }
            if (!source.isFile()) {
                return "File '" + fileName + "' not found.";
            }

            String dataUri = toDataUri(source.toPath());
            Map<String, Object> input = Map.of(
                    "prompt", prompt,
                    "image_url", dataUri
            );
            Output<JsonObject> result = falClient.subscribe(
                    "fal-ai/flux-pro/kontext",
                    SubscribeOptions.<JsonObject>builder()
                            .input(input)
                            .logs(false)
                            .resultType(JsonObject.class)
                            .onQueueUpdate(u -> {
                                if (u instanceof QueueStatus.InProgress progress) {
                                    // ignore logs
                                }
                            })
                            .build()
            );
            String url = result.getData().getAsJsonArray("images").get(0).getAsJsonObject().get("url").getAsString();
            try (InputStream in = new URL(url).openStream()) {
                Files.createDirectories(parent.toPath());
                Files.copy(in, dest.toPath());
            }
            return "I created a new file " + outputFileName + " on your upload folder with the change you asked.";
        } catch (Exception e) {
            return "Failed to process image: " + e.getMessage();
        }
    }
}
