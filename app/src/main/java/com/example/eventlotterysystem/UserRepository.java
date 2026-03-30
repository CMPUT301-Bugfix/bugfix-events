package com.example.eventlotterysystem;


import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The Class the manages the interactions between UserProfile as a object in the program and as a document in the database
 */
public class UserRepository {
    private final FirebaseFirestore firestore;
    private final FirebaseStorage storage;

    /**
     * creates the UserRepository object which is just references to the database
     * the methods allows for access to User documents in the database
     */
    public UserRepository() {
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    /**
     * Loads the Information of all entrant with a match to find the correct one
     */
    public Task<List<UserProfile>> loadMatchingEntrant(String field, String value) {
        return firestore.collection("users")
                .whereEqualTo(field, value)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException() != null
                                ? task.getException()
                                : new IllegalStateException("No Matching User Found");
                    }

                    List<UserProfile> Users = new ArrayList<>();

                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            Users.add(readUserProfile(doc));
                    }
                    return Users;
                });
    }

    /**
     * reads in a user document in the database into an UserProfile object
     * @param doc
     * a reference to user document in the database
     * @return
     * a created UserProfile object with the data from the document
     */
    public UserProfile readUserProfile(@NonNull DocumentSnapshot doc) {
        String uid = doc.getId();
        String name = doc.getString("fullName");
        String email = doc.getString("email");
        String username = doc.getString("username");
        String usernameKey = doc.getString("usernameKey");
        String acccountType = doc.getString("accountType");
        String phoneNumber = doc.getString("phoneNumber");

        if (phoneNumber == null) {
            phoneNumber = "";
        }

        UserProfile user = new UserProfile(name, email, username, usernameKey, phoneNumber, acccountType);
        user.setUid(uid);

        return user;
    }
}
