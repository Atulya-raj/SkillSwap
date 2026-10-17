package com.skillswap.service;

import com.skillswap.io.DraftStore;
import com.skillswap.model.RequestDraft;

import java.util.Optional;

/**
 * Service for saving, restoring, and deleting session request drafts.
 */
public class DraftService {

    private final DraftStore draftStore;

    /**
     * Default constructor using standard DraftStore.
     */
    public DraftService() {
        this(new DraftStore());
    }

    /**
     * Constructs DraftService with injected DraftStore.
     *
     * @param draftStore store for draft persistence
     */
    public DraftService(DraftStore draftStore) {
        this.draftStore = draftStore;
    }

    /**
     * Saves or updates a session request draft.
     *
     * @param userId ID of the learner
     * @param listingId ID of the provider listing
     * @param draft draft data to persist
     */
    public void save(long userId, long listingId, RequestDraft draft) {
        draftStore.save(userId, listingId, draft);
    }

    /**
     * Loads a session request draft if one exists.
     *
     * @param userId ID of the learner
     * @param listingId ID of the provider listing
     * @return Optional containing RequestDraft or empty
     */
    public Optional<RequestDraft> load(long userId, long listingId) {
        return draftStore.load(userId, listingId);
    }

    /**
     * Deletes a session request draft.
     *
     * @param userId ID of the learner
     * @param listingId ID of the provider listing
     */
    public void delete(long userId, long listingId) {
        draftStore.delete(userId, listingId);
    }
}
