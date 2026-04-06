package com.example.eventlotterysystem;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import android.content.Context;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Tests the functionality of the SettingsActivity
 * this activity allows a signed-in user to update their profile information
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SettingsActivityTest {

    /**
     * test to see if a user can update their name and phone number on the profile
     */
    @Test
    public void updateNameAndPhoneTest() throws Exception {
        signInTestUser();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        DocumentSnapshot originalProfile = Tasks.await(FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid()).get(), 15, TimeUnit.SECONDS);
        String originalName = originalProfile.getString("fullName") == null ? "" : originalProfile.getString("fullName");
        String originalPhone = originalProfile.getString("phoneNumber") == null ? "" : originalProfile.getString("phoneNumber");
        String updatedName = "Update Test";
        String updatedPhone = "888 888 8888";

        try {
            try (ActivityScenario<SettingsActivity> ignored = ActivityScenario.launch(SettingsActivity.class)) {
                SystemClock.sleep(4000);

                onView(withId(R.id.updateInformationHeader)).perform(click());
                onView(withId(R.id.updateNameInput)).check(matches(isDisplayed()));
                onView(withId(R.id.updateNameInput)).perform(replaceText(updatedName), closeSoftKeyboard());
                onView(withId(R.id.updatePhoneInput)).perform(replaceText(updatedPhone), closeSoftKeyboard());
                onView(withId(R.id.updateInformationContentScroll)).perform(swipeUp(), swipeUp());
                onView(withId(R.id.saveProfileChangesButton)).perform(click());
                SystemClock.sleep(4000);

                onView(withId(R.id.updateNameInput)).check(matches(withText(updatedName)));
                onView(withId(R.id.updatePhoneInput)).check(matches(withText(updatedPhone)));
            }

            DocumentSnapshot updatedProfile = Tasks.await(FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid()).get(), 15, TimeUnit.SECONDS);
            assertEquals(updatedName, updatedProfile.getString("fullName"));
            assertEquals(updatedPhone, updatedProfile.getString("phoneNumber"));
        } finally {
            restoreProfile(currentUser.getUid(), originalName, originalPhone);
        }
    }

    /**
     * test to see if a user can delete their profile if they no longer wish to use the app
     */
    @Test
    public void deleteAccountTest() throws Exception {
        signOutSession();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String email = "testdelete" + timestamp + "@gmail.com";
        String username = "testdelete" + timestamp;
        String password = "test123";
        String uid = createTemporaryUser(email, password, username, "Delete Test");

        try {
            try (ActivityScenario<SettingsActivity> ignored = ActivityScenario.launch(SettingsActivity.class)) {
                SystemClock.sleep(4000);

                onView(withId(R.id.deleteAccountButton)).perform(click());
                onView(withHint(R.string.current_password)).perform(replaceText(password), closeSoftKeyboard());
                onView(withText(R.string.delete_account_confirm_action)).perform(click());
                SystemClock.sleep(5000);

                onView(withId(R.id.menuLoginButton)).check(matches(isDisplayed()));
            }

            assertNull(FirebaseAuth.getInstance().getCurrentUser());
            try {
                Tasks.await(FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password), 10, TimeUnit.SECONDS);
                fail("Deleted account should not be able to sign in.");
            } catch (Exception expected) {
            }
        } finally {
            cleanupTemporaryUser(email, password, username, uid);
        }
    }

    /**
     * test to see if a user can opt out of notification categories and have the
     * preferences persisted on their profile
     */
    @Test
    public void saveNotificationPreferencesTest() throws Exception {
        signInTestUser();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        DocumentSnapshot originalProfile = Tasks.await(
                FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid()).get(),
                15,
                TimeUnit.SECONDS
        );

        boolean originalCoorganizer = !originalProfile.contains("optInCoorganizerInvites")
                || Boolean.TRUE.equals(originalProfile.getBoolean("optInCoorganizerInvites"));
        boolean originalPrivate = !originalProfile.contains("optInPrivateInvites")
                || Boolean.TRUE.equals(originalProfile.getBoolean("optInPrivateInvites"));
        boolean originalWinning = !originalProfile.contains("optInWinningNotifications")
                || Boolean.TRUE.equals(originalProfile.getBoolean("optInWinningNotifications"));
        boolean originalOther = !originalProfile.contains("optInOtherNotifications")
                || Boolean.TRUE.equals(originalProfile.getBoolean("optInOtherNotifications"));

        try {
            try (ActivityScenario<SettingsActivity> ignored = ActivityScenario.launch(SettingsActivity.class)) {
                SystemClock.sleep(4000);

                onView(withId(R.id.notificationPreferencesHeader)).perform(click());
                onView(withId(R.id.optInCoorganizerInvitesSwitch)).perform(click());
                onView(withId(R.id.optInPrivateInvitesSwitch)).perform(click());
                onView(withId(R.id.optInWinningNotificationsSwitch)).perform(click());
                onView(withId(R.id.optInOtherNotificationsSwitch)).perform(click());
                onView(withId(R.id.saveNotificationPreferencesButton)).perform(click());
                SystemClock.sleep(4000);
            }

            DocumentSnapshot updatedProfile = Tasks.await(
                    FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid()).get(),
                    15,
                    TimeUnit.SECONDS
            );

            assertEquals(Boolean.FALSE, updatedProfile.getBoolean("optInCoorganizerInvites"));
            assertEquals(Boolean.FALSE, updatedProfile.getBoolean("optInPrivateInvites"));
            assertEquals(Boolean.FALSE, updatedProfile.getBoolean("optInWinningNotifications"));
            assertEquals(Boolean.FALSE, updatedProfile.getBoolean("optInOtherNotifications"));
        } finally {
            restoreNotificationPreferences(
                    currentUser.getUid(),
                    originalCoorganizer,
                    originalPrivate,
                    originalWinning,
                    originalOther
            );
        }
    }

    /**
     * signs in the shared test account and ensures that remember-me is disabled
     */
    private void signInTestUser() throws Exception {
        TestAuthHelper.ensureSharedTestUser();
    }

    /**
     * restores the shared test account profile fields after the test run
     * @param uid
     * document id of the user to restore
     * @param fullName
     * original name value
     * @param phoneNumber
     * original phone value
     */
    private void restoreProfile(String uid, String fullName, String phoneNumber) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("fullName", fullName);
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            payload.put("phoneNumber", FieldValue.delete());
        } else {
            payload.put("phoneNumber", phoneNumber);
        }
        Tasks.await(FirebaseFirestore.getInstance().collection("users").document(uid).set(payload, SetOptions.merge()), 15, TimeUnit.SECONDS);
    }

    /**
     * restores the saved notification preferences for the shared test account
     * @param uid
     * uid of the shared test account
     * @param optInCoorganizerInvites
     * original coorganizer invitation preference
     * @param optInPrivateInvites
     * original private invitation preference
     * @param optInWinningNotifications
     * original winning notification preference
     * @param optInOtherNotifications
     * original other notification preference
     */
    private void restoreNotificationPreferences(
            String uid,
            boolean optInCoorganizerInvites,
            boolean optInPrivateInvites,
            boolean optInWinningNotifications,
            boolean optInOtherNotifications
    ) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("optInCoorganizerInvites", optInCoorganizerInvites);
        payload.put("optInPrivateInvites", optInPrivateInvites);
        payload.put("optInWinningNotifications", optInWinningNotifications);
        payload.put("optInOtherNotifications", optInOtherNotifications);
        Tasks.await(
                FirebaseFirestore.getInstance().collection("users").document(uid).set(payload, SetOptions.merge()),
                15,
                TimeUnit.SECONDS
        );
    }

    /**
     * clears the authenticated session and remember-me state before a test run
     */
    private void signOutSession() {
        FirebaseAuth.getInstance().signOut();
        Context context = ApplicationProvider.getApplicationContext();
        AuthSessionPreference.setRemember(context, false);
    }

    /**
     * creates a temporary signed-in user and the matching profile records for delete-account testing
     * @param email
     * email of the temporary account
     * @param password
     * password of the temporary account
     * @param username
     * username of the temporary account
     * @param fullName
     * profile name of the temporary account
     * @return
     * uid of the created account
     */
    private String createTemporaryUser(String email, String password, String username, String fullName) throws Exception {
        FirebaseUser user = Tasks.await(FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password), 15, TimeUnit.SECONDS).getUser();
        String uid = user.getUid();

        Map<String, Object> profilePayload = new HashMap<>();
        profilePayload.put("fullName", fullName);
        profilePayload.put("email", email);
        profilePayload.put("username", username);
        profilePayload.put("usernameKey", username.toLowerCase());
        profilePayload.put("accountType", "user");

        Map<String, Object> usernamePayload = new HashMap<>();
        usernamePayload.put("uid", uid);
        usernamePayload.put("email", email);

        Tasks.await(FirebaseFirestore.getInstance().collection("users").document(uid).set(profilePayload), 15, TimeUnit.SECONDS);
        Tasks.await(FirebaseFirestore.getInstance().collection("usernames").document(username.toLowerCase()).set(usernamePayload), 15, TimeUnit.SECONDS);
        return uid;
    }

    /**
     * removes any temporary account data left behind if the delete flow fails
     * @param email
     * email of the temporary account
     * @param password
     * password of the temporary account
     * @param username
     * username of the temporary account
     * @param uid
     * uid of the temporary account
     */
    private void cleanupTemporaryUser(String email, String password, String username, String uid) throws Exception {
        try {
            FirebaseAuth.getInstance().signOut();
            Tasks.await(FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password), 10, TimeUnit.SECONDS);
            try {
                Tasks.await(FirebaseFirestore.getInstance().collection("users").document(uid).delete(), 10, TimeUnit.SECONDS);
            } catch (Exception ignored) {
            }
            try {
                Tasks.await(FirebaseFirestore.getInstance().collection("usernames").document(username.toLowerCase()).delete(), 10, TimeUnit.SECONDS);
            } catch (Exception ignored) {
            }
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                Tasks.await(currentUser.delete(), 10, TimeUnit.SECONDS);
            }
        } catch (Exception ignored) {
        } finally {
            FirebaseAuth.getInstance().signOut();
        }
    }
}
