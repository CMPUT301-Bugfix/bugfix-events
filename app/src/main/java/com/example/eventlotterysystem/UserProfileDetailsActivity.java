package com.example.eventlotterysystem;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class UserProfileDetailsActivity extends AppCompatActivity {

    public static final String NAME = "name";
    public static final String ACCOUNT_TYPE = "accountType";
    public static final String USERNAME = "username";
    public static final String EMAIL = "email";
    public static final String PHONE = "phone";
    public static final String UID = "uid";
    public static final String TIME_MILLIS = "timeMillis";

    private FirebaseFirestore firestore;
    private Button deleteProfileButton;
    private boolean isDeleting;
    private String viewedUid;
    private String viewedAccountType;
    private String viewedName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile_details);
        firestore = FirebaseFirestore.getInstance();

        TextView detailsBackButton = findViewById(R.id.userProfileDetailsBackButton);
        TextView detailsNameValue = findViewById(R.id.userProfileDetailsNameValue);
        TextView detailsTypeValue = findViewById(R.id.userProfileDetailsTypeValue);
        TextView detailsUsernameValue = findViewById(R.id.userProfileDetailsUsernameValue);
        TextView detailsEmailValue = findViewById(R.id.userProfileDetailsEmailValue);
        TextView detailsPhoneValue = findViewById(R.id.userProfileDetailsPhoneValue);
        TextView detailsJoinDateValue = findViewById(R.id.userProfileDetailsJoinDateValue);
        deleteProfileButton = findViewById(R.id.userProfileDeleteButton);

        detailsBackButton.setOnClickListener(v -> finish());

        viewedName = normalize(getIntent().getStringExtra(NAME));
        viewedAccountType = normalize(getIntent().getStringExtra(ACCOUNT_TYPE));
        viewedUid = normalize(getIntent().getStringExtra(UID));

        String username = normalize(getIntent().getStringExtra(USERNAME));
        String email = normalize(getIntent().getStringExtra(EMAIL));
        String phone = normalize(getIntent().getStringExtra(PHONE));
        long timeMillis = getIntent().getLongExtra(TIME_MILLIS, -1L);

        detailsNameValue.setText(getString(R.string.profile_name_label, safeValue(viewedName, R.string.unknown_name)));
        detailsTypeValue.setText(getString(R.string.profile_type_label, safeValue(viewedAccountType, R.string.unknown_account_type)));
        detailsUsernameValue.setText(getString(R.string.profile_username_label, safeValue(username, R.string.unknown_username)));
        detailsEmailValue.setText(getString(R.string.profile_email_label, safeValue(email, R.string.unknown_email)));
        detailsPhoneValue.setText(getString(R.string.profile_phone_label, safeValue(phone, R.string.unknown_phone)));
        detailsJoinDateValue.setText(getString(R.string.profile_join_date_label, formatJoinDate(timeMillis)));

        boolean canDeleteViewedProfile = !TextUtils.isEmpty(viewedUid)
                && !"admin".equalsIgnoreCase(viewedAccountType);
        if (!canDeleteViewedProfile) {
            deleteProfileButton.setVisibility(View.GONE);
            return;
        }
        deleteProfileButton.setOnClickListener(v -> onDeleteProfileClicked());
    }

    private void onDeleteProfileClicked() {
        if (isDeleting) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.admin_delete_profile_confirm_title)
                .setMessage(getString(
                        R.string.admin_delete_profile_confirm_message,
                        safeValue(viewedName, R.string.unknown_name)
                ))
                .setNegativeButton(R.string.admin_delete_profile_cancel_action, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.admin_delete_profile_confirm_action, (dialog, which) -> deleteViewedProfile())
                .show();
    }

    private void deleteViewedProfile() {
        if (TextUtils.isEmpty(viewedUid)) {
            showMessage(getString(R.string.admin_delete_profile_failed));
            return;
        }

        setDeleting(true);
        firestore.collection("users")
                .document(viewedUid)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    setDeleting(false);
                    showMessage(getString(R.string.admin_delete_profile_success));
                    finish();
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        handleDeleteFailure(e);
                    }
                });
    }

    private void handleDeleteFailure(@NonNull Exception exception) {
        setDeleting(false);
        if (exception instanceof FirebaseFirestoreException
                && ((FirebaseFirestoreException) exception).getCode()
                == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            showMessage(getString(R.string.admin_delete_profile_permission_denied));
            return;
        }
        showMessage(getString(R.string.admin_delete_profile_failed));
    }

    private void setDeleting(boolean deleting) {
        isDeleting = deleting;
        deleteProfileButton.setEnabled(!deleting);
    }

    private void showMessage(@NonNull String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
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
