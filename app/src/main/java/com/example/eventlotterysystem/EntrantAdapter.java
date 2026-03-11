package com.example.eventlotterysystem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class EntrantAdapter extends ArrayAdapter<UserProfile> {
    private final LayoutInflater inflater;

    public EntrantAdapter(@NonNull Context context, @NonNull List<UserProfile> items) {
        super(context, 0, items);
        inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.item_entrant, parent, false);
        }

        UserProfile item = getItem(position);
        if (item == null) {
            return view;
        }

        TextView nameView = view.findViewById(R.id.entrantItemName);
        TextView usernameView = view.findViewById(R.id.entrantItemUsername);

        nameView.setText(getContext().getString(
                R.string.profile_name_label,
                safeValue(item.getName(), getContext().getString(R.string.unknown_name))
        ));
        usernameView.setText(getContext().getString(
                R.string.profile_username_label,
                safeValue(item.getUsername(), getContext().getString(R.string.unknown_username))
        ));
        return view;
    }

    @NonNull
    private String safeValue(String value, @NonNull String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}
