package com.skillswap.service;

import com.skillswap.dao.AvailabilityDao;
import com.skillswap.dao.jdbc.JdbcAvailabilityDao;
import com.skillswap.exception.ValidationException;
import com.skillswap.model.AvailabilitySlot;
import com.skillswap.model.enums.SessionMode;
import com.skillswap.util.Validator;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service managing user weekly availability schedules and overlap detection.
 */
public class AvailabilityService {

    private static final AvailabilityService INSTANCE = new AvailabilityService();

    private final AvailabilityDao availabilityDao;

    public AvailabilityService() {
        this(new JdbcAvailabilityDao());
    }

    public AvailabilityService(AvailabilityDao availabilityDao) {
        this.availabilityDao = availabilityDao;
    }

    /**
     * Returns the singleton instance of AvailabilityService.
     *
     * @return availability service singleton
     */
    public static AvailabilityService getInstance() {
        return INSTANCE;
    }

    /**
     * Returns all weekly availability slots configured for a user.
     *
     * @param userId user id
     * @return list of availability slots
     */
    public List<AvailabilitySlot> slots(long userId) {
        return availabilityDao.findByUser(userId);
    }

    /**
     * Adds a new availability slot for a user after checking for intra-day time overlaps.
     *
     * @param userId user id
     * @param dayOfWeek day of the week
     * @param startTime slot start time
     * @param endTime slot end time
     * @param mode session mode (ONLINE, IN_PERSON, HYBRID)
     * @return created slot id
     * @throws ValidationException if times are invalid or overlap exists
     */
    public long add(long userId, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime)
            throws ValidationException {
        Validator.validateAvailability(dayOfWeek, startTime, endTime);

        if (availabilityDao.hasOverlap(userId, dayOfWeek, startTime, endTime, null)) {
            Map<String, String> errors = new HashMap<>();
            errors.put("time", "This time slot overlaps with an existing slot on " + dayOfWeek.name() + ".");
            throw new ValidationException(errors);
        }

        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setUserId(userId);
        slot.setDayOfWeek(dayOfWeek);
        slot.setStartTime(startTime);
        slot.setEndTime(endTime);

        return availabilityDao.insert(slot);
    }

    public long add(long userId, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, SessionMode mode)
            throws ValidationException {
        return add(userId, dayOfWeek, startTime, endTime);
    }

    /**
     * Removes an availability slot belonging to a user.
     *
     * @param userId user id
     * @param slotId slot id
     */
    public void remove(long userId, long slotId) {
        availabilityDao.delete(slotId, userId);
    }
}
