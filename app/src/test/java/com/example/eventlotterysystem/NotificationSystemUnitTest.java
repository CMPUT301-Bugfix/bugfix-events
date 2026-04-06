package com.example.eventlotterysystem;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for notification-related model classes and logic.
 */
public class NotificationSystemUnitTest {

    /**
     * Verifies that a NotificationItem is correctly initialized with default values.
     */
    @Test
    public void testNotificationItemInitialization() {
        String eventId = "test-event-id";
        String title = "Test Title";
        String message = "Test Message";
        String type = "INVITE";

        NotificationItem item = new NotificationItem(eventId, title, message, type);

        assertEquals(eventId, item.getEventId());
        assertEquals(title, item.getTitle());
        assertEquals(message, item.getMessage());
        assertEquals(type, item.getType());
        assertEquals("PENDING", item.getStatus());
        assertTrue(item.getTimestamp() > 0);
    }

    /**
     * Verifies status updates for a NotificationItem.
     */
    @Test
    public void testNotificationStatusUpdate() {
        NotificationItem item = new NotificationItem("id", "title", "msg", "type");
        
        item.setStatus("ACCEPTED");
        assertEquals("ACCEPTED", item.getStatus());
        
        item.setStatus("REJECTED");
        assertEquals("REJECTED", item.getStatus());
    }
}
