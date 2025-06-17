package com.github.beothorn.telegramAIConnector.backoffice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FileServiceTest {
    @TempDir
    Path tempDir;

    /**
     * Makes sure we can rename and delete a file.
     * This uses the temp folder.
     * @throws Exception
     */
    @Test
    void uploadRenameDeleteFile() throws Exception {
        FileService service = new FileService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("file","test.txt","text/plain","hi".getBytes());
        service.upload(1L, file);
        assertEquals(List.of("test.txt"), service.list(1L));

        Resource r = service.download(1L, "test.txt");
        assertNotNull(r);

        service.rename(1L, "test.txt", "new.txt");
        assertEquals(List.of("new.txt"), service.list(1L));

        service.delete(1L, "new.txt");
        assertTrue(service.list(1L).isEmpty());
    }
}
