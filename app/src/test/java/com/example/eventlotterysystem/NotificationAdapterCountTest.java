package com.example.eventlotterysystem;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Local unit tests for the {@link NotificationAdapter} class.
 * These tests focus on the data handling and item count logic of the adapter
 * They are basic and don't touch firebase, just checking that the count is correct
 */
public class NotificationAdapterCountTest {

    /**
     * checks that {@link NotificationAdapter#getItemCount()} returns the correct
     * size when the list is populated with notification items
     */
    @Test
    public void getItemCount_returnsCorrectSize() {
        List<NotificationItem> list = new ArrayList<>();
        list.add(new NotificationItem("e1", "t1", "m1", "GENERAL"));
        list.add(new NotificationItem("e2", "t2", "m2", "WIN"));
        
        NotificationAdapter adapter = new NotificationAdapter(list, null);
        assertEquals(2, adapter.getItemCount());
    }

    /**
     * checks that {@link NotificationAdapter#getItemCount()} returns zero
     * when an empty list is provided to the adapter
     */
    @Test
    public void getItemCount_returnsZeroForEmptyList() {
        List<NotificationItem> list = new ArrayList<>();
        NotificationAdapter adapter = new NotificationAdapter(list, null);
        assertEquals(0, adapter.getItemCount());
    }
}
