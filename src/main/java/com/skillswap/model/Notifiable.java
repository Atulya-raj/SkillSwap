package com.skillswap.model;

/**
 * Interface implemented by entities capable of receiving notifications.
 */
public interface Notifiable {

    /**
     * Returns the unique identifier of the notifiable entity.
     *
     * @return unique ID
     */
    long getId();

    /**
     * Returns the human-readable display name for notifications.
     *
     * @return display name
     */
    String displayName();
}
