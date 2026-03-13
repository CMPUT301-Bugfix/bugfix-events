package com.example.eventlotterysystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

/**
 * This is a class that is the controller of the activity_profile_removed screen
 * this activity is launch when a user is logged into a deleted account
 */
public class ProfileRemovedActivity extends AppCompatActivity {

    /**
     * This is the creation of the Activity
     * This connect to the view on the screen and connects button to its controller
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_removed);

        Button goToLoginButton = findViewById(R.id.profileRemovedGoToLoginButton);
        goToLoginButton.setOnClickListener(v -> navigateToAuthMenu());
    }

    /**
     * This is the startup of the Activity
     * signs the user out if the deleted account
     */
    @Override
    protected void onStart() {
        super.onStart();
        FirebaseAuth.getInstance().signOut();
        AuthSessionPreference.setRemember(this, false);
    }

    /**
     * this navigates user to AuthMenuActivity and is the controller for goToLoginButton button
     */
    private void navigateToAuthMenu() {
        Intent intent = new Intent(this, AuthMenuActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
