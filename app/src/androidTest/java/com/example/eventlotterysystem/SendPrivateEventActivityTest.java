package com.example.eventlotterysystem;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.os.SystemClock;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * UI tests for private-event invitation flows.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SendPrivateEventActivityTest {

    /**
     * verifies that searching for a user by phone number returns the matching profile
     */
    @Test
    public void searchByPhoneNumberTest() throws Exception {
        TestAuthHelper.ensureSharedTestUser();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String email = "phonesearch" + timestamp + "@example.com";
        String password = "test123";
        String username = "phonesearch" + timestamp;
        String fullName = "Phone Search User";
        String phoneNumber = "780 555 9876";
        String uid = createTemporaryUser(email, password, username, fullName, phoneNumber, "user");

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SendPrivateEventActivity.class);
        intent.putExtra("EVENT_ID", "phone-search-event");
        intent.putExtra("EVENT_TITLE", "Phone Search Event");

        try (ActivityScenario<SendPrivateEventActivity> ignored = ActivityScenario.launch(intent)) {
            SystemClock.sleep(2000);
            onView(withId(R.id.userSearchInput)).perform(replaceText(phoneNumber), closeSoftKeyboard());
            onView(withId(R.id.userSearchButton)).perform(click());
            SystemClock.sleep(3000);

            onView(withText(fullName)).check(matches(isDisplayed()));
        } finally {
            deleteTemporaryUser(uid, email, password);
        }
    }

    /**
     * verifies that an organizer can send a private event invitation and the entrant can accept it from the inbox
     */
    @Test
    public void acceptPrivateInvitationFlowTest() throws Exception {
        TestAuthHelper.ensureSharedTestUser();
        FirebaseUser organizer = FirebaseAuth.getInstance().getCurrentUser();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String eventTitle = "Private Accept Event " + timestamp;
        String eventId = createPrivateEvent(eventTitle, organizer.getUid());
        String email = "privateaccept" + timestamp + "@gmail.com";
        String password = "test123";
        String username = "privateaccept" + timestamp;
        String fullName = "Private Accept User " + timestamp;
        String phoneNumber = "780 555 " + timestamp.substring(Math.max(0, timestamp.length() - 4));
        String uid = createTemporaryUser(email, password, username, fullName, phoneNumber, "user");

        try {
            TestAuthHelper.ensureSharedTestUser();
            sendInviteFromOrganizerUi(eventId, eventTitle, phoneNumber, fullName);

            signInUser(email, password);
            respondToPrivateInviteFromInbox(eventTitle, true);

            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            DocumentSnapshot invitation = Tasks.await(
                    firestore.collection("events").document(eventId).collection("invitations").document(uid).get(),
                    15,
                    TimeUnit.SECONDS
            );
            DocumentSnapshot eventWaitlist = Tasks.await(
                    firestore.collection("events").document(eventId).collection("waitlist").document(uid).get(),
                    15,
                    TimeUnit.SECONDS
            );
            DocumentSnapshot userWaitlist = Tasks.await(
                    firestore.collection("users").document(uid).collection("waitlists").document(eventId).get(),
                    15,
                    TimeUnit.SECONDS
            );
            QuerySnapshot notifications = Tasks.await(
                    firestore.collection("users")
                            .document(uid)
                            .collection("notifications")
                            .whereEqualTo("eventId", eventId)
                            .whereEqualTo("type", "INVITE")
                            .get(),
                    15,
                    TimeUnit.SECONDS
            );

            assertEquals("ACCEPTED", invitation.getString("status"));
            assertTrue(eventWaitlist.exists());
            assertTrue(userWaitlist.exists());
            assertFalse(notifications.isEmpty());
            assertEquals("ACCEPTED", notifications.getDocuments().get(0).getString("status"));
        } finally {
            cleanupPrivateInviteFlow(eventId, uid, email, password);
        }
    }

    /**
     * verifies that an organizer can send a private event invitation and the entrant can reject it from the inbox
     */
    @Test
    public void rejectPrivateInvitationFlowTest() throws Exception {
        TestAuthHelper.ensureSharedTestUser();
        FirebaseUser organizer = FirebaseAuth.getInstance().getCurrentUser();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String eventTitle = "Private Reject Event " + timestamp;
        String eventId = createPrivateEvent(eventTitle, organizer.getUid());
        String email = "privatereject" + timestamp + "@gmail.com";
        String password = "test123";
        String username = "privatereject" + timestamp;
        String fullName = "Private Reject User " + timestamp;
        String phoneNumber = "587 555 " + timestamp.substring(Math.max(0, timestamp.length() - 4));
        String uid = createTemporaryUser(email, password, username, fullName, phoneNumber, "user");

        try {
            TestAuthHelper.ensureSharedTestUser();
            sendInviteFromOrganizerUi(eventId, eventTitle, phoneNumber, fullName);

            signInUser(email, password);
            respondToPrivateInviteFromInbox(eventTitle, false);

            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            DocumentSnapshot invitation = Tasks.await(
                    firestore.collection("events").document(eventId).collection("invitations").document(uid).get(),
                    15,
                    TimeUnit.SECONDS
            );
            DocumentSnapshot eventWaitlist = Tasks.await(
                    firestore.collection("events").document(eventId).collection("waitlist").document(uid).get(),
                    15,
                    TimeUnit.SECONDS
            );
            DocumentSnapshot userWaitlist = Tasks.await(
                    firestore.collection("users").document(uid).collection("waitlists").document(eventId).get(),
                    15,
                    TimeUnit.SECONDS
            );
            QuerySnapshot notifications = Tasks.await(
                    firestore.collection("users")
                            .document(uid)
                            .collection("notifications")
                            .whereEqualTo("eventId", eventId)
                            .whereEqualTo("type", "INVITE")
                            .get(),
                    15,
                    TimeUnit.SECONDS
            );

            assertEquals("REJECTED", invitation.getString("status"));
            assertFalse(eventWaitlist.exists());
            assertFalse(userWaitlist.exists());
            assertFalse(notifications.isEmpty());
            assertEquals("REJECTED", notifications.getDocuments().get(0).getString("status"));
        } finally {
            cleanupPrivateInviteFlow(eventId, uid, email, password);
        }
    }

    /**
     * sends a private invite through the organizer screen
     * @param eventId
     * id of the private event
     * @param eventTitle
     * title of the private event
     * @param searchTerm
     * exact search text for the target user
     * @param fullName
     * displayed name of the target user
     */
    private void sendInviteFromOrganizerUi(String eventId, String eventTitle, String searchTerm, String fullName) {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SendPrivateEventActivity.class);
        intent.putExtra("EVENT_ID", eventId);
        intent.putExtra("EVENT_TITLE", eventTitle);

        try (ActivityScenario<SendPrivateEventActivity> scenario = ActivityScenario.launch(intent)) {
            SystemClock.sleep(2500);
            onView(withId(R.id.userSearchInput)).perform(replaceText(searchTerm), closeSoftKeyboard());
            onView(withId(R.id.userSearchButton)).perform(click());
            SystemClock.sleep(3000);

            onView(withText(fullName)).check(matches(isDisplayed()));
            scenario.onActivity(activity -> clickRecyclerChild(
                    activity.findViewById(R.id.userSearchResultsRecyclerView),
                    0,
                    R.id.addUserToBatchButton
            ));
            onView(withId(R.id.notifyBatchButton)).perform(click());
            SystemClock.sleep(3000);
        }
    }

    /**
     * opens the private invitation from the inbox and accepts or rejects it
     * @param eventTitle
     * title used in the private invite message
     * @param accept
     * true to accept the invite, false to reject it
     */
    private void respondToPrivateInviteFromInbox(String eventTitle, boolean accept) {
        try (ActivityScenario<MyThingsActivity> scenario = ActivityScenario.launch(MyThingsActivity.class)) {
            SystemClock.sleep(5000);

            onView(withText(containsString(eventTitle))).check(matches(isDisplayed()));
            scenario.onActivity(activity -> clickRecyclerRow(
                    activity.findViewById(R.id.notificationsRecyclerView),
                    0
            ));
            SystemClock.sleep(1000);

            if (accept) {
                onView(withText("Accept")).inRoot(isDialog()).perform(click());
            } else {
                onView(withText("Reject")).inRoot(isDialog()).perform(click());
            }

            SystemClock.sleep(5000);
        }
    }

    /**
     * creates a private event document used by the invitation flow tests
     * @param title
     * title of the created event
     * @param hostUid
     * uid of the event host
     * @return
     * the created event id
     */
    private String createPrivateEvent(String title, String hostUid) throws Exception {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        String eventId = firestore.collection("events").document().getId();

        Map<String, Object> payload = new HashMap<>();
        payload.put("title", title);
        payload.put("description", "Private invitation flow test event.");
        payload.put("location", "Edmonton");
        payload.put("posterUrl", "");
        payload.put("maxEntrants", 10);
        payload.put("maxParticipants", 5);
        payload.put("totalEntrants", 0);
        payload.put("registrationDeadline", new Timestamp(new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(2))));
        payload.put("eventDate", new Timestamp(new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3))));
        payload.put("requiresGeolocation", false);
        payload.put("hostUid", hostUid);
        payload.put("hostDisplayName", "Private Invite Host");
        payload.put("waitlistOpen", true);
        payload.put("deleted", false);
        payload.put("createdAt", Timestamp.now());
        payload.put("winningMessage", "Welcome");
        payload.put("coOrganizerUids", new ArrayList<>());
        payload.put("keywords", new ArrayList<>());
        payload.put("isPublic", false);

        Tasks.await(firestore.collection("events").document(eventId).set(payload), 15, TimeUnit.SECONDS);
        return eventId;
    }

    /**
     * creates a temporary auth user and matching profile document for private invite tests
     * @param email
     * email of the temporary user
     * @param password
     * password of the temporary user
     * @param username
     * username of the temporary user
     * @param fullName
     * full name of the temporary user
     * @param phoneNumber
     * phone number of the temporary user
     * @param accountType
     * account type stored on the temporary profile
     * @return
     * uid of the temporary user
     */
    private String createTemporaryUser(
            String email,
            String password,
            String username,
            String fullName,
            String phoneNumber,
            String accountType
    ) throws Exception {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser created = Tasks.await(
                auth.createUserWithEmailAndPassword(email, password),
                15,
                TimeUnit.SECONDS
        ).getUser();

        Map<String, Object> payload = new HashMap<>();
        payload.put("fullName", fullName);
        payload.put("email", email);
        payload.put("username", username);
        payload.put("usernameKey", username.toLowerCase());
        payload.put("phoneNumber", phoneNumber);
        payload.put("accountType", accountType);
        payload.put("createdAt", Timestamp.now());
        payload.put("pendingEmail", "");
        payload.put("deleted", false);
        payload.put("optInPrivateInvites", true);
        payload.put("optInCoorganizerInvites", true);
        payload.put("optInWinningNotifications", true);
        payload.put("optInOtherNotifications", true);

        Tasks.await(
                FirebaseFirestore.getInstance().collection("users").document(created.getUid()).set(payload),
                15,
                TimeUnit.SECONDS
        );
        return created.getUid();
    }

    /**
     * removes all data created by a private invitation flow test
     * @param eventId
     * id of the private event
     * @param uid
     * uid of the invited user
     * @param email
     * email of the invited user
     * @param password
     * password of the invited user
     */
    private void cleanupPrivateInviteFlow(String eventId, String uid, String email, String password) throws Exception {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        try {
            signInUser(email, password);
            for (DocumentSnapshot doc : Tasks.await(
                    firestore.collection("users").document(uid).collection("notifications").get(),
                    15,
                    TimeUnit.SECONDS
            ).getDocuments()) {
                Tasks.await(doc.getReference().delete(), 15, TimeUnit.SECONDS);
            }
        } catch (Exception ignored) {
        }

        try {
            Tasks.await(
                    firestore.collection("users").document(uid).collection("waitlists").document(eventId).delete(),
                    15,
                    TimeUnit.SECONDS
            );
        } catch (Exception ignored) {
        }

        try {
            TestAuthHelper.ensureSharedTestUser();
            Tasks.await(
                    firestore.collection("events").document(eventId).collection("waitlist").document(uid).delete(),
                    15,
                    TimeUnit.SECONDS
            );
        } catch (Exception ignored) {
        }

        try {
            TestAuthHelper.ensureSharedTestUser();
            Tasks.await(
                    firestore.collection("events").document(eventId).collection("invitations").document(uid).delete(),
                    15,
                    TimeUnit.SECONDS
            );
        } catch (Exception ignored) {
        }

        try {
            TestAuthHelper.ensureSharedTestUser();
            Tasks.await(
                    firestore.collection("events").document(eventId).delete(),
                    15,
                    TimeUnit.SECONDS
            );
        } catch (Exception ignored) {
        }

        try {
            signInUser(email, password);
            try {
                Tasks.await(
                        firestore.collection("users").document(uid).delete(),
                        15,
                        TimeUnit.SECONDS
                );
            } catch (Exception ignored) {
            }

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                try {
                    Tasks.await(currentUser.delete(), 15, TimeUnit.SECONDS);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        } finally {
            FirebaseAuth.getInstance().signOut();
        }
    }

    /**
     * signs in with the supplied credentials
     * @param email
     * email to sign in with
     * @param password
     * password to sign in with
     */
    private void signInUser(String email, String password) throws Exception {
        FirebaseAuth.getInstance().signOut();
        Tasks.await(
                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password),
                15,
                TimeUnit.SECONDS
        );
    }

    /**
     * removes a temporary auth user and profile
     * @param uid
     * uid of the temporary user
     * @param email
     * email of the temporary user
     * @param password
     * password of the temporary user
     */
    private void deleteTemporaryUser(String uid, String email, String password) throws Exception {
        try {
            signInUser(email, password);
            try {
                Tasks.await(
                        FirebaseFirestore.getInstance().collection("users").document(uid).delete(),
                        15,
                        TimeUnit.SECONDS
                );
            } catch (Exception ignored) {
            }

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                try {
                    Tasks.await(currentUser.delete(), 15, TimeUnit.SECONDS);
                } catch (Exception ignored) {
                }
            }
        } finally {
            FirebaseAuth.getInstance().signOut();
        }
    }

    /**
     * clicks a recycler row at the requested adapter position
     * @param recyclerView
     * recycler view containing the row
     * @param position
     * adapter position to click
     */
    private void clickRecyclerRow(RecyclerView recyclerView, int position) {
        RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);
        if (holder != null) {
            holder.itemView.performClick();
        }
    }

    /**
     * clicks a child view inside a recycler row
     * @param recyclerView
     * recycler view containing the row
     * @param position
     * adapter position containing the child
     * @param viewId
     * id of the child view to click
     */
    private void clickRecyclerChild(RecyclerView recyclerView, int position, int viewId) {
        RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);
        if (holder != null) {
            android.view.View child = holder.itemView.findViewById(viewId);
            if (child != null) {
                child.performClick();
            }
        }
    }
}
