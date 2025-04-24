package com.github.beothorn.telegramAIConnector;

import java.io.File;
import java.io.IOException;

public class FileUtils {

    public static boolean isValid(String parentPathStr, String filePathStr) {
        try {
            File parentFolder = new File(parentPathStr);
            File fileToCreate = new File(filePathStr);

            String parentPath = null;
            parentPath = parentFolder.getCanonicalPath();
            String filePath = fileToCreate.getCanonicalPath();

            if (filePath.startsWith(parentPath + File.separator)) {
                return fileToCreate.createNewFile();
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }
}