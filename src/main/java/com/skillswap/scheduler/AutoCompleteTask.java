package com.skillswap.scheduler;

import com.skillswap.service.SessionService;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Scheduled background task completing sessions when one side confirmed and 48 hours have elapsed (F-28).
 */
public class AutoCompleteTask implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(AutoCompleteTask.class.getName());

    private final SessionService sessionService;

    /**
     * Default constructor wiring standard SessionService.
     */
    public AutoCompleteTask() {
        this(new SessionService());
    }

    /**
     * Constructs AutoCompleteTask with injected SessionService.
     *
     * @param sessionService session service
     */
    public AutoCompleteTask(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public void run() {
        try {
            int completedCount = sessionService.autoCompleteHalfConfirmed();
            if (completedCount > 0) {
                LOGGER.info("AutoCompleteTask completed " + completedCount + " half-confirmed session(s).");
            }
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Error executing AutoCompleteTask", t);
        }
    }
}
