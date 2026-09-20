package org.akusher.crmfortutor.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.akusher.crmfortutor.exception.BadRequestException;
import org.akusher.crmfortutor.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class LocalFileStorageService implements FileStorageService {

    public static final Set<String> DEFAULT_ALLOWED_EXTENSIONS = Set.of(
            "pdf", "jpg", "jpeg", "png", "doc", "docx", "zip"
    );

    private final Path uploadPath;
    private final long maxFileSizeBytes;
    private final String maxFileSizeRaw;
    private final Set<String> allowedExtensions;

    @Autowired
    public LocalFileStorageService(
            @Value("${storage.upload-dir:./uploads}") String uploadDir,
            @Value("${storage.max-file-size:10MB}") String maxFileSize) {
        this(uploadDir, maxFileSize, DEFAULT_ALLOWED_EXTENSIONS);
    }

    public LocalFileStorageService(String uploadDir, String maxFileSize, Set<String> allowedExtensions) {
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.maxFileSizeRaw = maxFileSize != null ? maxFileSize : "10MB";
        this.maxFileSizeBytes = parseDataSize(this.maxFileSizeRaw);
        this.allowedExtensions = allowedExtensions != null ? allowedExtensions : DEFAULT_ALLOWED_EXTENSIONS;
        init();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize upload directory: " + uploadPath, e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty or not provided");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new BadRequestException(String.format(
                    "File size (%d bytes) exceeds maximum allowed limit of %s (%d bytes)",
                    file.getSize(), maxFileSizeRaw, maxFileSizeBytes));
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BadRequestException("File name is invalid or missing");
        }

        String cleanedFilename = StringUtils.cleanPath(originalFilename);
        if (cleanedFilename.contains("..")) {
            throw new BadRequestException("File name contains invalid path sequence: " + originalFilename);
        }

        String extension = getFileExtension(cleanedFilename);
        if (extension.isEmpty() || !allowedExtensions.contains(extension.toLowerCase())) {
            throw new BadRequestException(String.format(
                    "File extension '%s' is not allowed. Allowed extensions: %s",
                    extension, allowedExtensions));
        }

        String generatedFileName = UUID.randomUUID() + "." + extension.toLowerCase();

        try {
            Path targetLocation = this.uploadPath.resolve(generatedFileName).normalize();
            if (!targetLocation.startsWith(this.uploadPath)) {
                throw new BadRequestException("Cannot store file outside target directory");
            }
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return generatedFileName;
        } catch (IOException e) {
            log.error("Failed to store file {}", generatedFileName, e);
            throw new RuntimeException("Failed to store file " + generatedFileName, e);
        }
    }

    @Override
    public Resource load(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new BadRequestException("File name cannot be empty");
        }

        try {
            Path filePath = this.uploadPath.resolve(fileName).normalize();
            if (!filePath.startsWith(this.uploadPath)) {
                throw new BadRequestException("Cannot access file outside storage directory: " + fileName);
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found: " + fileName);
            }
        } catch (MalformedURLException e) {
            throw new BadRequestException("Invalid file path: " + fileName, e);
        }
    }

    @Override
    public void delete(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return;
        }

        try {
            Path filePath = this.uploadPath.resolve(fileName).normalize();
            if (!filePath.startsWith(this.uploadPath)) {
                throw new BadRequestException("Cannot delete file outside storage directory: " + fileName);
            }

            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("Failed to delete file {}", fileName, e);
            throw new RuntimeException("Failed to delete file: " + fileName, e);
        }
    }

    public Path getUploadPath() {
        return uploadPath;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public Set<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1);
        }
        return "";
    }

    private long parseDataSize(String sizeStr) {
        try {
            return DataSize.parse(sizeStr).toBytes();
        } catch (Exception e) {
            try {
                return Long.parseLong(sizeStr);
            } catch (NumberFormatException nfe) {
                return 10 * 1024 * 1024L;
            }
        }
    }
}
