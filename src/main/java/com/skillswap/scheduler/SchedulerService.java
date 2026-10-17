package com.skillswap.scheduler;

import com.skillswap.util.Config;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages periodic background tasks (match scanning, request expiry, session reminders, auto-completion).
 */
public class SchedulerService {

    private static final Logger LOGGER = Logger.getLogger(SchedulerService.class.getName());
    private static volatile SchedulerService instance;

    private ScheduledExecutorService executor;
    private final MatchScanTask matchScanTask;
    private final ExpiryTask expiryTask;
    private final ReminderTask reminderTask;
    private final AutoCompleteTask autoCompleteTask;
    private volatile boolean started;

    /**
     * Default constructor wiring standard task instances.
     */
    public SchedulerService() {
        this(new MatchScanTask(), new ExpiryTask(), new ReminderTask(), new AutoCompleteTask());
    }

    /**
     * Constructs SchedulerService with injected tasks.
     *
     * @param matchScanTask match scan task
     * @param expiryTask expiry task
     * @param reminderTask reminder task
     * @param autoCompleteTask auto complete task
     */
    public SchedulerService(MatchScanTask matchScanTask, ExpiryTask expiryTask,
                            ReminderTask reminderTask, AutoCompleteTask autoCompleteTask) {
        this.matchScanTask = matchScanTask;
        this.expiryTask = expiryTask;
        this.reminderTask = reminderTask;
        this.autoCompleteTask = autoCompleteTask;
        this.started = false;
    }

    /**
     * Returns singleton instance.
     *
     * @return global SchedulerService instance
     */
    public static SchedulerService getInstance() {
        if (instance == null) {
            synchronized (SchedulerService.class) {
                if (instance == null) {
                    instance = new SchedulerService();
                }
            }
        }
        return instance;
    }

    /**
     * Starts the 2-thread scheduled background engine with configured intervals.
     */
    public synchronized void start() {
        if (started) {
            return;
        }

        int matchSeconds = Config.getInt("scheduler.match.seconds", 60);
        int expiryMinutes = Config.getInt("scheduler.expiry.minutes", 5);
        int reminderMinutes = Config.getInt("scheduler.reminder.minutes", 5);
        int autoCompleteMinutes = Config.getInt("scheduler.autocomplete.minutes", 10);

        executor = Executors.newScheduledThreadPool(2, new ThreadFactory() {
            private final AtomicInteger count = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "skillswap-scheduler-" + count.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        });

        executor.scheduleWithFixedDelay(matchScanTask, 5, Math.max(5, matchSeconds), TimeUnit.SECONDS);
        executor.scheduleWithFixedDelay(expiryTask, 10, Math.max(10, expiryMinutes * 60L), TimeUnit.SECONDS);
        executor.scheduleWithFixedDelay(reminderTask, 15, Math.max(15, reminderMinutes * 60L), TimeUnit.SECONDS);
        executor.scheduleWithFixedDelay(autoCompleteTask, 20, Math.max(20, autoCompleteMinutes * 60L), TimeUnit.SECONDS);

        started = true;
        LOGGER.log(Level.INFO, "SchedulerService started (match: {0}s, expiry: {1}m, reminder: {2}m, autocomplete: {3}m).",
                new Object[]{matchSeconds, expiryMinutes, reminderMinutes, autoCompleteMinutes});
    }

    /**
     * Shuts down the scheduled executor cleanly, waiting up to 10 seconds before forcing termination.
     */
    public synchronized void stop() {
        if (!started || executor == null) {
            return;
        }
        LOGGER.log(Level.INFO, "Shutting down SchedulerService...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                LOGGER.log(Level.WARNING, "SchedulerService did not terminate in 10s, forcing shutdown...");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        started = false;
        LOGGER.log(Level.INFO, "SchedulerService shut down successfully.");
    }

    public boolean isStarted() {
        return started;
    }

    public MatchScanTask getMatchScanTask() {
        return matchScanTask;
    }

    public ExpiryTask getExpiryTask() {
        return expiryTask;
    }

    public ReminderTask getReminderTask() {
        return reminderTask;
    }

    public AutoCompleteTask getAutoCompleteTask() {
        return autoCompleteTask;
    }
}
