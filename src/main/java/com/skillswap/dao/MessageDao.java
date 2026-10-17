package com.skillswap.dao;

import com.skillswap.model.Message;

import java.util.List;

/**
 * Data access interface for in-app thread messages.
 */
public interface MessageDao {

    long insert(Message m);

    List<Message> findThread(long sessionId);

    int countUnread(long userId);

    void markThreadRead(long sessionId, long readerId);
}
