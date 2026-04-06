package com.example.eventlotterysystem;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

/**
 * This is a class that is the controller of the activity_terms screen
 * it shows the app terms and allows the user to leave the screen
 */
public class TermsActivity extends AppCompatActivity {
    /**
     * This is the creation of the Activity
     * This connects to all the view on the screen and connects the clickable view to their controller
     * @param savedInstanceState
     * the saved state of the Activity so that the screen is not reset
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms);

        Button okButton = findViewById(R.id.okButton);
        okButton.setOnClickListener(v -> finish());
    }
}
