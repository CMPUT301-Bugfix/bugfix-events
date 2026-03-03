package com.example.eventlotterysystem;

public class UserProfile {
    private String name;
    private String email;
    private String username;
    private String usernameKey;
    private String phoneNumber;
    private String accountType;

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
}
