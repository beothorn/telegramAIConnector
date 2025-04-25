package com.github.beothorn.telegramAIConnector;

import java.io.File;
import java.io.IOException;

public class FileUtils {

    public static boolean isInvalid(
            final File parentFolder,
            final File fileToCreate
    ) {
        try {
            String parentPath = parentFolder.getCanonicalPath();
            String filePath = fileToCreate.getCanonicalPath();
            return !filePath.startsWith(parentPath + File.separator);
        } catch (IOException e) {
            return true;
        }
    }
}