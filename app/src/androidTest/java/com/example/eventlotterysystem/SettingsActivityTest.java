package com.example.eventlotterysystem;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;

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
                onView(withId(R.id.updateInformationContent)).check(matches(isDisplayed()));
                onView(withId(R.id.updateNameInput)).perform(scrollTo(), replaceText(updatedName), closeSoftKeyboard());
                onView(withId(R.id.updatePhoneInput)).perform(scrollTo(), replaceText(updatedPhone), closeSoftKeyboard());
                onView(withId(R.id.saveProfileChangesButton)).perform(scrollTo(), click());
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
     * signs in the shared test account and ensures that remember-me is disabled
     */
    private void signInTestUser() throws Exception {
        FirebaseAuth.getInstance().signOut();
        Context context = ApplicationProvider.getApplicationContext();
        AuthSessionPreference.setRemember(context, false);
        Tasks.await(FirebaseAuth.getInstance().signInWithEmailAndPassword("test@gmail.com", "test123"), 15, TimeUnit.SECONDS);
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
}
