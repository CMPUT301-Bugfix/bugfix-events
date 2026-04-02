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

    /**
     * creates an empty message thread object for Firebase
     */
    public MessageThreadItem() {
    }

    /**
     * creates a message thread object
     * @param threadId
     * the String id of the thread
     * @param userOneUid
     * the uid of the first user
     * @param userOneName
     * the name of the first user
     * @param userTwoUid
     * the uid of the second user
     * @param userTwoName
     * the name of the second user
     * @param lastMessageText
     * the preview text of the last message
     * @param lastMessageAt
     * the time of the last message
     * @param lastSenderUid
     * the uid of the last sender
     */
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

    /**
     * gets the thread id
     * @return
     * the String id of the thread
     */
    public String getThreadId() {
        return threadId;
    }

    /**
     * sets the thread id
     * @param threadId
     * the String id of the thread
     */
    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    /**
     * gets the first user uid
     * @return
     * the uid of the first user
     */
    public String getUserOneUid() {
        return userOneUid;
    }

    /**
     * sets the first user uid
     * @param userOneUid
     * the uid of the first user
     */
    public void setUserOneUid(String userOneUid) {
        this.userOneUid = userOneUid;
    }

    /**
     * gets the first user name
     * @return
     * the name of the first user
     */
    public String getUserOneName() {
        return userOneName;
    }

    /**
     * sets the first user name
     * @param userOneName
     * the name of the first user
     */
    public void setUserOneName(String userOneName) {
        this.userOneName = userOneName;
    }

    /**
     * gets the second user uid
     * @return
     * the uid of the second user
     */
    public String getUserTwoUid() {
        return userTwoUid;
    }

    /**
     * sets the second user uid
     * @param userTwoUid
     * the uid of the second user
     */
    public void setUserTwoUid(String userTwoUid) {
        this.userTwoUid = userTwoUid;
    }

    /**
     * gets the second user name
     * @return
     * the name of the second user
     */
    public String getUserTwoName() {
        return userTwoName;
    }

    /**
     * sets the second user name
     * @param userTwoName
     * the name of the second user
     */
    public void setUserTwoName(String userTwoName) {
        this.userTwoName = userTwoName;
    }

    /**
     * gets the preview text of the last message
     * @return
     * the preview text of the last message
     */
    public String getLastMessageText() {
        return lastMessageText;
    }

    /**
     * sets the preview text of the last message
     * @param lastMessageText
     * the preview text of the last message
     */
    public void setLastMessageText(String lastMessageText) {
        this.lastMessageText = lastMessageText;
    }

    /**
     * gets the time of the last message
     * @return
     * the time of the last message
     */
    public long getLastMessageAt() {
        return lastMessageAt;
    }

    /**
     * sets the time of the last message
     * @param lastMessageAt
     * the time of the last message
     */
    public void setLastMessageAt(long lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    /**
     * gets the uid of the last sender
     * @return
     * the uid of the last sender
     */
    public String getLastSenderUid() {
        return lastSenderUid;
    }

    /**
     * sets the uid of the last sender
     * @param lastSenderUid
     * the uid of the last sender
     */
    public void setLastSenderUid(String lastSenderUid) {
        this.lastSenderUid = lastSenderUid;
    }
}
