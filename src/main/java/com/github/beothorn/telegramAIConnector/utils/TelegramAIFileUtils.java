package com.github.beothorn.telegramAIConnector.utils;

import java.io.File;

public class TelegramAIFileUtils {

    /**
     * Ensures that the given file resides within the parent folder.
     *
     * @return {@code true} if the file is outside the parent folder
     */
    public static boolean isNotInParentFolder(File parentFolder, File fileToCreate) {
        try {
            String parentPath = parentFolder.getCanonicalPath();
            String filePath = fileToCreate.getCanonicalPath();
            return !filePath.startsWith(parentPath + File.separator);
        } catch (Exception e) {
            return true;
        }
    }
}
