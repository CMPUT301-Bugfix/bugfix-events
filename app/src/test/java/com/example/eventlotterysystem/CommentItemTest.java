package com.example.eventlotterysystem;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Date;

/**
 * Unit tests for the CommentItem model.
 */
public class CommentItemTest {

    /**
     * Verifies that the constructor stores all comment fields correctly.
     */
    @Test
    public void constructor_storesAllFieldsCorrectly() {
        Date createdAt = new Date();

        CommentItem comment = new CommentItem(
                "comment123",
                "user123",
                "Bilal",
                "This is a test comment",
                createdAt
        );

        assertEquals("comment123", comment.getCommentId());
        assertEquals("user123", comment.getUid());
        assertEquals("Bilal", comment.getUsername());
        assertEquals("This is a test comment", comment.getText());
        assertEquals(createdAt, comment.getCreatedAt());
    }
}