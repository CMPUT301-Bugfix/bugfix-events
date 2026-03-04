package com.example.eventlotterysystem;

import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class UserProfileArrayAdapter extends ArrayAdapter<UserProfile> {

    public UserProfileArrayAdapter(@NonNull Context context, @NonNull ArrayList<UserProfile> profiles) {
        super(context, 0, profiles);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.row_user_profile, parent, false);
        }

        UserProfile profile = getItem(position);

        TextView name = view.findViewById(R.id.txt_profile_name);
        TextView role = view.findViewById(R.id.txt_profile_role);
        TextView summary = view.findViewById(R.id.txt_profile_summary);

        if (profile != null) {
            name.setText(profile.getFullName());
            role.setText(profile.getAccountType().name());
            summary.setText(profile.getSummary() == null ? "" : profile.getSummary());
        }

        return view;
    }
}
