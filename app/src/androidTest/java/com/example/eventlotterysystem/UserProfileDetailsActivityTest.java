package com.example.eventlotterysystem;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * UI tests for {@link UserProfileDetailsActivity}.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class UserProfileDetailsActivityTest {

    @Rule
    public ActivityScenarioRule<UserProfileDetailsActivity> scenario =
            new ActivityScenarioRule<>(UserProfileDetailsActivity.class);


    /**
     * Verifies that the user profile detail views are visible.
     */
    @Test
    public void testUserProfileDetailsViewsDisplayed() {
        onView(withId(R.id.userProfileDetailsBackButton)).check(matches(isDisplayed()));
        onView(withId(R.id.userProfileDetailsNameValue)).check(matches(isDisplayed()));
        onView(withId(R.id.userProfileDetailsTypeValue)).check(matches(isDisplayed()));
        onView(withId(R.id.userProfileDetailsUsernameValue)).check(matches(isDisplayed()));
        onView(withId(R.id.userProfileDetailsEmailValue)).check(matches(isDisplayed()));
        onView(withId(R.id.userProfileDetailsPhoneValue)).check(matches(isDisplayed()));
        onView(withId(R.id.userProfileDetailsJoinDateValue)).check(matches(isDisplayed()));
    }

    /**
     * Verifies that pressing the back button closes the screen.
     */
    @Test
    public void testBackButtonClosesActivity() {
        onView(withId(R.id.userProfileDetailsBackButton)).perform(click());
    }

    /**
     * Test if the host can assign an entrant as a co-organizer for an Event
     * and remove them from the entrant pool for that Event
     */
    @Test
    public void assignCoorganizerTest() throws Exception {
        signInTestUser();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String eventId = createCoorganizerTestEvent(currentUser.getUid(), "Edmonton Coorganizer Event " + timestamp);
        String targetEmail = "test" + timestamp + "@gmail.com";
        String targetPassword = "test123";
        String targetUsername = "testco" + timestamp;
        String targetUid = createTemporaryCoorganizerUser(targetEmail, targetPassword, targetUsername, "Coorganizer Test");
        createCoorganizerWaitlistEntry(eventId, targetUid);
        signInTestUser();

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), UserProfileDetailsActivity.class);
        intent.putExtra(UserProfileDetailsActivity.NAME, "Coorganizer Test");
        intent.putExtra(UserProfileDetailsActivity.ACCOUNT_TYPE, "entrant");
        intent.putExtra(UserProfileDetailsActivity.USERNAME, targetUsername);
        intent.putExtra(UserProfileDetailsActivity.EMAIL, targetEmail);
        intent.putExtra(UserProfileDetailsActivity.PHONE, "888 888 8888");
        intent.putExtra(UserProfileDetailsActivity.UID, targetUid);
        intent.putExtra(UserProfileDetailsActivity.TIME_MILLIS, System.currentTimeMillis());
        intent.putExtra(UserProfileDetailsActivity.ALLOW_DELETE, true);
        intent.putExtra(UserProfileDetailsActivity.EVENT_ID, eventId);

        try {
            try (ActivityScenario<UserProfileDetailsActivity> ignored = ActivityScenario.launch(intent)) {
                SystemClock.sleep(4000);

                onView(withId(R.id.userProfileAssignCoorganizerButton)).check(matches(isDisplayed()));
                onView(withId(R.id.userProfileAssignCoorganizerButton)).perform(click());
                onView(withText(R.string.assign_coorganizer_action)).perform(click());

                SystemClock.sleep(4000);
            }

            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            DocumentSnapshot eventSnapshot = Tasks.await(firestore.collection("events").document(eventId).get(), 15, TimeUnit.SECONDS);
            DocumentSnapshot eventWaitlist = Tasks.await(firestore.collection("events").document(eventId).collection("waitlist").document(targetUid).get(), 15, TimeUnit.SECONDS);
            DocumentSnapshot userWaitlist = Tasks.await(firestore.collection("users").document(targetUid).collection("waitlists").document(eventId).get(), 15, TimeUnit.SECONDS);

            assertTrue(eventSnapshot.exists());
            assertTrue(eventSnapshot.contains("coOrganizerUids"));
            assertTrue(((ArrayList<?>) eventSnapshot.get("coOrganizerUids")).contains(targetUid));
            assertFalse(eventWaitlist.exists());
            assertFalse(userWaitlist.exists());
        } finally {
            deleteCoorganizerTestData(eventId, targetUid, targetEmail, targetPassword);
        }
    }

    /**
     * Test if the admin can remove a user profile
     */
    @Test
    public void deleteProfileTest() throws Exception {
        signInTestUser();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String password = "test123";
        String fullName = "Admin Delete Test " + timestamp;
        String username = "delete" + timestamp;
        String email = "test" + timestamp + "@gmail.com";
        String uid = createTemporaryCoorganizerUser(email, password, username, fullName);
        signInTestUser();

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), UserProfileDetailsActivity.class);
        intent.putExtra(UserProfileDetailsActivity.NAME, fullName);
        intent.putExtra(UserProfileDetailsActivity.ACCOUNT_TYPE, "entrant");
        intent.putExtra(UserProfileDetailsActivity.USERNAME, username);
        intent.putExtra(UserProfileDetailsActivity.EMAIL, email);
        intent.putExtra(UserProfileDetailsActivity.PHONE, "888 888 8888");
        intent.putExtra(UserProfileDetailsActivity.UID, uid);
        intent.putExtra(UserProfileDetailsActivity.TIME_MILLIS, System.currentTimeMillis());
        intent.putExtra(UserProfileDetailsActivity.ALLOW_DELETE, true);

        try {
            try (ActivityScenario<UserProfileDetailsActivity> ignored = ActivityScenario.launch(intent)) {
                SystemClock.sleep(3000);

                onView(withId(R.id.userProfileDeleteButton)).check(matches(isDisplayed()));
                onView(withId(R.id.userProfileDeleteButton)).perform(click());
                onView(withText(R.string.admin_delete_profile_confirm_action)).perform(click());

                SystemClock.sleep(3000);
            }

            DocumentSnapshot snapshot = Tasks.await(FirebaseFirestore.getInstance().collection("users").document(uid).get(), 15, TimeUnit.SECONDS);
            assertFalse(snapshot.exists());
        } finally {
            deleteTemporaryProfileUser(uid, email, password);
        }
    }

    /**
     * Test if the admin can remove organizer privileges and suspend the organizer
     */
    @Test
    public void removeOrganizerPrivilegesTest() throws Exception {
        signInTestUser();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String password = "test123";
        String fullName = "Organizer Remove Test " + timestamp;
        String username = "organizer" + timestamp;
        String email = "organizer" + timestamp + "@gmail.com";
        String uid = createTemporaryProfileUser(email, password, username, fullName, "organizer");
        signInTestUser();

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), UserProfileDetailsActivity.class);
        intent.putExtra(UserProfileDetailsActivity.NAME, fullName);
        intent.putExtra(UserProfileDetailsActivity.ACCOUNT_TYPE, "organizer");
        intent.putExtra(UserProfileDetailsActivity.USERNAME, username);
        intent.putExtra(UserProfileDetailsActivity.EMAIL, email);
        intent.putExtra(UserProfileDetailsActivity.PHONE, "888 888 8888");
        intent.putExtra(UserProfileDetailsActivity.UID, uid);
        intent.putExtra(UserProfileDetailsActivity.TIME_MILLIS, System.currentTimeMillis());
        intent.putExtra(UserProfileDetailsActivity.ALLOW_DELETE, true);

        try {
            try (ActivityScenario<UserProfileDetailsActivity> ignored = ActivityScenario.launch(intent)) {
                SystemClock.sleep(3000);

                onView(withId(R.id.userProfileRemoveOrganizerButton)).check(matches(isDisplayed()));
                onView(withId(R.id.userProfileRemoveOrganizerButton)).perform(click());
                onView(withText(R.string.admin_remove_organizer_confirm_action)).perform(click());

                SystemClock.sleep(3000);
            }

            DocumentSnapshot snapshot = Tasks.await(
                    FirebaseFirestore.getInstance().collection("users").document(uid).get(),
                    15,
                    TimeUnit.SECONDS
            );
            assertTrue(snapshot.exists());
            assertEquals("user", snapshot.getString("accountType"));
            assertEquals(Boolean.TRUE, snapshot.getBoolean("suspended"));
        } finally {
            deleteTemporaryProfileUser(uid, email, password);
        }
    }

    /**
     * Test if adding Coorganizer Creates a notification to the correct User
     */
    @Test
    public void sendNotificationsTest() throws Exception {
        signInTestUser();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String eventId = createCoorganizerTestEvent(currentUser.getUid(), "Edmonton Coorganizer Event " + timestamp);
        String targetEmail = "test" + timestamp + "@gmail.com";
        String targetPassword = "test123";
        String targetUsername = "testco" + timestamp;
        String targetUid = createTemporaryCoorganizerUser(targetEmail, targetPassword, targetUsername, "Coorganizer Test");
        createCoorganizerWaitlistEntry(eventId, targetUid);
        signInTestUser();

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), UserProfileDetailsActivity.class);
        intent.putExtra(UserProfileDetailsActivity.NAME, "Coorganizer Test");
        intent.putExtra(UserProfileDetailsActivity.ACCOUNT_TYPE, "entrant");
        intent.putExtra(UserProfileDetailsActivity.USERNAME, targetUsername);
        intent.putExtra(UserProfileDetailsActivity.EMAIL, targetEmail);
        intent.putExtra(UserProfileDetailsActivity.PHONE, "888 888 8888");
        intent.putExtra(UserProfileDetailsActivity.UID, targetUid);
        intent.putExtra(UserProfileDetailsActivity.TIME_MILLIS, System.currentTimeMillis());
        intent.putExtra(UserProfileDetailsActivity.ALLOW_DELETE, true);
        intent.putExtra(UserProfileDetailsActivity.EVENT_ID, eventId);

        try {
            try (ActivityScenario<UserProfileDetailsActivity> ignored = ActivityScenario.launch(intent)) {
                SystemClock.sleep(4000);

                onView(withId(R.id.userProfileAssignCoorganizerButton)).check(matches(isDisplayed()));
                onView(withId(R.id.userProfileAssignCoorganizerButton)).perform(click());
                onView(withText(R.string.assign_coorganizer_action)).perform(click());

                SystemClock.sleep(4000);
            }

            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            DocumentSnapshot eventSnapshot = Tasks.await(firestore.collection("events").document(eventId).get(), 15, TimeUnit.SECONDS);
            QuerySnapshot eventNotificationQuery = Tasks.await(firestore.collection("events").document(eventId).collection("coorganizerinvites").whereEqualTo("uid", targetUid).get(), 15, TimeUnit.SECONDS);
            DocumentSnapshot eventNotification = eventNotificationQuery.getDocuments().get(0);

            assertTrue(eventNotification.exists());
            assertEquals(eventNotification.getString("uid"), targetUid);

        } finally {
            deleteCoorganizerTestData(eventId, targetUid, targetEmail, targetPassword);
        }
    }

    /**
     * signs in the shared test account and ensures that remember-me is disabled
     */
    private void signInTestUser() throws Exception {
        TestAuthHelper.ensureSharedTestUser();
    }

    /**
     * creates an Event document for the co-organizer assignment test
     * @param hostUid
     * uid of the host managing the Event
     * @param title
     * title of the Event that should be created
     * @return
     * the document id of the created Event
     */
    private String createCoorganizerTestEvent(String hostUid, String title) throws Exception {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        String eventId = firestore.collection("events").document().getId();

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("title", title);
        eventPayload.put("description", "Coorganizer assignment test in Edmonton.");
        eventPayload.put("location", "SUB Edmonton");
        eventPayload.put("posterUrl", "");
        eventPayload.put("maxEntrants", 10);
        eventPayload.put("maxParticipants", 5);
        eventPayload.put("totalEntrants", 1);
        eventPayload.put("registrationDeadline", new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3)));
        eventPayload.put("eventDate", new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)));
        eventPayload.put("requiresGeolocation", false);
        eventPayload.put("hostUid", hostUid);
        eventPayload.put("hostDisplayName", "Update Test");
        eventPayload.put("waitlistOpen", true);
        eventPayload.put("deleted", false);
        eventPayload.put("createdAt", Timestamp.now());
        eventPayload.put("winningMessage", "Welcome to the Edmonton event.");
        eventPayload.put("coOrganizerUids", new ArrayList<>());
        eventPayload.put("keywords", new ArrayList<>());
        eventPayload.put("isPublic", true);

        Tasks.await(firestore.collection("events").document(eventId).set(eventPayload), 15, TimeUnit.SECONDS);
        return eventId;
    }

    /**
     * creates the temporary viewed entrant account that will be assigned as a co-organizer
     * @param email
     * email stored on the viewed profile
     * @param password
     * password used for the temporary auth account
     * @param username
     * username stored on the viewed profile
     * @param fullName
     * name stored on the viewed profile
     * @return
     * uid of the created temporary user
     */
    private String createTemporaryCoorganizerUser(String email, String password, String username, String fullName) throws Exception {
        return createTemporaryProfileUser(email, password, username, fullName, "entrant");
    }

    /**
     * creates a temporary auth user with the requested account type
     * @param email
     * email stored on the viewed profile
     * @param password
     * password used for the temporary auth account
     * @param username
     * username stored on the viewed profile
     * @param fullName
     * name stored on the viewed profile
     * @param accountType
     * account type stored on the viewed profile
     * @return
     * uid of the created temporary user
     */
    private String createTemporaryProfileUser(
            String email,
            String password,
            String username,
            String fullName,
            String accountType
    ) throws Exception {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        Tasks.await(auth.createUserWithEmailAndPassword(email, password), 15, TimeUnit.SECONDS);
        FirebaseUser currentUser = auth.getCurrentUser();

        Map<String, Object> userPayload = new HashMap<>();
        userPayload.put("username", username);
        userPayload.put("fullName", fullName);
        userPayload.put("email", email);
        userPayload.put("phoneNumber", "888 888 8888");
        userPayload.put("accountType", accountType);
        userPayload.put("createdAt", Timestamp.now());
        userPayload.put("deleted", false);

        Tasks.await(FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid()).set(userPayload), 15, TimeUnit.SECONDS);
        return currentUser.getUid();
    }

    /**
     * creates a temporary user profile document for an admin profile test
     * @param uid
     * uid of the profile document
     * @param fullName
     * name stored on the profile
     * @param username
     * username stored on the profile
     * @param email
     * email stored on the profile
     * @param accountType
     * account type stored on the profile
     */
    private void createProfileTestUser(String uid, String fullName, String username, String email, String accountType) throws Exception {
        Map<String, Object> userPayload = new HashMap<>();
        userPayload.put("fullName", fullName);
        userPayload.put("username", username);
        userPayload.put("email", email);
        userPayload.put("phoneNumber", "888 888 8888");
        userPayload.put("accountType", accountType);
        userPayload.put("createdAt", Timestamp.now());
        userPayload.put("deleted", false);

        Tasks.await(FirebaseFirestore.getInstance().collection("users").document(uid).set(userPayload), 15, TimeUnit.SECONDS);
    }

    /**
     * removes the temporary auth account created for an admin profile deletion test
     * @param uid
     * uid of the temporary viewed user
     * @param email
     * email of the temporary viewed user
     * @param password
     * password of the temporary viewed user
     */
    private void deleteTemporaryProfileUser(String uid, String email, String password) throws Exception {
        FirebaseAuth.getInstance().signOut();
        Tasks.await(FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password), 15, TimeUnit.SECONDS);
        try {
            Tasks.await(FirebaseFirestore.getInstance().collection("users").document(uid).delete(), 15, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
        Tasks.await(FirebaseAuth.getInstance().getCurrentUser().delete(), 15, TimeUnit.SECONDS);
    }

    /**
     * creates matching waitlist entries for the target user and Event
     * @param eventId
     * id of the Event the target user joined
     * @param targetUid
     * uid of the entrant being assigned
     */
    private void createCoorganizerWaitlistEntry(String eventId, String targetUid) throws Exception {
        Map<String, Object> waitlistPayload = new HashMap<>();
        waitlistPayload.put("eventId", eventId);
        waitlistPayload.put("uid", targetUid);
        waitlistPayload.put("status", EventRepository.WAITLIST_STATUS_IN);
        waitlistPayload.put("joinedAt", Timestamp.now());

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        Tasks.await(firestore.collection("events").document(eventId).collection("waitlist").document(targetUid).set(waitlistPayload), 15, TimeUnit.SECONDS);
        Tasks.await(firestore.collection("users").document(targetUid).collection("waitlists").document(eventId).set(waitlistPayload), 15, TimeUnit.SECONDS);
    }

    /**
     * removes the Event and viewed user documents created for the co-organizer assignment test
     * @param eventId
     * id of the Event to remove
     * @param targetUid
     * uid of the viewed user to remove
     * @param targetEmail
     * email used for the temporary viewed account
     * @param targetPassword
     * password used for the temporary viewed account
     */
    private void deleteCoorganizerTestData(String eventId, String targetUid, String targetEmail, String targetPassword) throws Exception {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        signInTestUser();
        Tasks.await(firestore.collection("users").document(targetUid).collection("waitlists").document(eventId).delete(), 15, TimeUnit.SECONDS);
        Tasks.await(firestore.collection("events").document(eventId).collection("waitlist").document(targetUid).delete(), 15, TimeUnit.SECONDS);
        Tasks.await(firestore.collection("events").document(eventId).delete(), 15, TimeUnit.SECONDS);

        FirebaseAuth.getInstance().signOut();
        Tasks.await(FirebaseAuth.getInstance().signInWithEmailAndPassword(targetEmail, targetPassword), 15, TimeUnit.SECONDS);
        Tasks.await(firestore.collection("users").document(targetUid).delete(), 15, TimeUnit.SECONDS);
        Tasks.await(FirebaseAuth.getInstance().getCurrentUser().delete(), 15, TimeUnit.SECONDS);
    }
}
