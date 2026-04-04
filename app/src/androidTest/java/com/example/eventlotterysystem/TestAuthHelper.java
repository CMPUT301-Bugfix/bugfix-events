package com.example.eventlotterysystem;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.TimeUnit;

/**
 * Shared authentication helper for instrumentation tests to avoid unnecessary repeated sign-ins.
 */
public final class TestAuthHelper {

    private static final String SHARED_TEST_EMAIL = "test@gmail.com";
    private static final String SHARED_TEST_PASSWORD = "test123";

    private TestAuthHelper() {
    }

    /**
     * Ensures the shared test account is signed in and remember-me is disabled.
     */
    public static void ensureSharedTestUser() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        AuthSessionPreference.setRemember(context, false);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null && SHARED_TEST_EMAIL.equalsIgnoreCase(currentUser.getEmail())) {
            return;
        }

        if (currentUser != null) {
            auth.signOut();
        }

        Tasks.await(
                auth.signInWithEmailAndPassword(SHARED_TEST_EMAIL, SHARED_TEST_PASSWORD),
                30,
                TimeUnit.SECONDS
        );
    }
}
