package com.example.eventlotterysystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EntrantListTest {

    private UserProfile createTestEntrant() {
        return new UserProfile("Jim Beasty","example@test.com","User", "IDK","1234567890");
    }

    @Test
    public void testAddEntrantToWaitingList() {
        boolean inWaitingList = false;
        UserProfile testEntrant = createTestEntrant();
        EntrantList testList = new EntrantList(50);
        testList.addEntrantToWaitingList(testEntrant);
        for (UserProfile Entrant: testList.getWaitingList()) {
            if (Entrant.getUsernameKey().equals(testEntrant.getUsernameKey())) {
                inWaitingList = true;
                break;
            }
        }
        assertTrue(inWaitingList);
    }

    @Test
    public void testsignupLimit() {
        EntrantList testList = new EntrantList(2, 5);
        UserProfile u1 = new UserProfile("U1", "e1", "u1", "k1", "1");
        UserProfile u2 = new UserProfile("U2", "e2", "u2", "k2", "2");
        UserProfile u3 = new UserProfile("U3", "e3", "u3", "k3", "3");

        assertTrue(testList.addEntrantToWaitingList(u1));
        assertTrue(testList.addEntrantToWaitingList(u2));
        assertFalse(testList.addEntrantToWaitingList(u3));
        assertEquals(2, testList.getWaitingList().size());
    }

    @Test
    public void testConfirmEntrant() {
        boolean inConfirmedList = false;
        UserProfile testEntrant = createTestEntrant();
        EntrantList testList = new EntrantList(50);
        testList.addChoosen(testEntrant);
        testList.confirmEntrant(testEntrant);
        for (UserProfile Entrant: testList.getConfirmedList()) {
            if (Entrant.getUsernameKey().equals(testEntrant.getUsernameKey())) {
                inConfirmedList = true;
                break;
            }
        }
        assertTrue(inConfirmedList);
    }

    @Test
    public void testChoosing() {
        // Test that Choosing moves the correct number of entrants to the chosen list
        EntrantList testList = new EntrantList(10, 2);
        UserProfile u1 = new UserProfile("U1", "e1", "u1", "k1", "1");
        UserProfile u2 = new UserProfile("U2", "e2", "u2", "k2", "2");
        UserProfile u3 = new UserProfile("U3", "e3", "u3", "k3", "3");

        testList.addEntrantToWaitingList(u1);
        testList.addEntrantToWaitingList(u2);
        testList.addEntrantToWaitingList(u3);

        testList.Choosing();

        // 2 should be chosen, 1 remains in waiting list
        assertEquals(2, testList.getChoosenList().size());
        assertEquals(1, testList.getWaitingList().size());

        // Verify that entrants moved from waiting list to chosen list
        for (UserProfile chosen : testList.getChoosenList()) {
            assertFalse(testList.getWaitingList().contains(chosen));
        }
    }

    @Test
    public void testChoosingEmptyWaitingList() {
        EntrantList testList = new EntrantList(10, 5);
        testList.Choosing();
        assertEquals(0, testList.getChoosenList().size());
    }

    @Test
    public void testChoosingFewerThanLimit() {
        EntrantList testList = new EntrantList(10, 5);
        UserProfile u1 = new UserProfile("U1", "e1", "u1", "k1", "1");
        UserProfile u2 = new UserProfile("U2", "e2", "u2", "k2", "2");

        testList.addEntrantToWaitingList(u1);
        testList.addEntrantToWaitingList(u2);

        testList.Choosing();

        assertEquals(2, testList.getChoosenList().size());
        assertEquals(0, testList.getWaitingList().size());
    }
}