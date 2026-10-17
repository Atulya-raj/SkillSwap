package com.skillswap.dao;

import com.skillswap.model.AvailabilitySlot;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Data access interface for user weekly availability slots.
 */
public interface AvailabilityDao {

    List<AvailabilitySlot> findByUser(long userId);

    long insert(AvailabilitySlot slot);

    void delete(long id, long userId);

    boolean hasOverlap(long userId, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, Long excludeId);
}
