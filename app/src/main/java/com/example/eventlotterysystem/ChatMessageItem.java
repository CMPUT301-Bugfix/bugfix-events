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

    public ChatMessageItem() {
    }

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

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderUid() {
        return senderUid;
    }

    public void setSenderUid(String senderUid) {
        this.senderUid = senderUid;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getSentAt() {
        return sentAt;
    }

    public void setSentAt(long sentAt) {
        this.sentAt = sentAt;
    }
}
