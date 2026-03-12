package com.example.eventlotterysystem;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Local unit tests for the logic used during the lottery draw.
 * specifically tests math used to determine how many winners to draw
 */
public class EventRepositoryMathTest {

    /**
     * checks that the draw count correctly fills the remaining available spots
     * when there are more candidates than available capacity
     */
    @Test
    public void calculateDrawCount_fillsAvailableSpots() {
        // Event has 10 spots, 2 are already filled, 20 people waiting
        int candidates = 20;
        int maxParticipants = 10;
        int alreadyChosen = 2;
        
        int result = EventRepository.calculateDrawCount(candidates, maxParticipants, alreadyChosen);
        assertEquals(8, result);
    }

    /**
     * checks that the draw count is limited by the number of candidates
     * when there are fewer people waiting than there are available spots
     */
    @Test
    public void calculateDrawCount_handlesFewerCandidatesThanSpots() {
        // Event has 10 spots, 0 filled, only 5 people waiting
        int candidates = 5;
        int maxParticipants = 10;
        int alreadyChosen = 0;
        
        int result = EventRepository.calculateDrawCount(candidates, maxParticipants, alreadyChosen);
        assertEquals(5, result);
    }

    /**
     * checks that the draw count is zero when the event has already reached
     * its maximum capacity of chosen or confirmed participants
     */
    @Test
    public void calculateDrawCount_handlesFullEvent() {
        // Event is already full
        int candidates = 50;
        int maxParticipants = 10;
        int alreadyChosen = 10;
        
        int result = EventRepository.calculateDrawCount(candidates, maxParticipants, alreadyChosen);
        assertEquals(0, result);
    }

    /**
     * check that the draw count is zero if for some weird other reason the number of
     * already chosen participants exceeds the maximum participant limit
     */
    @Test
    public void calculateDrawCount_handlesOverfilledEvent() {
        // More people confirmed than the limit (shouldn't happen, but test safety)
        int candidates = 10;
        int maxParticipants = 10;
        int alreadyChosen = 12;
        
        int result = EventRepository.calculateDrawCount(candidates, maxParticipants, alreadyChosen);
        assertEquals(0, result);
    }
}
