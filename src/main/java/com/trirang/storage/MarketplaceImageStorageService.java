package com.trirang.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Slf4j
@Service
public class MarketplaceImageStorageService {

    private final Path rootLocation;
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    public MarketplaceImageStorageService(
            @Value("${trirang.marketplace.upload-dir:uploads/listings}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
            log.info("Initialized marketplace image storage at: {}", this.rootLocation);
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize marketplace image storage directory", e);
        }
    }

    public List<String> storeImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return new ArrayList<>();
        }

        if (files.size() > 5) {
            throw new IllegalArgumentException("Maximum of 5 images can be uploaded for a single listing");
        }

        List<String> storedPaths = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            // Size check
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new IllegalArgumentException("File " + file.getOriginalFilename() + " exceeds maximum size limit of 10MB");
            }

            // MIME type check
            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
                throw new IllegalArgumentException("File " + file.getOriginalFilename() + " has invalid MIME type: " + contentType + ". Only JPEG, PNG, and WEBP images are allowed.");
            }

            // Generate unique name
            String extension = "png";
            if (contentType.contains("jpeg")) extension = "jpg";
            else if (contentType.contains("webp")) extension = "webp";

            String filename = UUID.randomUUID().toString() + "." + extension;
            Path destinationFile = this.rootLocation.resolve(Paths.get(filename)).normalize().toAbsolutePath();

            // Prevent path traversal
            if (!destinationFile.getParent().equals(this.rootLocation)) {
                throw new IllegalArgumentException("Cannot store file outside current directory");
            }

            try {
                Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
                log.info("Stored listing image successfully at: {}", destinationFile);
                storedPaths.add("listings/" + filename);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to store file: " + file.getOriginalFilename(), e);
            }
        }

        return storedPaths;
    }

    public void deleteImage(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        
        String filename = relativePath;
        if (filename.startsWith("listings/")) {
            filename = filename.substring("listings/".length());
        }

        Path file = this.rootLocation.resolve(filename).normalize().toAbsolutePath();
        try {
            if (Files.exists(file) && file.getParent().equals(this.rootLocation)) {
                Files.delete(file);
                log.info("Deleted listing image: {}", file);
            }
        } catch (IOException e) {
            log.error("Failed to delete marketplace image: {}", file, e);
        }
    }
}
