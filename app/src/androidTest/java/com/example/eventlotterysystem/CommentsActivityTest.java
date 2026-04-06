package com.example.eventlotterysystem;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anything;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * UI tests for comment stories.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class CommentsActivityTest {

    /**
     * verifies that an entrant can post a comment on an event
     */
    @Test
    public void entrantCanPostCommentTest() throws Exception {
        FirebaseAuth.getInstance().signOut();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String email = "commentuser" + timestamp + "@gmail.com";
        String password = "test123";
        String uid = createTemporaryUser(email, password, "commentuser" + timestamp, "Comment User " + timestamp);
        String eventId = createCommentTestEvent("Comment Posting Event " + timestamp, "comment-host");
        String commentText = "This is a UI comment " + timestamp;

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), CommentsActivity.class);
        intent.putExtra(CommentsActivity.EVENT_ID, eventId);

        try (ActivityScenario<CommentsActivity> ignored = ActivityScenario.launch(intent)) {
            SystemClock.sleep(3000);
            onView(withId(R.id.commentsInput)).perform(replaceText(commentText), closeSoftKeyboard());
            onView(withId(R.id.commentsPostButton)).perform(click());
            SystemClock.sleep(4000);

            onView(withText(commentText)).check(matches(isDisplayed()));

            boolean found = false;
            com.google.firebase.firestore.QuerySnapshot snapshot = Tasks.await(
                    FirebaseFirestore.getInstance().collection("events").document(eventId).collection("comments").get(),
                    15,
                    TimeUnit.SECONDS
            );
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                if (commentText.equals(doc.getString("text")) && uid.equals(doc.getString("uid"))) {
                    found = true;
                    break;
                }
            }
            assertTrue(found);
        } finally {
            deleteCommentTestData(eventId);
            deleteTemporaryUser(uid, email, password);
        }
    }

    /**
     * verifies that an admin can moderate and soft-delete an event comment
     */
    @Test
    public void adminCanDeleteCommentTest() throws Exception {
        TestAuthHelper.ensureSharedTestUser();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String eventId = createCommentTestEvent("Admin Comment Moderation Event " + timestamp, "external-host");
        String email = "commentmoderation" + timestamp + "@gmail.com";
        String password = "test123";
        String username = "commentmoderation" + timestamp;
        String commenterUid = createTemporaryUser(email, password, username, "Entrant User");
        String commentId = createComment(eventId, commenterUid, "Entrant User", "Needs moderation " + timestamp);

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), CommentsActivity.class);
        intent.putExtra(CommentsActivity.EVENT_ID, eventId);

        try {
            TestAuthHelper.ensureSharedTestUser();
            try (ActivityScenario<CommentsActivity> ignored = ActivityScenario.launch(intent)) {
                SystemClock.sleep(3000);

                onData(anything())
                        .inAdapterView(withId(R.id.commentsListView))
                        .atPosition(0)
                        .perform(longClick());

                onView(withText(R.string.comments_delete_action)).perform(click());
                SystemClock.sleep(3000);

                DocumentSnapshot commentDoc = Tasks.await(
                        FirebaseFirestore.getInstance()
                                .collection("events")
                                .document(eventId)
                                .collection("comments")
                                .document(commentId)
                                .get(),
                        15,
                        TimeUnit.SECONDS
                );

                assertEquals("[deleted]", commentDoc.getString("text"));
                assertEquals(Boolean.TRUE, commentDoc.getBoolean("deleted"));
            }
        } finally {
            deleteCommentTestData(eventId);
            deleteTemporaryUser(commenterUid, email, password);
        }
    }

    /**
     * creates a temporary User for the comment posting test
     * @param email
     * email of the temporary User
     * @param password
     * password of the temporary User
     * @param username
     * username of the temporary User
     * @param fullName
     * full name of the temporary User
     * @return
     * the uid of the created User
     */
    private String createTemporaryUser(String email, String password, String username, String fullName) throws Exception {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser created = Tasks.await(
                auth.createUserWithEmailAndPassword(email, password),
                15,
                TimeUnit.SECONDS
        ).getUser();

        Map<String, Object> userPayload = new HashMap<>();
        userPayload.put("fullName", fullName);
        userPayload.put("email", email);
        userPayload.put("username", username);
        userPayload.put("usernameKey", username.toLowerCase());
        userPayload.put("phoneNumber", "780 555 0000");
        userPayload.put("accountType", "user");
        userPayload.put("createdAt", Timestamp.now());
        userPayload.put("pendingEmail", "");
        Tasks.await(
                FirebaseFirestore.getInstance().collection("users").document(created.getUid()).set(userPayload),
                15,
                TimeUnit.SECONDS
        );

        return created.getUid();
    }

    /**
     * removes the temporary auth user and profile created for the comment test
     * @param uid
     * uid of the temporary User
     * @param email
     * email used to sign back in
     * @param password
     * password used to sign back in
     */
    private void deleteTemporaryUser(String uid, String email, String password) throws Exception {
        FirebaseAuth.getInstance().signOut();
        Tasks.await(
                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password),
                15,
                TimeUnit.SECONDS
        );

        try {
            Tasks.await(FirebaseFirestore.getInstance().collection("users").document(uid).delete(), 15, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            try {
                Tasks.await(user.delete(), 15, TimeUnit.SECONDS);
            } catch (Exception ignored) {
            }
        }

        FirebaseAuth.getInstance().signOut();
    }

    /**
     * creates a public Event document used for the comment tests
     * @param title
     * title of the Event
     * @param hostUid
     * uid stored as the Event host
     * @return
     * the created Event id
     */
    private String createCommentTestEvent(String title, String hostUid) throws Exception {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        String eventId = firestore.collection("events").document().getId();

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("title", title);
        eventPayload.put("description", "Comment story test event.");
        eventPayload.put("location", "Edmonton");
        eventPayload.put("posterUrl", "");
        eventPayload.put("maxEntrants", 10);
        eventPayload.put("maxParticipants", 5);
        eventPayload.put("totalEntrants", 0);
        eventPayload.put("registrationDeadline", new Timestamp(new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1))));
        eventPayload.put("eventDate", new Timestamp(new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(2))));
        eventPayload.put("requiresGeolocation", false);
        eventPayload.put("hostUid", hostUid);
        eventPayload.put("hostDisplayName", "Comment Host");
        eventPayload.put("waitlistOpen", true);
        eventPayload.put("deleted", false);
        eventPayload.put("createdAt", Timestamp.now());
        eventPayload.put("winningMessage", "Comment event");
        eventPayload.put("isPublic", true);

        Tasks.await(firestore.collection("events").document(eventId).set(eventPayload), 15, TimeUnit.SECONDS);
        return eventId;
    }

    /**
     * creates a Comment document under an Event
     * @param eventId
     * id of the Event receiving the Comment
     * @param uid
     * uid of the user who authored the Comment
     * @param username
     * display name stored on the Comment
     * @param text
     * body text of the Comment
     * @return
     * the created Comment id
     */
    private String createComment(String eventId, String uid, String username, String text) throws Exception {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || !uid.equals(currentUser.getUid())) {
            throw new IllegalStateException("Comment author must be signed in before seeding a comment");
        }
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        String commentId = firestore.collection("events").document(eventId).collection("comments").document().getId();

        Map<String, Object> payload = new HashMap<>();
        payload.put("uid", uid);
        payload.put("text", text);
        payload.put("username", username);
        payload.put("createdAt", Timestamp.now());
        payload.put("parentCommentId", "");
        payload.put("depth", 0);
        payload.put("score", 0);
        payload.put("deleted", false);

        Tasks.await(
                firestore.collection("events").document(eventId).collection("comments").document(commentId).set(payload),
                15,
                TimeUnit.SECONDS
        );
        return commentId;
    }

    /**
     * removes the seeded Comments and Event document created for the comment test
     * @param eventId
     * id of the Event to delete
     */
    private void deleteCommentTestData(String eventId) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        try {
            com.google.firebase.firestore.QuerySnapshot snapshot = Tasks.await(
                    firestore.collection("events").document(eventId).collection("comments").get(),
                    15,
                    TimeUnit.SECONDS
            );
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                Tasks.await(doc.getReference().delete(), 15, TimeUnit.SECONDS);
            }
        } catch (Exception ignored) {
        }

        try {
            Tasks.await(firestore.collection("events").document(eventId).delete(), 15, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }
}
