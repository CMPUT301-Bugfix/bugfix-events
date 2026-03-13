package com.example.eventlotterysystem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * This is a class converts the list of Events that a User has into a displayable format
 */
public class WaitlistEntryAdapter extends ArrayAdapter<WaitlistEntryItem> {
    private static final String DATE_PATTERN = "MMM d, yyyy";
    private final LayoutInflater inflater;

    /**
     * This initializes the adapter with all of the WaitlistEntryItem for the User
     * @param context
     * Context the instance of the application running
     * @param items
     * List<WaitlistEntryItem> of every current event that the user is in a stage of waitlist for
     */
    public WaitlistEntryAdapter(@NonNull Context context, @NonNull List<WaitlistEntryItem> items) {
        super(context, 0, items);
        inflater = LayoutInflater.from(context);
    }

    /**
     * This initializes the adapter with all of the WaitlistEntryItem for the User
     * @param position
     * The index which view in the listview that the Event information is on
     * @param convertView
     * The initial view of a entry without signed up Event info
     * @param parent
     * ViewGroup the collection of views for all listView items
     * @return
     * View a listView of all signed up Events
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.item_waitlist_event, parent, false);
        }

        WaitlistEntryItem item = getItem(position);
        if (item == null) {
            return view;
        }

        TextView nameView = view.findViewById(R.id.waitlistItemName);
        TextView dateView = view.findViewById(R.id.waitlistItemDate);
        TextView statusView = view.findViewById(R.id.waitlistItemStatus);

        nameView.setText(item.getTitle());
        String date = formatDate(item.getEventDate());
        if (!date.isEmpty()) {
            dateView.setText(getContext().getString(R.string.event_date_label, date));
        } else {
            dateView.setText(getContext().getString(
                    R.string.event_date_label,
                    getContext().getString(R.string.event_card_missing_date)
            ));
        }
        statusView.setText(getContext().getString(R.string.waitlist_status_label, getStatusLabel(item.getStatus())));
        return view;
    }

    /**
     * This changes the date to match locality
     */
    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).format(date);
    }

    /**
     * This gets the matching text label for the state that the signup is in
     * @param status
     */
    private String getStatusLabel(String status) {
        if (EventRepository.WAITLIST_STATUS_IN.equals(status)) {
            return getContext().getString(R.string.waitlist_status_in_waitlist);
        }
        if (EventRepository.WAITLIST_STATUS_CHOSEN.equals(status)) {
            return "Chosen";
        }
        if (EventRepository.WAITLIST_STATUS_CONFIRMED.equals(status)) {
            return "Confirmed";
        }
        if (EventRepository.WAITLIST_STATUS_DECLINED.equals(status)) {
            return "Declined";
        }
        return status;
    }
}
