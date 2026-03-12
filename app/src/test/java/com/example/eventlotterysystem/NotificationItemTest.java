package com.example.eventlotterysystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Local unit tests for the {@link NotificationItem} data model.
 */
public class NotificationItemTest {

    /**
     * Tests that the constructor correctly initializes
     * and sets the default status of notification to "PENDING"
     */
    @Test
    public void testConstructor() {
        String eventId = "event123";
        String title = "Congratulations!";
        String message = "You won the lottery!";
        String type = "WIN";

        NotificationItem item = new NotificationItem(eventId, title, message, type);

        assertEquals(eventId, item.getEventId());
        assertEquals(title, item.getTitle());
        assertEquals(message, item.getMessage());
        assertEquals(type, item.getType());
        assertEquals("PENDING", item.getStatus());
        assertTrue(item.getTimestamp() > 0);
    }

    /**
     * Tests that setters and getters work for all fields
     */
    @Test
    public void testSettersAndGetters() {
        NotificationItem item = new NotificationItem();

        item.setId("notifId");
        assertEquals("notifId", item.getId());

        item.setEventId("eventId");
        assertEquals("eventId", item.getEventId());

        item.setTitle("title");
        assertEquals("title", item.getTitle());

        item.setMessage("message");
        assertEquals("message", item.getMessage());

        item.setType("GENERAL");
        assertEquals("GENERAL", item.getType());

        item.setStatus("READ");
        assertEquals("READ", item.getStatus());

        long now = System.currentTimeMillis();
        item.setTimestamp(now);
        assertEquals(now, item.getTimestamp());
    }
}
