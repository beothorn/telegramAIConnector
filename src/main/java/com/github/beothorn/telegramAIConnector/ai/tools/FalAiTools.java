package com.github.beothorn.telegramAIConnector.ai.tools;

import ai.fal.client.FalClient;
import ai.fal.client.Output;
import ai.fal.client.SubscribeOptions;
import ai.fal.client.queue.QueueStatus;
import com.github.beothorn.telegramAIConnector.utils.TelegramAIFileUtils;
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
import java.util.concurrent.TimeUnit;

public class FalAiTools {

    private final FalClient falClient;
    private final String uploadFolder;

    public FalAiTools(FalClient falClient, String uploadFolder) {
        this.falClient = falClient;
        this.uploadFolder = uploadFolder;
    }

    private static String toDataUri(Path file) throws Exception {
        String mime = Files.probeContentType(file);
        if (mime == null) {
            mime = "application/octet-stream";
        }
        String base64 = Base64.getEncoder().encodeToString(Files.readAllBytes(file));
        return "data:" + mime + ";base64," + base64;
    }

    @Tool(description = "AI to edit images with an image and a text describing the transformation as input and a transformed image as output.")
    public String editImage(
        @ToolParam(description = "Name of the source image located in the Telegram upload folder") String fileName,
        @ToolParam(description = "Instruction describing the desired modification") String prompt,
        @ToolParam(description = "Name of the output image to be created") String outputFileName
    ) {
        try {
            File parent = new File(uploadFolder);
            File source = new File(parent, fileName);
            File dest = new File(parent, outputFileName);

            if (TelegramAIFileUtils.isNotInParentFolder(parent, source) || TelegramAIFileUtils.isNotInParentFolder(parent, dest)) {
                return "Invalid file name.";
            }
            if (!source.isFile()) {
                return "File '" + fileName + "' not found.";
            }

            String dataUri = toDataUri(source.toPath());
            Map<String, Object> input = Map.of(
                    "prompt", prompt,
                    "safety_tolerance", "5", // This is important, low values give too much false positives
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

    @Tool(description = "AI to generate images from a text prompt.")
    public String generateImage(
        @ToolParam(description = "Instruction describing the desired image") String prompt,
        @ToolParam(description = "Name of the output image to be created") String outputFileName
    ) {
        try {
            File parent = new File(uploadFolder);
            File dest = new File(parent, outputFileName);

            if (TelegramAIFileUtils.isNotInParentFolder(parent, dest)) {
                return "Invalid file name.";
            }

            Map<String, Object> input = Map.of(
                    "prompt", prompt,
                    "safety_tolerance", "5" // Allow most permissive generation
            );
            Output<JsonObject> result = falClient.subscribe(
                "fal-ai/fast-sdxl",
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
            return "I created a new file " + outputFileName + " on your upload folder.";
        } catch (Exception e) {
            return "Failed to generate image: " + e.getMessage();
        }
    }

    @Tool(description = "Describe the content of an image and transcribe any text found on it.")
    public String describeImage(
        @ToolParam(description = "Name of the image located in the Telegram upload folder") String fileName
    ) {
        try {
            File parent = new File(uploadFolder);
            File source = new File(parent, fileName);

            if (TelegramAIFileUtils.isNotInParentFolder(parent, source)) {
                return "Invalid file name.";
            }
            if (!source.isFile()) {
                return "File '" + fileName + "' not found.";
            }

            String dataUri = toDataUri(source.toPath());
            Map<String, Object> input = Map.of(
                    "image_url", dataUri,
                    "prompt", "Describe the image and transcribe any text you find"
            );
            Output<JsonObject> result = falClient.subscribe(
                "fal-ai/llava-1.5-7b",
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
            String description = result.getData().get("text").getAsString();

            String ocr = ocrWithTesseract(source.toPath());
            if (!ocr.isBlank()) {
                description += "\nText: " + ocr;
            }
            return description;
        } catch (Exception e) {
            return "Failed to analyze image: " + e.getMessage();
        }
    }

    private static Path toMp3(Path source) throws Exception {
        Path mp3 = Files.createTempFile("telegram", ".mp3");
        Process process = new ProcessBuilder(
                "ffmpeg", "-y", "-i", source.toString(), mp3.toString()
        ).redirectErrorStream(true).start();
        if (!process.waitFor(30, TimeUnit.SECONDS) || process.exitValue() != 0) {
            String logs = new String(process.getInputStream().readAllBytes());
            throw new RuntimeException("ffmpeg failed: " + logs);
        }
        return mp3;
    }

    private static String ocrWithTesseract(Path image) throws Exception {
        Process process = new ProcessBuilder(
                "tesseract", image.toString(), "-", "-l", "eng"
        ).redirectErrorStream(true).start();
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroy();
            throw new RuntimeException("tesseract timed out");
        }
        return new String(process.getInputStream().readAllBytes()).trim();
    }

    @Tool(description = "Transcribes an audio file.")
    public String audioToText(
        @ToolParam(description = "Name of the audio file located in the Telegram upload folder") String fileName
    ) {
        try {
            File parent = new File(uploadFolder);
            File source = new File(parent, fileName);

            if (TelegramAIFileUtils.isNotInParentFolder(parent, source)) {
                return "Invalid file name.";
            }
            if (!source.isFile()) {
                return "File '" + fileName + "' not found.";
            }

            File audioFile = source;
            String lower = fileName.toLowerCase();
            if (lower.endsWith(".oga") || lower.endsWith(".ogg")) {
                Path temp = toMp3(source.toPath());
                audioFile = temp.toFile();
            }

            String dataUri = toDataUri(audioFile.toPath());

            Map<String, Object> input = Map.of(
                    "audio_url", dataUri
            );
            Output<JsonObject> result = falClient.subscribe(
                "fal-ai/whisper",
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
            return result.getData().get("text").getAsString();
        } catch (Exception e) {
            return "Failed to transcribe audio: " + e.getMessage();
        }
    }
}
