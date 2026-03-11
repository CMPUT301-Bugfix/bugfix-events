package com.example.eventlotterysystem;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * This is a class that is the controller of the activity_auth_menu screen
 */
public class AuthMenuActivity extends AppCompatActivity {

    /**
     * This is the creation of the Activity
     * This connects to layout for the screen and connects the clickable view to their controller
     * @param savedInstanceState
     * the saved state of the Activity so that the screen is not reset
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth_menu);

        findViewById(R.id.menuLoginButton).setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));
        findViewById(R.id.menuCreateAccountButton).setOnClickListener(v ->
                startActivity(new Intent(this, SignupActivity.class)));
    }

    /**
     * This is the startup of the Activity
     * This connects to the firebase database and tries the current user
     * if there is a current user runs navigateAndClearTask()
     */
    @Override
    protected void onStart() {
        super.onStart();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null && AuthSessionPreference.shouldRemember(this)) {
            navigateAndClearTask(HomeActivity.class);
        } else if (currentUser != null) {
            auth.signOut();
        }
    }

    /**
     * This finished the current activity and open a new activity based on arguments
     * @param destination
     * the activity class that should be naviagted to
     */
    private void navigateAndClearTask(Class<?> destination) {
        Intent intent = new Intent(this, destination);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
