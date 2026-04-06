package com.example.eventlotterysystem;

/**
 * Represents a single chat message within a thread.
 */
public class ChatMessageItem {
    private String messageId;
    private String senderUid;
    private String senderName;
    private String text;
    private long sentAt;

    /**
     * creates an empty chat message object for Firebase
     */
    public ChatMessageItem() {
    }

    /**
     * creates a chat message object
     * @param messageId
     * the String id of the message
     * @param senderUid
     * the uid of the user who sent the message
     * @param senderName
     * the name of the user who sent the message
     * @param text
     * the text content of the message
     * @param sentAt
     * the time the message was sent
     */
    public ChatMessageItem(
            String messageId,
            String senderUid,
            String senderName,
            String text,
            long sentAt
    ) {
        this.messageId = messageId;
        this.senderUid = senderUid;
        this.senderName = senderName;
        this.text = text;
        this.sentAt = sentAt;
    }

    /**
     * gets the message id
     * @return
     * the String id of the message
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * sets the message id
     * @param messageId
     * the String id of the message
     */
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    /**
     * gets the sender uid
     * @return
     * the uid of the user who sent the message
     */
    public String getSenderUid() {
        return senderUid;
    }

    /**
     * sets the sender uid
     * @param senderUid
     * the uid of the user who sent the message
     */
    public void setSenderUid(String senderUid) {
        this.senderUid = senderUid;
    }

    /**
     * gets the sender name
     * @return
     * the name of the user who sent the message
     */
    public String getSenderName() {
        return senderName;
    }

    /**
     * sets the sender name
     * @param senderName
     * the name of the user who sent the message
     */
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    /**
     * gets the message text
     * @return
     * the text content of the message
     */
    public String getText() {
        return text;
    }

    /**
     * sets the message text
     * @param text
     * the text content of the message
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * gets the time the message was sent
     * @return
     * the time the message was sent
     */
    public long getSentAt() {
        return sentAt;
    }

    /**
     * sets the time the message was sent
     * @param sentAt
     * the time the message was sent
     */
    public void setSentAt(long sentAt) {
        this.sentAt = sentAt;
    }
}
