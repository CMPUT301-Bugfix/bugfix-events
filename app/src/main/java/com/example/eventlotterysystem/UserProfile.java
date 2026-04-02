package com.example.eventlotterysystem;

import com.google.firebase.Timestamp;

/**
 * This class represents a Users profile and has all the getters and setters
 * associated with all of the necessary fields for each user.
 */

public class UserProfile {
    private String uid;
    private String name;
    private String email;
    private String username;
    private String usernameKey;
    private String phoneNumber;
    private String accountType;
    private Timestamp createdAt;

    // Notification preferences
    private boolean optInCoorganizerInvites = true;
    private boolean optInPrivateInvites = true;
    private boolean optInWinningNotifications = true;
    private boolean optInOtherNotifications = true;

    public UserProfile() {}

    public UserProfile(String name, String email, String username, String usernameKey, String phoneNumber) {
        this(name, email, username, usernameKey, phoneNumber, "");
    }

    public UserProfile(
            String name,
            String email,
            String username,
            String usernameKey,
            String phoneNumber,
            String accountType
    ) {
        this.name = name;
        this.email = email;
        this.username = username;
        this.usernameKey = usernameKey;
        this.phoneNumber = phoneNumber;
        this.accountType = accountType;
    }

    public String getName() {
        return name;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsernameKey() {
        return usernameKey;
    }

    public void setUsernameKey(String usernameKey) {
        this.usernameKey = usernameKey;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isOptInCoorganizerInvites() {
        return optInCoorganizerInvites;
    }

    public void setOptInCoorganizerInvites(boolean optInCoorganizerInvites) {
        this.optInCoorganizerInvites = optInCoorganizerInvites;
    }

    public boolean isOptInPrivateInvites() {
        return optInPrivateInvites;
    }

    public void setOptInPrivateInvites(boolean optInPrivateInvites) {
        this.optInPrivateInvites = optInPrivateInvites;
    }

    public boolean isOptInWinningNotifications() {
        return optInWinningNotifications;
    }

    public void setOptInWinningNotifications(boolean optInWinningNotifications) {
        this.optInWinningNotifications = optInWinningNotifications;
    }

    public boolean isOptInOtherNotifications() {
        return optInOtherNotifications;
    }

    public void setOptInOtherNotifications(boolean optInOtherNotifications) {
        this.optInOtherNotifications = optInOtherNotifications;
    }
}
