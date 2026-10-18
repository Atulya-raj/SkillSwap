package com.skillswap.model;

import com.skillswap.model.enums.SessionMode;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Weekly recurring availability slot for a user.
 */
public class AvailabilitySlot {

    private long id;
    private long userId;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private SessionMode mode = SessionMode.ONLINE;

    /**
     * Default constructor.
     */
    public AvailabilitySlot() {
    }

    /**
     * Constructs an availability slot with all properties.
     *
     * @param userId user ID
     * @param dayOfWeek day of the week
     * @param startTime slot start time
     * @param endTime slot end time
     */
    public AvailabilitySlot(long userId, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this(userId, dayOfWeek, startTime, endTime, SessionMode.ONLINE);
    }

    public AvailabilitySlot(long userId, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, SessionMode mode) {
        this.userId = userId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.mode = mode != null ? mode : SessionMode.ONLINE;
    }

    /**
     * Calculates the overlapping minutes between this slot and another slot.
     *
     * @param other another availability slot
     * @return overlapping minutes, or 0 if on different days or no overlap
     */
    public int overlapMinutes(AvailabilitySlot other) {
        if (other == null || this.dayOfWeek != other.dayOfWeek) {
            return 0;
        }
        LocalTime maxStart = this.startTime.isAfter(other.startTime) ? this.startTime : other.startTime;
        LocalTime minEnd = this.endTime.isBefore(other.endTime) ? this.endTime : other.endTime;

        if (maxStart.isBefore(minEnd)) {
            return (int) ChronoUnit.MINUTES.between(maxStart, minEnd);
        }
        return 0;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public SessionMode getMode() {
        return mode != null ? mode : SessionMode.ONLINE;
    }

    public void setMode(SessionMode mode) {
        this.mode = mode != null ? mode : SessionMode.ONLINE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AvailabilitySlot that = (AvailabilitySlot) o;
        return id != 0 && id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AvailabilitySlot{id=" + id + ", userId=" + userId + ", day=" + dayOfWeek + ", " + startTime + "-" + endTime + "}";
    }
}
