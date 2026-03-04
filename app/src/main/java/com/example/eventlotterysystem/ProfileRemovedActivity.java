package com.example.eventlotterysystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ProfileRemovedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_removed);

        Button goToLoginButton = findViewById(R.id.profileRemovedGoToLoginButton);
        goToLoginButton.setOnClickListener(v -> navigateToAuthMenu());
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseAuth.getInstance().signOut();
        AuthSessionPreference.setRemember(this, false);
    }

    private void navigateToAuthMenu() {
        Intent intent = new Intent(this, AuthMenuActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
