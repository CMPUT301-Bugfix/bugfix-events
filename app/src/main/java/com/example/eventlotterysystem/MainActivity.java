package com.example.eventlotterysystem;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * This is a class that is the controller which screen the user should start on
 */
public class MainActivity extends AppCompatActivity {

    /**
     * This opens the AuthMenuActivity if the user is not recognized
     * If the user is recognized opens HomeActivity
     * @param savedInstanceState
     * the saved state of the Activity so that the screen is not reset
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null && !AuthSessionPreference.shouldRemember(this)) {
            auth.signOut();
            currentUser = null;
        }

        Intent destination = currentUser == null
                ? new Intent(this, AuthMenuActivity.class)
                : new Intent(this, HomeActivity.class);
        startActivity(destination);
        finish();
    }
}
