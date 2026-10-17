package com.skillswap.io;

import com.skillswap.model.RequestDraft;
import com.skillswap.util.Config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * File-based persistence store for serializable RequestDraft objects.
 */
public class DraftStore {

    private static final Logger LOGGER = Logger.getLogger(DraftStore.class.getName());
    private final Path draftsDir;

    /**
     * Default constructor using drafts.dir from configuration.
     */
    public DraftStore() {
        String dir = Config.get("drafts.dir", System.getProperty("user.home") + File.separator + "skillswap" + File.separator + "drafts");
        this.draftsDir = Paths.get(dir);
        ensureDirectoryExists();
    }

    /**
     * Constructs DraftStore with custom directory path.
     *
     * @param draftsDir directory path to store drafts
     */
    public DraftStore(Path draftsDir) {
        this.draftsDir = draftsDir;
        ensureDirectoryExists();
    }

    private void ensureDirectoryExists() {
        try {
            if (!Files.exists(draftsDir)) {
                Files.createDirectories(draftsDir);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to create drafts directory: " + draftsDir, e);
        }
    }

    private Path getDraftPath(long userId, long listingId) {
        return draftsDir.resolve("draft_" + userId + "_" + listingId + ".ser");
    }

    /**
     * Saves a request draft to disk.
     *
     * @param userId ID of the learner
     * @param listingId ID of the provider listing
     * @param draft draft object to persist
     */
    public synchronized void save(long userId, long listingId, RequestDraft draft) {
        ensureDirectoryExists();
        Path file = getDraftPath(userId, listingId);
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file.toFile()))) {
            oos.writeObject(draft);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save draft for user " + userId + " listing " + listingId, e);
        }
    }

    /**
     * Loads a request draft from disk if it exists.
     *
     * @param userId ID of the learner
     * @param listingId ID of the provider listing
     * @return Optional containing RequestDraft or empty if none exists
     */
    public synchronized Optional<RequestDraft> load(long userId, long listingId) {
        Path file = getDraftPath(userId, listingId);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file.toFile()))) {
            Object obj = ois.readObject();
            if (obj instanceof RequestDraft) {
                return Optional.of((RequestDraft) obj);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load draft from " + file + ", cleaning corrupted file", e);
            delete(userId, listingId);
        }
        return Optional.empty();
    }

    /**
     * Deletes a request draft from disk if present.
     *
     * @param userId ID of the learner
     * @param listingId ID of the provider listing
     */
    public synchronized void delete(long userId, long listingId) {
        Path file = getDraftPath(userId, listingId);
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to delete draft: " + file, e);
        }
    }
}
