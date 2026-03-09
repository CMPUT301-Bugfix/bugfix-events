package com.example.eventlotterysystem;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

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
                ? new Intent(this, HostedEventsActivty.class) //is wrong need to remove
                : new Intent(this, HomeActivity.class);
        startActivity(destination);
        finish();
    }
}
