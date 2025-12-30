package com.biblio.services;

import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    private FileStorageService fileStorageService;
    private Path uploadDir;

    @BeforeEach
    void setUp() throws IOException {
        fileStorageService = new FileStorageService();
        uploadDir = Paths.get("uploads");

        // Nettoyer le dossier uploads avant chaque test
        if (Files.exists(uploadDir)) {
            Files.walk(uploadDir)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        // Nettoyage après test
        if (Files.exists(uploadDir)) {
            Files.walk(uploadDir)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    // ===============================
    // TEST : stockage réussi
    // ===============================
    @Test
    void store_OK() {
        MultipartFile file = new MockMultipartFile(
                "file",
                "image.png",
                "image/png",
                "test content".getBytes()
        );

        String storedFilename = fileStorageService.store(file);

        assertNotNull(storedFilename);
        assertTrue(storedFilename.endsWith(".png"));

        Path storedFile = uploadDir.resolve(storedFilename);
        assertTrue(Files.exists(storedFile));
    }

    // ===============================
    // TEST : fichier vide
    // ===============================
    @Test
    void store_EmptyFile_ShouldThrowException() {
        MultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> fileStorageService.store(emptyFile)
        );

        assertTrue(exception.getMessage().contains("empty file"));
    }

    // ===============================
    // TEST : tentative path traversal
    // ===============================
    @Test
    void store_InvalidPath_ShouldThrowException() {
        MultipartFile maliciousFile = new MockMultipartFile(
                "file",
                "../evil.txt",
                "text/plain",
                "hack".getBytes()
        );

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> fileStorageService.store(maliciousFile)
        );

        assertTrue(exception.getMessage().contains("Cannot store file outside"));
    }
}
