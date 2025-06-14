package com.github.beothorn.telegramAIConnector.backoffice;

import com.github.beothorn.telegramAIConnector.utils.TelegramAIFileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
public class FileService {
    private final String uploadFolder;

    public FileService(@Value("${telegramIAConnector.uploadFolder}") String uploadFolder) {
        this.uploadFolder = uploadFolder;
    }

    private File baseDir(Long chatId) {
        return new File(uploadFolder + "/" + chatId);
    }

    public List<String> list(Long chatId) {
        File dir = baseDir(chatId);
        if (!dir.exists() || !dir.isDirectory()) {
            return List.of();
        }
        String[] files = dir.list();
        return files == null ? List.of() : Arrays.asList(files);
    }

    public Resource download(Long chatId, String name) {
        File dir = baseDir(chatId);
        File file = new File(dir, name);
        if (TelegramAIFileUtils.isNotInParentFolder(dir, file) || !file.exists()) {
            return null;
        }
        return new FileSystemResource(file);
    }

    public void delete(Long chatId, String name) {
        File dir = baseDir(chatId);
        File file = new File(dir, name);
        if (TelegramAIFileUtils.isNotInParentFolder(dir, file)) return;
        if (file.exists()) file.delete();
    }

    public void rename(Long chatId, String oldName, String newName) throws IOException {
        File dir = baseDir(chatId);
        File from = new File(dir, oldName);
        File to = new File(dir, newName);
        if (TelegramAIFileUtils.isNotInParentFolder(dir, from) || TelegramAIFileUtils.isNotInParentFolder(dir, to)) {
            throw new IOException("invalid name");
        }
        if (from.exists()) {
            if (!from.renameTo(to)) {
                throw new IOException("rename failed");
            }
        }
    }

    public void upload(Long chatId, MultipartFile file) throws IOException {
        File dir = baseDir(chatId);
        dir.mkdirs();
        File dest = new File(dir, file.getOriginalFilename());
        if (TelegramAIFileUtils.isNotInParentFolder(dir, dest)) {
            throw new IOException("invalid name");
        }
        file.transferTo(dest);
    }
}
