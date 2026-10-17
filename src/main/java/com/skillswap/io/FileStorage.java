package com.skillswap.io;

import com.skillswap.exception.ValidationException;
import com.skillswap.util.Config;
import com.skillswap.util.Validator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles avatar file storage on disk including magic bytes verification and size limits.
 */
public class FileStorage {

    private static final Logger LOGGER = Logger.getLogger(FileStorage.class.getName());
    private static final FileStorage INSTANCE = new FileStorage();
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2 MB

    private FileStorage() {
    }

    /**
     * Returns the singleton instance of FileStorage.
     *
     * @return file storage singleton
     */
    public static FileStorage getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the configured upload directory path.
     *
     * @return upload directory path
     */
    public Path getUploadDirectory() {
        String dir = Config.get("upload.dir", System.getProperty("user.home") + "/skillswap/uploads");
        Path path = Paths.get(dir).toAbsolutePath().normalize();
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to create upload directory: " + path, e);
        }
        return path;
    }

    /**
     * Validates and saves an uploaded avatar image, deleting the old avatar if present.
     *
     * @param originalFilename client original file name
     * @param inputStream data input stream
     * @param sizeBytes declared or measured size
     * @param oldAvatarFilename old avatar filename to delete if not null
     * @return new stored filename
     * @throws ValidationException if file is invalid or exceeds limit
     * @throws IOException on I/O error
     */
    public String saveAvatar(String originalFilename, InputStream inputStream, long sizeBytes,
                             String oldAvatarFilename) throws ValidationException, IOException {
        Path uploadDir = getUploadDirectory();

        // Read up to 2MB + 1 byte into memory or buffer to check size and magic bytes
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int bytesRead;
        long totalRead = 0;

        while ((bytesRead = inputStream.read(chunk)) != -1) {
            totalRead += bytesRead;
            if (totalRead > MAX_FILE_SIZE) {
                Validator.validateImageUpload(originalFilename, totalRead, new byte[0]);
            }
            buffer.write(chunk, 0, bytesRead);
        }

        byte[] allBytes = buffer.toByteArray();
        byte[] headerBytes = new byte[Math.min(16, allBytes.length)];
        System.arraycopy(allBytes, 0, headerBytes, 0, headerBytes.length);

        // Validate extension, size and magic bytes
        Validator.validateImageUpload(originalFilename, allBytes.length, headerBytes);

        String ext = ".jpg";
        String lower = originalFilename.trim().toLowerCase();
        if (lower.endsWith(".png")) {
            ext = ".png";
        } else if (lower.endsWith(".jpeg") || lower.endsWith(".jpg")) {
            ext = ".jpg";
        }

        String newFilename = UUID.randomUUID().toString() + ext;
        Path targetPath = uploadDir.resolve(newFilename).normalize();

        try (OutputStream os = new FileOutputStream(targetPath.toFile())) {
            os.write(allBytes);
        }

        // Delete old avatar if present
        if (oldAvatarFilename != null && !oldAvatarFilename.trim().isEmpty()) {
            deleteAvatar(oldAvatarFilename.trim());
        }

        return newFilename;
    }

    /**
     * Resolves and returns the avatar file safely against path traversal.
     *
     * @param avatarFilename stored avatar file name
     * @return existing file or null if not found
     */
    public File getAvatarFile(String avatarFilename) {
        if (avatarFilename == null || avatarFilename.trim().isEmpty()) {
            return null;
        }
        Path uploadDir = getUploadDirectory();
        Path resolved = uploadDir.resolve(avatarFilename.trim()).normalize();
        if (!resolved.startsWith(uploadDir)) {
            // Path traversal attempt
            return null;
        }
        File file = resolved.toFile();
        return file.exists() && file.isFile() ? file : null;
    }

    /**
     * Deletes an avatar file if it exists.
     *
     * @param avatarFilename stored avatar filename
     */
    public void deleteAvatar(String avatarFilename) {
        File file = getAvatarFile(avatarFilename);
        if (file != null && file.exists()) {
            try {
                file.delete();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to delete old avatar file: " + avatarFilename, e);
            }
        }
    }
}
