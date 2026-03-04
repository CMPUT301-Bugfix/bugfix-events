package com.example.eventlotterysystem;


public class UserProfile {

    public enum AccountType { USER, ORGANIZER, ADMIN }

    private final String id;
    private final String fullName;
    private final AccountType accountType;
    private final String summary;

    public UserProfile(String id, String fullName, AccountType accountType, String summary) {
        this.id = id;
        this.fullName = fullName;
        this.accountType = accountType;
        this.summary = summary;
    }

    public String getId() { return id; }

    public String getFullName() { return fullName; }

    public AccountType getAccountType() { return accountType; }

    public String getSummary() { return summary; }

    public static AccountType parseAccountType(String raw) {
        if (raw == null) return AccountType.USER;
        String v = raw.trim().toUpperCase();
        try {
            return AccountType.valueOf(v);
        } catch (Exception ignored) {
            return AccountType.USER;
        }
    }
}
