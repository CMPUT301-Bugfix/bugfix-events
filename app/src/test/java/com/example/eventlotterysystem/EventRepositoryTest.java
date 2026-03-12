package com.example.eventlotterysystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Date;

public class EventRepositoryTest {

    @Test
    public void isWaitlistJoinOpen_returnsTrue_whenOpenAndDeadlineInFuture() {
        Date now = new Date();
        Date future = new Date(now.getTime() + 60_000);

        boolean result = EventRepository.isWaitlistJoinOpen(true, future, now);

        assertTrue(result);
    }

    @Test
    public void isWaitlistJoinOpen_returnsTrue_whenOpenAndDeadlineNull() {
        Date now = new Date();

        boolean result = EventRepository.isWaitlistJoinOpen(true, null, now);

        assertTrue(result);
    }

    @Test
    public void isWaitlistJoinOpen_returnsFalse_whenWaitlistClosed() {
        Date now = new Date();
        Date future = new Date(now.getTime() + 60_000);

        boolean result = EventRepository.isWaitlistJoinOpen(false, future, now);

        assertFalse(result);
    }

    @Test
    public void isWaitlistJoinOpen_returnsFalse_whenDeadlinePassed() {
        Date now = new Date();
        Date past = new Date(now.getTime() - 60_000);

        boolean result = EventRepository.isWaitlistJoinOpen(true, past, now);

        assertFalse(result);
    }

    @Test
    public void incrementWaitlistCount_increasesByOne() {
        assertEquals(1, EventRepository.incrementWaitlistCount(0));
        assertEquals(6, EventRepository.incrementWaitlistCount(5));
    }

    @Test
    public void decrementWaitlistCount_decreasesByOne() {
        assertEquals(4, EventRepository.decrementWaitlistCount(5));
        assertEquals(0, EventRepository.decrementWaitlistCount(1));
    }

    @Test
    public void decrementWaitlistCount_doesNotGoBelowZero() {
        assertEquals(0, EventRepository.decrementWaitlistCount(0));
    }
}