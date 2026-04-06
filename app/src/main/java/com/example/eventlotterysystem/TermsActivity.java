package com.example.eventlotterysystem;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class TermsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms);

        Button okButton = findViewById(R.id.okButton);
        okButton.setOnClickListener(v -> finish());
    }
}