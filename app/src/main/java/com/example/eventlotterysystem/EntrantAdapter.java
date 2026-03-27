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

/**
 * This is a class that converts a list of Entrants into a displayable format
 */
public class EntrantAdapter extends ArrayAdapter<UserProfile> {
    private final LayoutInflater inflater;

    /**
     * This initializes the adapter with all Entrants in a list
     * @param context
     * the instance of the application running
     * @param items
     * List of UserProfiles of every Entrant to be displayed
     */
    public EntrantAdapter(@NonNull Context context, @NonNull List<UserProfile> items) {
        super(context, 0, items);
        inflater = LayoutInflater.from(context);
    }

    /**
     * This initializes the adapter with Name and Username of all Events
     * @param position The position of the item within the adapter's data set of the item whose view
     *        we want.
     * @param convertView The old view to reuse, if possible. Note: You should check that this view
     *        is non-null and of an appropriate type before using. If it is not possible to convert
     *        this view to display the correct data, this method can create a new view.
     *        Heterogeneous lists can specify their number of view types, so that this View is
     *        always of the right type (see {@link #getViewTypeCount()} and
     *        {@link #getItemViewType(int)}).
     * @param parent The parent that this view will eventually be attached to
     * @return
     * listView View of all Entrants
     */
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

    /**
     * ensure that a proper string is displayed by having a return fallback is there is an issue with value
     * @param value
     * desired string of the field to be displayed
     * @param fallback
     * string that should be displayed if value turns out ot be empty
     * @return
     * the string to be displayed
     */
    @NonNull
    private String safeValue(String value, @NonNull String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}
