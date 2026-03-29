package com.example.eventlotterysystem;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.firebase.auth.FirebaseAuth;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the functionality of LoginActivity
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class LoginActivityTest {
    /**
     * Test to see correct Username and Password connects to the correct user and navigates them to the home screen
     */
    @Test
    public void loginByUsernameTest() {
        FirebaseAuth.getInstance().signOut();
        Context context = ApplicationProvider.getApplicationContext();
        AuthSessionPreference.setRemember(context, false);

        try (ActivityScenario<LoginActivity> ignored = ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.loginIdentifierInput))
                    .perform(replaceText("test"), closeSoftKeyboard());
            onView(withId(R.id.loginPasswordInput))
                    .perform(replaceText("test123"), closeSoftKeyboard());
            onView(withId(R.id.loginButton)).perform(click());

            SystemClock.sleep(4000);

            onView(withId(R.id.myThingsButton)).check(matches(isDisplayed()));
        }
    }

    /**
     * Test to see correct Email and Password connects to the correct user and navigates them to the home screen
     */
    @Test
    public void loginByEmailTest() {
        FirebaseAuth.getInstance().signOut();
        Context context = ApplicationProvider.getApplicationContext();
        AuthSessionPreference.setRemember(context, false);

        try (ActivityScenario<LoginActivity> ignored = ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.loginIdentifierInput))
                    .perform(replaceText("test@gmail.com"), closeSoftKeyboard());
            onView(withId(R.id.loginPasswordInput))
                    .perform(replaceText("test123"), closeSoftKeyboard());
            onView(withId(R.id.loginButton)).perform(click());

            SystemClock.sleep(4000);

            onView(withId(R.id.myThingsButton)).check(matches(isDisplayed()));
        }
    }

    /**
     * Test to see if incorrect credentials notifies user that the attempt failed
     */
    @Test
    public void loginWithIncorrectUsernameTest() {
        FirebaseAuth.getInstance().signOut();
        Context context = ApplicationProvider.getApplicationContext();
        AuthSessionPreference.setRemember(context, false);

        try (ActivityScenario<LoginActivity> ignored = ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.loginIdentifierInput))
                    .perform(replaceText("not_a_real_test_user"), closeSoftKeyboard());
            onView(withId(R.id.loginPasswordInput))
                    .perform(replaceText("test123"), closeSoftKeyboard());
            onView(withId(R.id.loginButton)).perform(click());

            SystemClock.sleep(4000);

            onView(withId(R.id.loginButton)).check(matches(isDisplayed()));
            assertNull(FirebaseAuth.getInstance().getCurrentUser());
        }
    }

    /**
     * Test to see if incorrect Password notifies user that the password is incorrect
     */
    @Test
    public void loginWithIncorrectPasswordTest() {
        FirebaseAuth.getInstance().signOut();
        Context context = ApplicationProvider.getApplicationContext();
        AuthSessionPreference.setRemember(context, false);

        try (ActivityScenario<LoginActivity> ignored = ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.loginIdentifierInput))
                    .perform(replaceText("test"), closeSoftKeyboard());
            onView(withId(R.id.loginPasswordInput))
                    .perform(replaceText("wrongPassword123"), closeSoftKeyboard());
            onView(withId(R.id.loginButton)).perform(click());

            SystemClock.sleep(4000);

            onView(withId(R.id.loginButton)).check(matches(isDisplayed()));
            assertNull(FirebaseAuth.getInstance().getCurrentUser());
        }
    }

    /**
     * Test to see if remember me keeps the user signed in when the app is opened again
     */
    @Test
    public void rememberMeTest() {
        FirebaseAuth.getInstance().signOut();
        Context context = ApplicationProvider.getApplicationContext();
        AuthSessionPreference.setRemember(context, false);

        try (ActivityScenario<LoginActivity> ignored = ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.loginIdentifierInput))
                    .perform(replaceText("test"), closeSoftKeyboard());
            onView(withId(R.id.loginPasswordInput))
                    .perform(replaceText("test123"), closeSoftKeyboard());
            onView(withId(R.id.rememberMeCheckBox)).perform(click());
            onView(withId(R.id.loginButton)).perform(click());

            SystemClock.sleep(4000);

            onView(withId(R.id.myThingsButton)).check(matches(isDisplayed()));
        }

        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            SystemClock.sleep(2000);
            onView(withId(R.id.myThingsButton)).check(matches(isDisplayed()));
        }

        FirebaseAuth.getInstance().signOut();
        AuthSessionPreference.setRemember(context, false);
    }

    /**
     * Test to see if leaving remember me unchecked returns the user to authentication when the app is opened again
     */
    @Test
    public void rememberMeUncheckedTest() {
        FirebaseAuth.getInstance().signOut();
        Context context = ApplicationProvider.getApplicationContext();
        AuthSessionPreference.setRemember(context, false);

        try (ActivityScenario<LoginActivity> ignored = ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.loginIdentifierInput))
                    .perform(replaceText("test"), closeSoftKeyboard());
            onView(withId(R.id.loginPasswordInput))
                    .perform(replaceText("test123"), closeSoftKeyboard());
            onView(withId(R.id.loginButton)).perform(click());

            SystemClock.sleep(4000);

            onView(withId(R.id.myThingsButton)).check(matches(isDisplayed()));
        }

        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            SystemClock.sleep(2000);
            onView(withId(R.id.menuLoginButton)).check(matches(isDisplayed()));
        }

        FirebaseAuth.getInstance().signOut();
        AuthSessionPreference.setRemember(context, false);
    }
}
