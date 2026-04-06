package com.example.eventlotterysystem;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.hasFocus;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.example.eventlotterysystem.EntrantsActivity.EVENT_ID;

import static org.hamcrest.Matchers.allOf;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

/**
 * Tests the functionality of the AdminNotificationLogActivity
 * this is the activity that shows all Notifications to Admins
 */

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AdminNotificationLogActivityTest {

    private final String NotificationTitle = "AdminTest";
    private final String NotificationContent = "For testing Admin Viewing Capability";

    /**
     * Test to see if notifications are displayed for an admin
     * @throws Exception if authentication or asynchronous setup fails
     */
    @Test
    public void viewNotificationsTest() throws Exception {
        signInTestUser();
        String notificationId = createAdminNotification();

        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                AdminNotificationsLogActivity.class
        );

        try {
            try (ActivityScenario<AdminNotificationsLogActivity> scenario = ActivityScenario.launch(intent)) {
                SystemClock.sleep(5000);
                onView(withId(R.id.adminNotificationsRecyclerView))
                        .check(matches(hasDescendant(withText(NotificationTitle))));
                onView(withId(R.id.adminNotificationsRecyclerView))
                        .check(matches(hasDescendant(withText(NotificationContent))));
            }
        } finally {
            deleteAdminNotification(notificationId);
        }
    }

    /**
     * Test to see if activity shows notifications when the Activity is navigated to
     * @throws Exception if authentication or asynchronous setup fails
     */
    @Test
    public void navigatedToAdminNotificationLogActivityTest() throws Exception {
        signInTestUser();
        String notificationId = createAdminNotification();

        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                AdminZoneActivity.class
        );
        try {
            try (ActivityScenario<AdminZoneActivity> scenario = ActivityScenario.launch(intent)) {
                SystemClock.sleep(5000);
                onView(withId(R.id.adminNotificationLogButton)).perform(click());
                SystemClock.sleep(5000);
                onView(withId(R.id.adminNotificationsRecyclerView))
                        .check(matches(hasDescendant(withText(NotificationTitle))));
                onView(withId(R.id.adminNotificationsRecyclerView))
                        .check(matches(hasDescendant(withText(NotificationContent))));
            }
        } finally {
            deleteAdminNotification(notificationId);
        }
    }

    /**
     * signs in the shared test account and ensures that remember-me is disabled
     */
    private void signInTestUser() throws Exception {
        TestAuthHelper.ensureSharedTestUser();
    }

    /**
     * creates a notification visible in the admin notification log
     */
    private String createAdminNotification() throws Exception {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Tasks.await(
                new NotificationRepository().sendToSpecificUsers(
                        "admin-log-event",
                        NotificationTitle,
                        NotificationContent,
                        "GENERAL",
                        java.util.Collections.singletonList(uid)
                ),
                15,
                TimeUnit.SECONDS
        );
        return uid;
    }

    /**
     * deletes a seeded admin notification
     * @param notificationId
     * uid of the user whose seeded notification documents should be removed
     */
    private void deleteAdminNotification(String notificationId) throws Exception {
        try {
            for (com.google.firebase.firestore.DocumentSnapshot doc : Tasks.await(
                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(notificationId)
                            .collection("notifications")
                            .whereEqualTo("title", NotificationTitle)
                            .whereEqualTo("message", NotificationContent)
                            .get(),
                    15,
                    TimeUnit.SECONDS
            ).getDocuments()) {
                Tasks.await(doc.getReference().delete(), 15, TimeUnit.SECONDS);
            }
        } catch (Exception ignored) {
        }
    }
}
