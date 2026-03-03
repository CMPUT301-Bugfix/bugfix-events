package com.example.eventlotterysystem;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserProfileDetailsActivity extends AppCompatActivity {

    public static final String NAME = "name";
    public static final String ACCOUNT_TYPE = "accountType";
    public static final String USERNAME = "username";
    public static final String EMAIL = "email";
    public static final String PHONE = "phone";
    public static final String TIME_MILLIS = "timeMillis";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile_details);

        TextView detailsBackButton = findViewById(R.id.userProfileDetailsBackButton);
        TextView detailsNameValue = findViewById(R.id.userProfileDetailsNameValue);
        TextView detailsTypeValue = findViewById(R.id.userProfileDetailsTypeValue);
        TextView detailsUsernameValue = findViewById(R.id.userProfileDetailsUsernameValue);
        TextView detailsEmailValue = findViewById(R.id.userProfileDetailsEmailValue);
        TextView detailsPhoneValue = findViewById(R.id.userProfileDetailsPhoneValue);
        TextView detailsJoinDateValue = findViewById(R.id.userProfileDetailsJoinDateValue);

        detailsBackButton.setOnClickListener(v -> finish());

        String name = normalize(getIntent().getStringExtra(NAME));
        String accountType = normalize(getIntent().getStringExtra(ACCOUNT_TYPE));
        String username = normalize(getIntent().getStringExtra(USERNAME));
        String email = normalize(getIntent().getStringExtra(EMAIL));
        String phone = normalize(getIntent().getStringExtra(PHONE));
        long timeMillis = getIntent().getLongExtra(TIME_MILLIS, -1L);

        detailsNameValue.setText(getString(R.string.profile_name_label, safeValue(name, R.string.unknown_name)));
        detailsTypeValue.setText(getString(R.string.profile_type_label, safeValue(accountType, R.string.unknown_account_type)));
        detailsUsernameValue.setText(getString(R.string.profile_username_label, safeValue(username, R.string.unknown_username)));
        detailsEmailValue.setText(getString(R.string.profile_email_label, safeValue(email, R.string.unknown_email)));
        detailsPhoneValue.setText(getString(R.string.profile_phone_label, safeValue(phone, R.string.unknown_phone)));
        detailsJoinDateValue.setText(getString(R.string.profile_join_date_label, formatJoinDate(timeMillis)));
    }

    @NonNull
    private String safeValue(@NonNull String value, int fallbackResId) {
        if (TextUtils.isEmpty(value)) {
            return getString(fallbackResId);
        }
        return value;
    }

    @NonNull
    private String formatJoinDate(long createdAtMillis) {
        if (createdAtMillis < 0L) {
            return getString(R.string.unknown_join_date);
        }
        return new SimpleDateFormat("MMM d, yyyy", Locale.US).format(new Date(createdAtMillis));
    }

    @NonNull
    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
