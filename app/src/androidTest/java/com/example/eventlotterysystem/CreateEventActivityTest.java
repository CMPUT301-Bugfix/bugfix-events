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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import android.view.View;
import android.widget.ImageView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Tests the functionality of the CreateEventActivity
 * this activity allows an organiser to create a new event or edit one that already exists
 * it also verifies that event data is saved correctly in the database
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class CreateEventActivityTest {


    /**
     * test to see if an event object is created when a user creates an event
     * @throws Exception if authentication, UI setup, or Firestore operations fail
     */
    @Test
    public void createEventTest() throws Exception {
        signInTestUser();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String title = "UofA Study Night " + timestamp;
        deleteEventsByTitle(title);

        try (ActivityScenario<CreateEventActivity> ignored = ActivityScenario.launch(CreateEventActivity.class)) {
            fillRequiredFields(title, "Exam prep session for U of A students in Edmonton.", "CAB Edmonton", "5");
            pickToday(R.id.createEventDeadlineButton);
            pickToday(R.id.createEventDateButton);

            onView(withId(R.id.createEventSubmitButton)).perform(scrollTo(), click());
            SystemClock.sleep(5000);
        }

        try (ActivityScenario<HostedEventsActivity> ignored = ActivityScenario.launch(HostedEventsActivity.class)) {
            SystemClock.sleep(4000);
            onView(withText(title)).check(matches(isDisplayed()));
        }

        deleteEventsByTitle(title);
    }

    /**
     * test to see if event creation is prevented when the input fields are missing information
     * @throws Exception if authentication or UI setup fails
     */
    @Test
    public void inputFieldsValidTest() throws Exception {
        signInTestUser();

        try (ActivityScenario<CreateEventActivity> ignored = ActivityScenario.launch(CreateEventActivity.class)) {
            onView(withId(R.id.createEventSubmitButton)).perform(scrollTo(), click());

            onView(withId(R.id.createEventTitleInput))
                    .check(matches(hasErrorText("This field is required.")));
            onView(withId(R.id.createEventDescriptionInput))
                    .check(matches(hasErrorText("This field is required.")));
            onView(withId(R.id.createEventLocationInput))
                    .check(matches(hasErrorText("This field is required.")));
            onView(withId(R.id.createEventDeadlineValue))
                    .check(matches(withText(R.string.registration_deadline_required)));
            onView(withId(R.id.createEventDateValue))
                    .check(matches(withText(R.string.event_date_required)));
        }
    }

    /**
     * test to see if event created is uploaded to the database
     * @throws Exception if authentication, UI setup, or Firestore operations fail
     */
    @Test
    public void dataBaseUpdatedTest() throws Exception {
        signInTestUser();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String title = "UofA SUB Event " + timestamp;
        deleteEventsByTitle(title);

        try (ActivityScenario<CreateEventActivity> ignored = ActivityScenario.launch(CreateEventActivity.class)) {
            fillRequiredFields(title, "Evening social for University of Alberta students in Edmonton.", "SUB Edmonton", "7");
            pickToday(R.id.createEventDeadlineButton);
            pickToday(R.id.createEventDateButton);

            onView(withId(R.id.createEventSubmitButton)).perform(scrollTo(), click());
            SystemClock.sleep(5000);
        }

        DocumentSnapshot event = findEventByTitle(title);
        assertNotNull(event);
        assertTrue(event.exists());
        assertEquals(title, event.getString("title"));
        assertEquals("SUB Edmonton", event.getString("location"));
        deleteEventsByTitle(title);
    }

    /**
     * test to see if an event created with a Image is uploads the link to the image and stores the image on the database
     * @throws Exception if authentication, image setup, or Firestore operations fail
     */
    @Test
    public void ImageUploadedTest() throws Exception {
        signInTestUser();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String title = "UofA Poster Event " + timestamp;
        deleteEventsByTitle(title);
        Uri posterUri = createPosterUri("athabasca_hall.jpg");

        try (ActivityScenario<CreateEventActivity> scenario = ActivityScenario.launch(CreateEventActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    java.lang.reflect.Field field = CreateEventActivity.class.getDeclaredField("selectedPosterUri");
                    field.setAccessible(true);
                    field.set(activity, posterUri);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                activity.findViewById(R.id.createEventPosterPreview).setVisibility(android.view.View.VISIBLE);
                ((ImageView) activity.findViewById(R.id.createEventPosterPreview)).setImageURI(posterUri);
                ((android.widget.TextView) activity.findViewById(R.id.createEventPosterStatus))
                        .setText(R.string.poster_selected);
            });

            onView(withId(R.id.createEventPosterPreview)).check(matches(isDisplayed()));
            onView(withId(R.id.createEventPosterPreview)).check(matches(posterPreviewLoaded()));
            onView(withId(R.id.createEventPosterStatus)).check(matches(withText(R.string.poster_selected)));
            fillRequiredFields(title, "Poster test for an Edmonton campus event.", "SUB Edmonton", "6");
            pickToday(R.id.createEventDeadlineButton);
            pickToday(R.id.createEventDateButton);

            onView(withId(R.id.createEventSubmitButton)).perform(scrollTo(), click());
            SystemClock.sleep(7000);
        }

        DocumentSnapshot event = findEventByTitle(title);
        assertNotNull(event);
        assertTrue(event.exists());
        String posterUrl = event.getString("posterUrl");
        assertNotNull(posterUrl);
        assertFalse(posterUrl.trim().isEmpty());
        deleteEventsByTitle(title);
    }

    /**
     * test to see if an existing event poster can be updated in edit mode
     * @throws Exception if authentication, image setup, or Firestore operations fail
     */
    @Test
    public void PosterUpdatedTest() throws Exception {
        signInTestUser();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String title = "UofA Poster Update Event " + timestamp;
        deleteEventsByTitle(title);
        String eventId = createHostedTestEvent(title, "Poster update test for Edmonton campus event.", "CCIS Edmonton", createPosterUri("athabasca_hall.jpg"));
        String originalPosterUrl = Tasks.await(FirebaseFirestore.getInstance().collection("events").document(eventId).get(), 15, TimeUnit.SECONDS).getString("posterUrl");

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), CreateEventActivity.class);
        intent.putExtra("EVENT_ID", eventId);
        Uri updatedPosterUri = createPosterUri("ccis.jpg");

        try (ActivityScenario<CreateEventActivity> scenario = ActivityScenario.launch(intent)) {
            SystemClock.sleep(4000);

            scenario.onActivity(activity -> {
                try {
                    java.lang.reflect.Field field = CreateEventActivity.class.getDeclaredField("selectedPosterUri");
                    field.setAccessible(true);
                    field.set(activity, updatedPosterUri);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                activity.findViewById(R.id.createEventPosterPreview).setVisibility(android.view.View.VISIBLE);
                ((ImageView) activity.findViewById(R.id.createEventPosterPreview)).setImageURI(updatedPosterUri);
                ((android.widget.TextView) activity.findViewById(R.id.createEventPosterStatus)).setText(R.string.poster_selected);
            });

            onView(withId(R.id.createEventPosterPreview)).check(matches(isDisplayed()));
            onView(withId(R.id.createEventPosterPreview)).check(matches(posterPreviewLoaded()));
            onView(withId(R.id.createEventPosterStatus)).check(matches(withText(R.string.poster_selected)));
            onView(withId(R.id.createEventSubmitButton)).perform(scrollTo(), click());
            SystemClock.sleep(7000);
        }

        DocumentSnapshot event = Tasks.await(FirebaseFirestore.getInstance().collection("events").document(eventId).get(), 15, TimeUnit.SECONDS);
        String updatedPosterUrl = event.getString("posterUrl");
        assertNotNull(originalPosterUrl);
        assertNotNull(updatedPosterUrl);
        assertFalse(updatedPosterUrl.trim().isEmpty());
        assertFalse(updatedPosterUrl.equals(originalPosterUrl));
        deleteEvent(eventId);
    }

    /**
     * test to see that the event information is populated when an activity is opened with an event to be edited
     */
    @Test
    public void EventEditOpenedTest() throws Exception {
        signInTestUser();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String title = "UofA Edit Event " + timestamp;
        deleteEventsByTitle(title);
        String eventId = createHostedTestEvent(title, "Original U of A event description.", "ECHA Edmonton");

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), CreateEventActivity.class);
        intent.putExtra("EVENT_ID", eventId);

        try (ActivityScenario<CreateEventActivity> ignored = ActivityScenario.launch(intent)) {
            SystemClock.sleep(4000);

            onView(withId(R.id.createEventTitle)).check(matches(withText(R.string.edit_event_title)));
            onView(withId(R.id.createEventSubmitButton)).check(matches(withText(R.string.edit_event_submit)));
            onView(withId(R.id.createEventTitleInput)).check(matches(withText(title)));
            onView(withId(R.id.createEventLocationInput)).check(matches(withText("ECHA Edmonton")));
        }

        deleteEvent(eventId);
    }

    /**
     * test to see if the edits made to the event actually change the the Event object and is saved to the database
     */
    @Test
    public void EventEditedTest() throws Exception {
        signInTestUser();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String originalTitle = "Edmonton Edit Source " + timestamp;
        deleteEventsByTitle(originalTitle);
        deleteEventsByTitle(originalTitle + " Updated");
        String updatedTitle = originalTitle + " Updated";
        String eventId = createHostedTestEvent(originalTitle, "Before edit at U of A.", "Tory Building Edmonton");

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), CreateEventActivity.class);
        intent.putExtra("EVENT_ID", eventId);

        try (ActivityScenario<CreateEventActivity> ignored = ActivityScenario.launch(intent)) {
            SystemClock.sleep(4000);

            onView(withId(R.id.createEventTitleInput))
                    .perform(replaceText(updatedTitle), closeSoftKeyboard());
            onView(withId(R.id.createEventLocationInput))
                    .perform(replaceText("CCIS Edmonton"), closeSoftKeyboard());
            onView(withId(R.id.createEventDescriptionInput))
                    .perform(replaceText("After edit for a University of Alberta event."), closeSoftKeyboard());
            onView(withId(R.id.createEventSubmitButton)).perform(scrollTo(), click());

            SystemClock.sleep(5000);
        }

        DocumentSnapshot event = Tasks.await(FirebaseFirestore.getInstance().collection("events").document(eventId).get(), 15, TimeUnit.SECONDS);
        assertEquals(updatedTitle, event.getString("title"));
        assertEquals("CCIS Edmonton", event.getString("location"));
        assertEquals("After edit for a University of Alberta event.", event.getString("description"));
        deleteEvent(eventId);
    }

    /**
     * fills the mandatory event fields on the screen with the provided values
     * @param title
     * title to be entered for the event
     * @param description
     * description to be entered for the event
     * @param location
     * location to be entered for the event
     * @param maxParticipants
     * maximum number of participants to be entered for the event
     */
    private void fillRequiredFields(String title, String description, String location, String maxParticipants) {
        onView(withId(R.id.createEventTitleInput))
                .perform(replaceText(title), closeSoftKeyboard());
        onView(withId(R.id.createEventDescriptionInput))
                .perform(replaceText(description), closeSoftKeyboard());
        onView(withId(R.id.createEventLocationInput))
                .perform(replaceText(location), closeSoftKeyboard());
        onView(withId(R.id.createEventMaxParticipantsInput))
                .perform(scrollTo(), replaceText(maxParticipants), closeSoftKeyboard());
    }

    /**
     * opens the date picker for the supplied button and confirms today's date
     * @param buttonId
     * the id of the date button that should be pressed
     */
    private void pickToday(int buttonId) {
        onView(withId(buttonId)).perform(scrollTo(), click());
        onView(withText(android.R.string.ok)).perform(click());
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
     * creates a hosted event directly in the database for edit mode tests
     * @param title
     * title of the test event
     * @param description
     * description of the test event
     * @param location
     * location of the test event
     * @return
     * the document id of the created event
     */
    private String createHostedTestEvent(String title, String description, String location) throws Exception {
        return createHostedTestEvent(title, description, location, null);
    }

    /**
     * creates a hosted event directly in the database for edit mode tests
     * @param title
     * title of the test event
     * @param description
     * description of the test event
     * @param location
     * location of the test event
     * @param posterUri
     * optional local poster uri to upload with the event
     * @return
     * the document id of the created event
     */
    private String createHostedTestEvent(String title, String description, String location, Uri posterUri) throws Exception {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        EventItem event = new EventItem("", title, description, location, "", 10, 5, 0, new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)), new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(2)), false, currentUser.getUid(), "");
        return Tasks.await(new EventRepository().createEvent(currentUser, event, posterUri), 15, TimeUnit.SECONDS);
    }

    /**
     * loads a hosted event from the database by its title for the signed-in test user
     * @param title
     * title of the event to look up
     * @return
     * the matching event document
     */
    private DocumentSnapshot findEventByTitle(String title) throws Exception {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return Tasks.await(FirebaseFirestore.getInstance().collection("events").whereEqualTo("hostUid", currentUser.getUid()).whereEqualTo("title", title).get(), 15, TimeUnit.SECONDS).getDocuments().get(0);
    }

    /**
     * removes all hosted test events that match the supplied title
     * @param title
     * title of the events that should be deleted
     */
    private void deleteEventsByTitle(String title) throws Exception {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        List<DocumentSnapshot> events = Tasks.await(FirebaseFirestore.getInstance().collection("events").whereEqualTo("hostUid", currentUser.getUid()).whereEqualTo("title", title).get(), 15, TimeUnit.SECONDS).getDocuments();
        for (DocumentSnapshot event : events) {
            deleteEvent(event.getId());
        }
    }

    /**
     * deletes a single event document from the database
     * @param eventId
     * document id of the event that should be removed
     */
    private void deleteEvent(String eventId) throws Exception {
        Tasks.await(FirebaseFirestore.getInstance().collection("events").document(eventId).delete(), 15, TimeUnit.SECONDS);
    }

    /**
     * copies the local Athabasca Hall image asset into cache and returns a file uri for upload
     * @return
     * a uri that can be passed to the poster upload flow
     */
    private Uri createPosterUri(String assetName) throws Exception {
        Context appContext = ApplicationProvider.getApplicationContext();
        Context testContext = InstrumentationRegistry.getInstrumentation().getContext();
        File file = new File(appContext.getCacheDir(), assetName);
        try (InputStream inputStream = testContext.getAssets().open(assetName);
             FileOutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
        }
        return Uri.fromFile(file);
    }

    /**
     * checks that an ImageView currently has drawable content to display
     * @return
     * matcher that passes when the preview image is loaded
     */
    private Matcher<View> posterPreviewLoaded() {
        return new TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(View view) {
                return view instanceof ImageView && ((ImageView) view).getDrawable() != null;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("ImageView with drawable");
            }
        };
    }
}
