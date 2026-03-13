package com.example.eventlotterysystem;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @deprecated
 */
public class EventTest {
    private UserProfile createTestEntrant() {
        return new UserProfile("John Smith","example@test.com","User", "IDK","1234567890");
    }

    private Event createTestEvent() {
        return new Event(100, 10, "Test Event", "This Event is not a real Event and is for testing purposes only", LocalDateTime.now(), LocalDateTime.now(), false);
    }

    @Test
    public void signUpTest() {
        boolean inWaitingList = false;
        UserProfile testEntrant = createTestEntrant();
        Event testEvent = createTestEvent();
        testEvent.signUp(testEntrant);
        for (UserProfile Entrant: testEvent.getEntrantList().getWaitingList()) {
            if (Entrant.getUsernameKey().equals(testEntrant.getUsernameKey())) {
                inWaitingList = true;
                break;
            }
        }
        assertTrue(inWaitingList);
    }

    @Test
    public void locationIsStoredOnEvent() {
        Event event = new Event(
                100,
                10,
                "Test Event",
                "This Event is not a real Event and is for testing purposes only",
                "Edmonton",
                LocalDateTime.now(),
                LocalDateTime.now(),
                false
        );
        assertTrue("Edmonton".equals(event.getLocation()));
    }

}
