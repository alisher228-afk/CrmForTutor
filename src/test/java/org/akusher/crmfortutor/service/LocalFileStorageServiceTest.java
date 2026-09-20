package org.akusher.crmfortutor.service;

import org.akusher.crmfortutor.exception.BadRequestException;
import org.akusher.crmfortutor.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalFileStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new LocalFileStorageService(tempDir.toString(), "10MB");
    }

    @Test
    @DisplayName("store - success for allowed extensions")
    void store_Success() throws IOException {
        String[] allowedFiles = {
                "document.pdf", "photo.jpg", "image.JPEG", "scan.png",
                "report.doc", "essay.docx", "archive.zip"
        };

        for (String filename : allowedFiles) {
            MockMultipartFile file = new MockMultipartFile(
                    "file", filename, "application/octet-stream", ("Content for " + filename).getBytes()
            );

            String storedName = storageService.store(file);

            assertThat(storedName).isNotBlank();
            String ext = filename.substring(filename.lastIndexOf('.')).toLowerCase();
            assertThat(storedName).endsWith(ext);

            Path storedPath = tempDir.resolve(storedName);
            assertThat(Files.exists(storedPath)).isTrue();
            assertThat(Files.readAllBytes(storedPath)).isEqualTo(("Content for " + filename).getBytes());
        }
    }

    @Test
    @DisplayName("store - disallowed extension throws BadRequestException")
    void store_DisallowedExtension() {
        String[] disallowedFiles = {"virus.exe", "script.sh", "index.html", "data.json", "styles.css"};

        for (String filename : disallowedFiles) {
            MockMultipartFile file = new MockMultipartFile(
                    "file", filename, "application/octet-stream", "content".getBytes()
            );

            assertThatThrownBy(() -> storageService.store(file))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("is not allowed");
        }
    }

    @Test
    @DisplayName("store - missing or empty extension throws BadRequestException")
    void store_NoExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "noextensionfile", "application/octet-stream", "content".getBytes()
        );

        assertThatThrownBy(() -> storageService.store(file))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("is not allowed");
    }

    @Test
    @DisplayName("store - null or empty file throws BadRequestException")
    void store_NullOrEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", new byte[0]
        );

        assertThatThrownBy(() -> storageService.store(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("File is empty or not provided");

        assertThatThrownBy(() -> storageService.store(emptyFile))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("File is empty or not provided");
    }

    @Test
    @DisplayName("store - file size exceeding max limit throws BadRequestException")
    void store_ExceedsMaxSize() {
        LocalFileStorageService strictStorage = new LocalFileStorageService(tempDir.toString(), "100B");

        byte[] largeContent = new byte[200];
        MockMultipartFile largeFile = new MockMultipartFile(
                "file", "large.pdf", "application/pdf", largeContent
        );

        assertThatThrownBy(() -> strictStorage.store(largeFile))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("exceeds maximum allowed limit");
    }

    @Test
    @DisplayName("store - path traversal in original file name throws BadRequestException")
    void store_PathTraversal() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "../evil.pdf", "application/pdf", "malicious content".getBytes()
        );

        assertThatThrownBy(() -> storageService.store(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid path sequence");
    }

    @Test
    @DisplayName("load - success returns readable Resource")
    void load_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.pdf", "application/pdf", "Algebra lecture notes".getBytes()
        );

        String storedName = storageService.store(file);
        Resource resource = storageService.load(storedName);

        assertThat(resource).isNotNull();
        assertThat(resource.exists()).isTrue();
        assertThat(resource.isReadable()).isTrue();
        assertThat(resource.getContentAsByteArray()).isEqualTo("Algebra lecture notes".getBytes());
    }

    @Test
    @DisplayName("load - nonexistent file throws ResourceNotFoundException")
    void load_NotFound() {
        assertThatThrownBy(() -> storageService.load("missing-uuid.pdf"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("File not found");
    }

    @Test
    @DisplayName("load - path traversal in fileName throws BadRequestException")
    void load_PathTraversal() {
        assertThatThrownBy(() -> storageService.load("../secret.txt"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot access file outside");
    }

    @Test
    @DisplayName("load - blank fileName throws BadRequestException")
    void load_BlankFileName() {
        assertThatThrownBy(() -> storageService.load("   "))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("File name cannot be empty");
    }

    @Test
    @DisplayName("delete - successfully deletes existing file")
    void delete_Success() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "to-delete.png", "image/png", "image bytes".getBytes()
        );

        String storedName = storageService.store(file);
        assertThat(Files.exists(tempDir.resolve(storedName))).isTrue();

        storageService.delete(storedName);
        assertThat(Files.exists(tempDir.resolve(storedName))).isFalse();
    }

    @Test
    @DisplayName("delete - non-existent file does not throw exception")
    void delete_NonExistent() {
        storageService.delete("not-exists.pdf");
    }

    @Test
    @DisplayName("delete - path traversal throws BadRequestException")
    void delete_PathTraversal() {
        assertThatThrownBy(() -> storageService.delete("../protected.txt"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot delete file outside");
    }
}
