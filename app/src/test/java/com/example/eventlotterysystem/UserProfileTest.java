package com.example.eventlotterysystem;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for the {@link UserProfile} model.
 */
public class UserProfileTest {


    /**
     * Verifies that the constructor stores the supplied field values.
     */
    @Test
    public void testCreateUser() {
        UserProfile profile = new UserProfile(
                "Somebody",
                "Somebody@gmail.com",
                "SomeUser",
                "someuser",
                "123-456-7890",
                "admin"
        );

        assertEquals("Somebody", profile.getName());
        assertEquals("Somebody@gmail.com", profile.getEmail());
        assertEquals("SomeUser", profile.getUsername());
        assertEquals("someuser", profile.getUsernameKey());
        assertEquals("123-456-7890", profile.getPhoneNumber());
        assertEquals("admin", profile.getAccountType());
    }

    /**
     * Verifies that the mutable profile fields can be updated through setters.
     */
    @Test
    public void testSetFields() {
        UserProfile profile = new UserProfile("Hey", "Hey", "Hey", "Hey", "Hey", "user");

        profile.setUid("uid123");
        profile.setName("New Test");
        profile.setEmail("newtest@gmail.com");
        profile.setUsername("TestUser");
        profile.setUsernameKey("testuser");
        profile.setPhoneNumber("123-456-7890");
        profile.setAccountType("organizer");

        assertEquals("uid123", profile.getUid());
        assertEquals("New Test", profile.getName());
        assertEquals("newtest@gmail.com", profile.getEmail());
        assertEquals("TestUser", profile.getUsername());
        assertEquals("testuser", profile.getUsernameKey());
        assertEquals("123-456-7890", profile.getPhoneNumber());
        assertEquals("organizer", profile.getAccountType());
    }

}
