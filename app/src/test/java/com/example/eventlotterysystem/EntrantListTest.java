package com.example.eventlotterysystem;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EntrantListTest {

    private UserProfile createTestEntrant() {
        return new UserProfile("John Smith","example@test.com","User", "IDK","1234567890");
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
        //TODO
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
}

