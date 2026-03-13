package com.example.eventlotterysystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for the EntrantList class.
 *
 * - adding entrants to the waiting list
 * - preventing duplicate entries
 * - enforcing the signup limit
 * - confirming chosen entrants
 */

public class EntrantListTest {
    /**
     * Creates a test UserProfile object for use in unit tests.
     *
     *  fullName full name of the test user
     *  email email of the test user
     *  username username of the test user
     *  usernameKey unique username key of the test user
     *  phoneNumber phone number of the test user
     * @return a UserProfile instance with the provided values
     */

    private UserProfile createTestEntrant(
            String fullName,
            String email,
            String username,
            String usernameKey,
            String phoneNumber
    ) {
        return new UserProfile(fullName, email, username, usernameKey, phoneNumber);
    }

    @Test
    public void testAddEntrantToWaitingList() {
        UserProfile testEntrant = createTestEntrant(
                "John Smith",
                "example@test.com",
                "User",
                "user",
                "1234567890"
        );

        EntrantList testList = new EntrantList(50);
        boolean added = testList.addEntrantToWaitingList(testEntrant);

        assertTrue(added);
        assertEquals(1, testList.getWaitingList().size());
        assertEquals(testEntrant, testList.getWaitingList().get(0));
    }

    @Test
    public void testAddDuplicateEntrantThrowsException() {
        UserProfile testEntrant = createTestEntrant(
                "John Smith",
                "example@test.com",
                "User",
                "user",
                "1234567890"
        );

        EntrantList testList = new EntrantList(50);
        testList.addEntrantToWaitingList(testEntrant);

        assertThrows(IllegalArgumentException.class, () ->
                testList.addEntrantToWaitingList(testEntrant));
    }

    @Test
    public void testSignupLimit() {
        EntrantList testList = new EntrantList(2, 1);

        UserProfile entrantOne = createTestEntrant(
                "John Smith",
                "one@test.com",
                "User1",
                "user1",
                "1234567890"
        );

        UserProfile entrantTwo = createTestEntrant(
                "Jane Smith",
                "two@test.com",
                "User2",
                "user2",
                "1234567891"
        );

        UserProfile entrantThree = createTestEntrant(
                "Bob Smith",
                "three@test.com",
                "User3",
                "user3",
                "1234567892"
        );

        assertTrue(testList.addEntrantToWaitingList(entrantOne));
        assertTrue(testList.addEntrantToWaitingList(entrantTwo));
        assertFalse(testList.addEntrantToWaitingList(entrantThree));
        assertEquals(2, testList.getWaitingList().size());
    }

    /**
     * Verifies that confirming a chosen entrant removes them from the chosen list
     * and places them into the confirmed list.
     */

    @Test
    public void testConfirmEntrant() {
        UserProfile testEntrant = createTestEntrant(
                "John Smith",
                "example@test.com",
                "User",
                "user",
                "1234567890"
        );

        EntrantList testList = new EntrantList(50);
        testList.addChoosen(testEntrant);

        boolean confirmed = testList.confirmEntrant(testEntrant);

        assertTrue(confirmed);
        assertEquals(0, testList.getChoosenList().size());
        assertEquals(1, testList.getConfirmedList().size());
        assertEquals(testEntrant, testList.getConfirmedList().get(0));
    }

    /**
     * Verifies that confirming an entrant who was never chosen fails
     * and does not modify the confirmed list.
     */

    @Test
    public void testConfirmEntrantReturnsFalseWhenNotChosen() {
        UserProfile testEntrant = createTestEntrant(
                "John Smith",
                "example@test.com",
                "User",
                "user",
                "1234567890"
        );

        EntrantList testList = new EntrantList(50);
        boolean confirmed = testList.confirmEntrant(testEntrant);

        assertFalse(confirmed);
        assertEquals(0, testList.getConfirmedList().size());
    }
}