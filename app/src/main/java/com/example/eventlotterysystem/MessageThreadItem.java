package com.example.eventlotterysystem;

/**
 * Represents a single direct message thread between two users.
 */
public class MessageThreadItem {
    private String threadId;
    private String userOneUid;
    private String userOneName;
    private String userTwoUid;
    private String userTwoName;
    private String lastMessageText;
    private long lastMessageAt;
    private String lastSenderUid;

    public MessageThreadItem() {
    }

    public MessageThreadItem(
            String threadId,
            String userOneUid,
            String userOneName,
            String userTwoUid,
            String userTwoName,
            String lastMessageText,
            long lastMessageAt,
            String lastSenderUid
    ) {
        this.threadId = threadId;
        this.userOneUid = userOneUid;
        this.userOneName = userOneName;
        this.userTwoUid = userTwoUid;
        this.userTwoName = userTwoName;
        this.lastMessageText = lastMessageText;
        this.lastMessageAt = lastMessageAt;
        this.lastSenderUid = lastSenderUid;
    }

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public String getUserOneUid() {
        return userOneUid;
    }

    public void setUserOneUid(String userOneUid) {
        this.userOneUid = userOneUid;
    }

    public String getUserOneName() {
        return userOneName;
    }

    public void setUserOneName(String userOneName) {
        this.userOneName = userOneName;
    }

    public String getUserTwoUid() {
        return userTwoUid;
    }

    public void setUserTwoUid(String userTwoUid) {
        this.userTwoUid = userTwoUid;
    }

    public String getUserTwoName() {
        return userTwoName;
    }

    public void setUserTwoName(String userTwoName) {
        this.userTwoName = userTwoName;
    }

    public String getLastMessageText() {
        return lastMessageText;
    }

    public void setLastMessageText(String lastMessageText) {
        this.lastMessageText = lastMessageText;
    }

    public long getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(long lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public String getLastSenderUid() {
        return lastSenderUid;
    }

    public void setLastSenderUid(String lastSenderUid) {
        this.lastSenderUid = lastSenderUid;
    }
}
