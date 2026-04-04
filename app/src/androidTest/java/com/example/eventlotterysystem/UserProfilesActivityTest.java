package com.example.eventlotterysystem;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Tests the functionality of the UserProfilesActivity
 * this is the activity that lets admins browse user profiles
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class UserProfilesActivityTest {

    /**
     * Test if the admin can browse user profiles
     */
    @Test
    public void browseProfilesTest() throws Exception {
        signInTestUser();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String email = "test" + timestamp + "@gmail.com";
        String password = "test123";
        String fullName = "Admin Browse Test " + timestamp;
        String username = "browse" + timestamp;
        String uid = createTemporaryProfileUser(email, password, fullName, username, "entrant");
        signInTestUser();

        try {
            try (ActivityScenario<UserProfilesActivity> ignored = ActivityScenario.launch(UserProfilesActivity.class)) {
                SystemClock.sleep(5000);

                onView(withText("Name: " + fullName)).check(matches(isDisplayed()));
                onView(Matchers.allOf(
                        withId(R.id.profileTypeValue),
                        withText("Account Type: entrant"),
                        withParent(hasDescendant(withText("Name: " + fullName)))
                ))
                        .check(matches(isDisplayed()));
            }
        } finally {
            deleteTemporaryProfileUser(uid, email, password);
        }
    }

    /**
     * signs in the shared test account and ensures that remember-me is disabled
     */
    private void signInTestUser() throws Exception {
        TestAuthHelper.ensureSharedTestUser();
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
    private String createTemporaryProfileUser(String email, String password, String fullName, String username, String accountType) throws Exception {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        Tasks.await(auth.createUserWithEmailAndPassword(email, password), 15, TimeUnit.SECONDS);
        FirebaseUser currentUser = auth.getCurrentUser();

        Map<String, Object> userPayload = new HashMap<>();
        userPayload.put("fullName", fullName);
        userPayload.put("username", username);
        userPayload.put("email", email);
        userPayload.put("phoneNumber", "888 888 8888");
        userPayload.put("accountType", accountType);
        userPayload.put("createdAt", Timestamp.now());
        userPayload.put("deleted", false);

        Tasks.await(FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid()).set(userPayload), 15, TimeUnit.SECONDS);
        return currentUser.getUid();
    }

    /**
     * removes the temporary user profile document created for an admin profile test
     * @param uid
     * uid of the profile document to remove
     */
    private void deleteTemporaryProfileUser(String uid, String email, String password) throws Exception {
        FirebaseAuth.getInstance().signOut();
        Tasks.await(FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password), 15, TimeUnit.SECONDS);
        Tasks.await(FirebaseFirestore.getInstance().collection("users").document(uid).delete(), 15, TimeUnit.SECONDS);
        Tasks.await(FirebaseAuth.getInstance().getCurrentUser().delete(), 15, TimeUnit.SECONDS);
    }
}
