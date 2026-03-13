package com.example.eventlotterysystem;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.SystemClock;
import android.text.TextUtils;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

/**
 * Tests the functionality of SignupActivity
 * this activity allows an entrant to provide profile information and create an account
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SignupActivityTest {

    /**
     * Test to see if a user profile object is created when an account creation is attempted
     */
    @Test
    public void createUserTest() throws Exception {
        signOutSession();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String email = "test" + timestamp + "@gmail.com";
        String username = "testsignup" + timestamp;
        String password = "test123";
        String createdUid = "";

        try {
            try (ActivityScenario<SignupActivity> ignored = ActivityScenario.launch(SignupActivity.class)) {
                fillSignupForm("Signup Test", email, username, "888 888 8888", password);
                onView(withId(R.id.createAccountButton)).perform(scrollTo(), click());
                SystemClock.sleep(7000);

                onView(withId(R.id.myThingsButton)).check(matches(isDisplayed()));
            }

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            assertTrue(currentUser != null);
            assertEquals(email, currentUser.getEmail());
            createdUid = currentUser.getUid();
        } finally {
            deleteCreatedUser(email, password, createdUid);
        }
    }

    /**
     * Test to ensure that an account is not created when the user input is incorrect
     */
    @Test
    public void FormNotFilledTest() throws Exception {
        signOutSession();

        try (ActivityScenario<SignupActivity> ignored = ActivityScenario.launch(SignupActivity.class)) {
            onView(withId(R.id.createAccountButton)).perform(scrollTo(), click());

            onView(withId(R.id.createNameInput)).check(matches(hasErrorText("This field is required.")));
        }
    }


    /**
     * Test to see if the User profile object is added to the database on account creation
     */
    @Test
    public void updateDatabaseTest() throws Exception {
        signOutSession();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String email = "test" + timestamp + "@gmail.com";
        String username = "testdb" + timestamp;
        String password = "test123";
        String createdUid = "";

        try {
            try (ActivityScenario<SignupActivity> ignored = ActivityScenario.launch(SignupActivity.class)) {
                fillSignupForm("Database Test", email, username, "888 888 8888", password);
                onView(withId(R.id.createAccountButton)).perform(scrollTo(), click());
                SystemClock.sleep(7000);
            }

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            assertTrue(currentUser != null);
            createdUid = currentUser.getUid();
            DocumentSnapshot userDoc = Tasks.await(FirebaseFirestore.getInstance().collection("users").document(createdUid).get(), 15, TimeUnit.SECONDS);
            assertTrue(userDoc.exists());
            assertEquals("Database Test", userDoc.getString("fullName"));
            assertEquals(username, userDoc.getString("username"));
            assertEquals("888 888 8888", userDoc.getString("phoneNumber"));
        } finally {
            deleteCreatedUser(email, password, createdUid);
        }
    }

    /**
     * fills the signup form with the provided account information
     * @param name
     * name value for the new account
     * @param email
     * email value for the new account
     * @param username
     * username value for the new account
     * @param phoneNumber
     * optional phone number for the new account
     * @param password
     * password for the new account
     */
    private void fillSignupForm(String name, String email, String username, String phoneNumber, String password) {
        onView(withId(R.id.createNameInput)).perform(replaceText(name), closeSoftKeyboard());
        onView(withId(R.id.createEmailInput)).perform(replaceText(email), closeSoftKeyboard());
        onView(withId(R.id.createUsernameInput)).perform(replaceText(username), closeSoftKeyboard());
        onView(withId(R.id.createPhoneInput)).perform(replaceText(phoneNumber), closeSoftKeyboard());
        onView(withId(R.id.createPasswordInput)).perform(replaceText(password), closeSoftKeyboard());
        onView(withId(R.id.createConfirmPasswordInput)).perform(replaceText(password), closeSoftKeyboard());
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
     * removes the created auth user and profile documents after the test run
     * @param email
     * email of the created account
     * @param password
     * password of the created account
     * @param uid
     * uid of the created account
     */
    private void deleteCreatedUser(String email, String password, String uid) throws Exception {
        try {
            Tasks.await(FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password), 15, TimeUnit.SECONDS);
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (!TextUtils.isEmpty(uid)) {
                try {
                    Tasks.await(FirebaseFirestore.getInstance().collection("users").document(uid).delete(), 15, TimeUnit.SECONDS);
                } catch (Exception ignored) {
                }
            }
            if (currentUser != null) {
                Tasks.await(currentUser.delete(), 15, TimeUnit.SECONDS);
            }
        } catch (Exception ignored) {
        } finally {
            FirebaseAuth.getInstance().signOut();
        }
    }
}
