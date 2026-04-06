package com.example.eventlotterysystem;
// The following code is from OpenAI, ChatGPT, "App Dev Testing Assistance", 2026-03-30
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE;

import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * UI Tests for MyThingsActivity
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MyThingsActivityTest {

    private static final String ADMIN_EMAIL = "test@gmail.com";
    private static final String ADMIN_PASSWORD = "test123";


    /**
     * Tests that a user does not see the Admin Zone button
     * @throws Exception
     */
    @Test
    public void suspendedUserDoesNotSeeAdminZoneButton() throws Exception {
        signInAdmin();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String email = ("suspended" + timestamp + "@gmail.com").toLowerCase(Locale.US);
        String password = "test123";
        String uid = createTemporaryUserProfile(email, password, "sus" + timestamp, "Suspended User " + timestamp);

        try {
            setSuspendedByAdmin(uid);

            signInUser(email, password);
            try (ActivityScenario<MyThingsActivity> ignored = ActivityScenario.launch(MyThingsActivity.class)) {
                SystemClock.sleep(3000);
                onView(withId(R.id.adminZoneButton)).check(matches(withEffectiveVisibility(GONE)));
            }
        } finally {
            deleteTemporaryAuthAndProfile(uid, email, password);
        }
    }

    /**
     * Tests that a suspended user cannot see the Host Event Button
     * @throws Exception
     */
    @Test
    public void suspendedUserDoesNotSeeHostEventButton() throws Exception {
        signInAdmin();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String email = ("suspendedhost" + timestamp + "@gmail.com").toLowerCase(Locale.US);
        String password = "test123";
        String uid = createTemporaryUserProfile(email, password, "sushost" + timestamp, "Suspended Host " + timestamp);

        try {
            setSuspendedByAdmin(uid);

            signInUser(email, password);
            try (ActivityScenario<MyThingsActivity> ignored = ActivityScenario.launch(MyThingsActivity.class)) {
                SystemClock.sleep(3000);
                onView(withId(R.id.hostEventButton)).check(matches(withEffectiveVisibility(GONE)));
            }
        } finally {
            deleteTemporaryAuthAndProfile(uid, email, password);
        }
    }

    /**
     * signs in the shared admin account
     */
    private void signInAdmin() throws Exception {
        FirebaseAuth.getInstance().signOut();
        Tasks.await(
                FirebaseAuth.getInstance().signInWithEmailAndPassword(ADMIN_EMAIL, ADMIN_PASSWORD),
                15,
                TimeUnit.SECONDS
        );
    }

    /**
     * signs in with the supplied credentials
     * @param email
     * email of the account to sign in
     * @param password
     * password of the account to sign in
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
     * creates a temporary UserProfile used by the suspended user tests
     * @param email
     * email of the temporary user
     * @param password
     * password of the temporary user
     * @param username
     * username of the temporary user
     * @param fullName
     * full name of the temporary user
     * @return
     * the uid of the created temporary user
     */
    private String createTemporaryUserProfile(String email, String password, String username, String fullName) throws Exception {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        Tasks.await(auth.createUserWithEmailAndPassword(email, password), 15, TimeUnit.SECONDS);
        FirebaseUser created = auth.getCurrentUser();

        Map<String, Object> userPayload = new HashMap<>();
        userPayload.put("fullName", fullName);
        userPayload.put("email", email);
        userPayload.put("username", username);
        userPayload.put("usernameKey", username.toLowerCase(Locale.US));
        userPayload.put("phoneNumber", "888 888 8888");
        userPayload.put("accountType", "user");
        userPayload.put("createdAt", Timestamp.now());
        userPayload.put("pendingEmail", "");
        Tasks.await(FirebaseFirestore.getInstance().collection("users").document(created.getUid()).set(userPayload), 15, TimeUnit.SECONDS);

        return created.getUid();
    }

    /**
     * marks the supplied user as suspended through the admin account
     * @param uid
     * uid of the user to suspend
     */
    private void setSuspendedByAdmin(String uid) throws Exception {
        signInAdmin();
        Map<String, Object> updates = new HashMap<>();
        updates.put("suspended", true);
        updates.put("accountType", "user");
        Tasks.await(FirebaseFirestore.getInstance().collection("users").document(uid).update(updates), 15, TimeUnit.SECONDS);
    }

    /**
     * removes the temporary auth account and matching profile document
     * @param uid
     * uid of the temporary user
     * @param email
     * email used to sign back into the temporary user
     * @param password
     * password used to sign back into the temporary user
     */
    private void deleteTemporaryAuthAndProfile(String uid, String email, String password) throws Exception {
        FirebaseAuth.getInstance().signOut();
        Tasks.await(FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password), 15, TimeUnit.SECONDS);

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
}
