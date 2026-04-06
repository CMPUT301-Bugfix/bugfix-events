package com.example.eventlotterysystem;

import java.util.Date;

/**
 * Represents a comment posted on an event.
 *
 * <p>This model supports threaded discussions by storing the parent comment ID,
 * nesting depth, aggregate vote score, and soft-delete state.</p>
 */
public class CommentItem {
    private final String commentId;
    private final String uid;
    private final String username;
    private final String text;
    private final Date createdAt;
    private final String parentCommentId;
    private final int depth;
    private final int score;
    private final boolean deleted;

    /**
     * Creates a comment item.
     *
     * @param commentId the Firestore document ID for the comment
     * @param uid the user ID of the comment author
     * @param username the display name shown with the comment
     * @param text the stored comment text
     * @param createdAt the timestamp when the comment was created
     * @param parentCommentId the parent comment ID, or an empty string for top-level comments
     * @param depth the nesting depth of the comment
     * @param score the total vote score for the comment
     * @param deleted whether the comment has been soft-deleted
     */
    public CommentItem(
            String commentId,
            String uid,
            String username,
            String text,
            Date createdAt,
            String parentCommentId,
            int depth,
            int score,
            boolean deleted
    ) {
        this.commentId = commentId;
        this.uid = uid;
        this.username = username;
        this.text = text;
        this.createdAt = createdAt;
        this.parentCommentId = parentCommentId == null ? "" : parentCommentId;
        this.depth = Math.max(0, depth);
        this.score = score;
        this.deleted = deleted;
    }

    /**
     * Returns the comment document ID.
     *
     * @return the comment ID
     */
    public String getCommentId() {
        return commentId;
    }

    /**
     * Returns the author user ID.
     *
     * @return the author UID
     */
    public String getUid() {
        return uid;
    }

    /**
     * Returns the display name shown for the comment author.
     *
     * @return the comment author name
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the display text for the comment.
     *
     * <p>If the comment was soft-deleted, this returns {@code [deleted]}.</p>
     *
     * @return the display text
     */
    public String getText() {
        return deleted ? "[deleted]" : text;
    }

    /**
     * Returns the original stored text.
     *
     * @return the raw comment text
     */
    public String getRawText() {
        return text;
    }

    /**
     * Returns the creation timestamp.
     *
     * @return the comment creation time, or null if unavailable
     */
    public Date getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns the parent comment ID.
     *
     * @return the parent comment ID, or an empty string for top-level comments
     */
    public String getParentCommentId() {
        return parentCommentId;
    }

    /**
     * Returns the nesting depth of the comment.
     *
     * @return the thread depth
     */
    public int getDepth() {
        return depth;
    }

    /**
     * Returns the aggregate vote score.
     *
     * @return the comment score
     */
    public int getScore() {
        return score;
    }

    /**
     * Returns whether the comment has been soft-deleted.
     *
     * @return true if deleted, otherwise false
     */
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * Returns whether the comment is a top-level comment.
     *
     * @return true if the comment has no parent, otherwise false
     */
    public boolean isTopLevel() {
        return parentCommentId == null || parentCommentId.trim().isEmpty();
    }
}