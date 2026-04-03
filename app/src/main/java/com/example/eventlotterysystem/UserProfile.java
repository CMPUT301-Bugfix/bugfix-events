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
    private boolean suspended;

    /**
     * creates the user profile object without an account type
     * @param name
     * the user's name
     * @param email
     * the user's email
     * @param username
     * the user's username
     * @param usernameKey
     * the normalized key for the username
     * @param phoneNumber
     * the user's phone number
     */
    public UserProfile(String name, String email, String username, String usernameKey, String phoneNumber) {
        this(name, email, username, usernameKey, phoneNumber, "");
    }

    /**
     * creates the user profile object
     * @param name
     * the user's name
     * @param email
     * the user's email
     * @param username
     * the user's username
     * @param usernameKey
     * the normalized key for the username
     * @param phoneNumber
     * the user's phone number
     * @param accountType
     * the user's account type
     */
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
        this.suspended = false;
    }

    /**
     * gets the user's name
     * @return
     * the user's name
     */
    public String getName() {
        return name;
    }

    /**
     * gets the user's uid
     * @return
     * the user's uid
     */
    public String getUid() {
        return uid;
    }

    /**
     * sets the user's uid
     * @param uid
     * the user's uid
     */
    public void setUid(String uid) {
        this.uid = uid;
    }

    /**
     * sets the user's name
     * @param name
     * the user's name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * gets the user's email
     * @return
     * the user's email
     */
    public String getEmail() {
        return email;
    }

    /**
     * sets the user's email
     * @param email
     * the user's email
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * gets the user's username
     * @return
     * the user's username
     */
    public String getUsername() {
        return username;
    }

    /**
     * sets the user's username
     * @param username
     * the user's username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * gets the normalized username key
     * @return
     * the normalized username key
     */
    public String getUsernameKey() {
        return usernameKey;
    }

    /**
     * sets the normalized username key
     * @param usernameKey
     * the normalized username key
     */
    public void setUsernameKey(String usernameKey) {
        this.usernameKey = usernameKey;
    }

    /**
     * gets the user's phone number
     * @return
     * the user's phone number
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * sets the user's phone number
     * @param phoneNumber
     * the user's phone number
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * gets the user's account type
     * @return
     * the user's account type
     */
    public String getAccountType() {
        return accountType;
    }

    /**
     * sets the user's account type
     * @param accountType
     * the user's account type
     */
    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    /**
     * gets the time the profile was created
     * @return
     * the time the profile was created
     */
    public Timestamp getCreatedAt() {
        return createdAt;
    }

    /**
     * sets the time the profile was created
     * @param createdAt
     * the time the profile was created
     */
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public boolean getSuspended() {
        return suspended;
    }

    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }
}
