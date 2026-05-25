package com.trirang.storage;

import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;

public interface FileStorageService {
    /**
     * Stores a multipart file and returns the relative file path to be saved in the database.
     *
     * @param file the multipart file to store
     * @return the relative path/filename of the stored file
     */
    String store(MultipartFile file);

    /**
     * Resolves the relative path/filename to an absolute Path on the local filesystem.
     *
     * @param relativePath the relative path/filename of the stored file
     * @return the resolved Path
     */
    Path load(String relativePath);

    /**
     * Deletes the file stored at the given relative path.
     *
     * @param relativePath the relative path/filename of the file to delete
     */
    void delete(String relativePath);
}
