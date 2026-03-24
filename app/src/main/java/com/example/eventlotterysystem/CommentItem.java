package com.example.eventlotterysystem;

import java.util.Date;

/**
 * Represents a comment posted on an event.
 */
public class CommentItem {
    private final String commentId;
    private final String uid;
    private final String username;
    private final String text;
    private final Date createdAt;

    /**
     * Creates a comment item.
     *
     * @param commentId the ID of the comment document
     * @param uid the ID of the user who posted the comment
     * @param username the display name of the user who posted the comment
     * @param text the comment text
     * @param createdAt the time the comment was created
     */
    public CommentItem(String commentId, String uid, String username, String text, Date createdAt) {
        this.commentId = commentId;
        this.uid = uid;
        this.username = username;
        this.text = text;
        this.createdAt = createdAt;
    }

    /**
     * Gets the comment document ID.
     *
     * @return the comment ID
     */
    public String getCommentId() {
        return commentId;
    }

    /**
     * Gets the ID of the user who posted the comment.
     *
     * @return the user ID
     */
    public String getUid() {
        return uid;
    }

    /**
     * Gets the display name of the user who posted the comment.
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Gets the comment text.
     *
     * @return the comment text
     */
    public String getText() {
        return text;
    }

    /**
     * Gets the time the comment was created.
     *
     * @return the creation time
     */
    public Date getCreatedAt() {
        return createdAt;
    }
}