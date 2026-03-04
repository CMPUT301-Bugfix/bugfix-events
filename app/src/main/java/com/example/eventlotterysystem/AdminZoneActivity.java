package com.example.eventlotterysystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class AdminZoneActivity extends AppCompatActivity {
    private ImageButton backButton;
    private LinearLayout userProfilesButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_zone);

        backButton = findViewById(R.id.btn_back);
        userProfilesButton = findViewById(R.id.btn_user_profiles);

        backButton.setOnClickListener(v -> finish());

        userProfilesButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminZoneActivity.this, UserProfilesActivity.class);
            startActivity(intent);
        });


    }
}
