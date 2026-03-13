package com.example.eventlotterysystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Date;

/**
 * Unit tests for helper logic in EventRepository.
 *
 * These tests verify waitlist-related helper methods used by the app,
 * including waitlist open/closed checks and waitlist count updates.
 */

public class EventRepositoryTest {

    /**
     * Verifies that the waitlist is considered open when the waitlist flag is true
     * and the registration deadline is in the future.
     */

    @Test
    public void isWaitlistJoinOpen_returnsTrue_whenOpenAndDeadlineInFuture() {
        Date now = new Date();
        Date future = new Date(now.getTime() + 60_000);

        boolean result = EventRepository.isWaitlistJoinOpen(true, future, now);

        assertTrue(result);
    }

    /**
     * Verifies that the waitlist is considered open when the waitlist flag is true
     * and no registration deadline has been set.
     */

    @Test
    public void isWaitlistJoinOpen_returnsTrue_whenOpenAndDeadlineNull() {
        Date now = new Date();

        boolean result = EventRepository.isWaitlistJoinOpen(true, null, now);

        assertTrue(result);
    }

    /**
     * Verifies that the waitlist is considered closed when the waitlist flag is false,
     * even if the registration deadline is still in the future.
     */

    @Test
    public void isWaitlistJoinOpen_returnsFalse_whenWaitlistClosed() {
        Date now = new Date();
        Date future = new Date(now.getTime() + 60_000);

        boolean result = EventRepository.isWaitlistJoinOpen(false, future, now);

        assertFalse(result);
    }

    /**
     * Verifies that the waitlist is considered closed when the registration deadline
     * has already passed.
     */

    @Test
    public void isWaitlistJoinOpen_returnsFalse_whenDeadlinePassed() {
        Date now = new Date();
        Date past = new Date(now.getTime() - 60_000);

        boolean result = EventRepository.isWaitlistJoinOpen(true, past, now);

        assertFalse(result);
    }

    /**
     * Verifies that incrementWaitlistCount increases the current waitlist count by one.
     */

    @Test
    public void incrementWaitlistCount_increasesByOne() {
        assertEquals(1, EventRepository.incrementWaitlistCount(0));
        assertEquals(6, EventRepository.incrementWaitlistCount(5));
    }

    /**
     * Verifies that decrementWaitlistCount decreases the current waitlist count by one.
     */

    @Test
    public void decrementWaitlistCount_decreasesByOne() {
        assertEquals(4, EventRepository.decrementWaitlistCount(5));
        assertEquals(0, EventRepository.decrementWaitlistCount(1));
    }

    /**
     * Verifies that decrementWaitlistCount never returns a value below zero.
     */

    @Test
    public void decrementWaitlistCount_doesNotGoBelowZero() {
        assertEquals(0, EventRepository.decrementWaitlistCount(0));
    }
}