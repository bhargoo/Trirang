package com.trirang.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path rootLocation;

    public LocalFileStorageService(@Value("${storage.local.upload-dir:./uploads}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
            log.info("Initialized local file storage directory at: {}", this.rootLocation);
        } catch (IOException e) {
            log.error("Could not create the directory where the uploaded files will be stored.", e);
            throw new RuntimeException("Could not initialize storage directory", e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Failed to store empty file.");
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        if (originalFilename.contains("..")) {
            // Security check to prevent directory traversal
            throw new IllegalArgumentException("Cannot store file with relative path outside current directory " + originalFilename);
        }

        // Generate a unique file name to avoid collisions
        String fileExtension = "";
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            fileExtension = originalFilename.substring(lastDotIndex);
        }
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

        try {
            Path destinationFile = this.rootLocation.resolve(Paths.get(uniqueFilename))
                    .normalize().toAbsolutePath();

            if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
                throw new IllegalArgumentException("Cannot store file outside current directory.");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
                log.debug("Saved file successfully to {}", destinationFile);
                return uniqueFilename;
            }
        } catch (IOException e) {
            log.error("Failed to store file {}", originalFilename, e);
            throw new RuntimeException("Failed to store file.", e);
        }
    }

    @Override
    public Path load(String relativePath) {
        Path resolvedPath = this.rootLocation.resolve(relativePath).normalize();
        if (!resolvedPath.startsWith(this.rootLocation)) {
            throw new IllegalArgumentException("Cannot access files outside storage directory.");
        }
        return resolvedPath;
    }

    @Override
    public void delete(String relativePath) {
        try {
            Path file = load(relativePath);
            Files.deleteIfExists(file);
            log.debug("Deleted file successfully at {}", file);
        } catch (IOException e) {
            log.error("Could not delete file at path: {}", relativePath, e);
            throw new RuntimeException("Could not delete file.", e);
        }
    }
}
