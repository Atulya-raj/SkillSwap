package com.skillswap.scheduler;

import com.skillswap.service.SessionService;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Scheduled background task expiring stale pending session requests and refunding held credits (F-26).
 */
public class ExpiryTask implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(ExpiryTask.class.getName());

    private final SessionService sessionService;

    /**
     * Default constructor wiring standard SessionService.
     */
    public ExpiryTask() {
        this(new SessionService());
    }

    /**
     * Constructs ExpiryTask with injected SessionService.
     *
     * @param sessionService session service
     */
    public ExpiryTask(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public void run() {
        try {
            int expiredCount = sessionService.expireStale();
            if (expiredCount > 0) {
                LOGGER.info("ExpiryTask expired " + expiredCount + " stale session request(s).");
            }
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Error executing ExpiryTask", t);
        }
    }
}
