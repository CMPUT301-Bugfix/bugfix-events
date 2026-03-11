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
 * This is a class that converts a list of Events into a displayable format
 */
public class EventListAdapter extends ArrayAdapter<EventItem> {

    private static final String DATE_PATTERN = "MMM d, yyyy";
    private final LayoutInflater inflater;

    /**
     * This initializes the adapter with all Events in a list
     * @param context
     * Context the instance of the application running
     * @param items
     * List<EventItem> of every event to be displayed
     */
    public EventListAdapter(@NonNull Context context, @NonNull List<EventItem> items) {
        super(context, 0, items);
        inflater = LayoutInflater.from(context);
    }

    /**
     * This initializes the adapter with title location and date of all events
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
     * View a listView of all Events
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.item_event, parent, false);
        }

        EventItem item = getItem(position);
        if (item == null) {
            return view;
        }

        TextView titleView = view.findViewById(R.id.eventItemTitle);
        TextView locationView = view.findViewById(R.id.eventItemLocation);
        TextView dateView = view.findViewById(R.id.eventItemDate);

        String location = hasText(item.getLocation())
                ? item.getLocation()
                : getContext().getString(R.string.event_card_missing_location);
        String date = hasText(formatDate(item.getEventDate()))
                ? formatDate(item.getEventDate())
                : getContext().getString(R.string.event_card_missing_date);

        titleView.setText(item.getTitle());
        locationView.setText(getContext().getString(R.string.event_location_label, location));
        dateView.setText(getContext().getString(R.string.event_date_label, date));
        return view;
    }

    /**
     * method that check is there is content in the string
     * @param value
     * the string to be testing
     * @return
     * true if there was actual text in the string
     */
    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * This changes the date to match locality
     * @param date
     * Date the time of the Event
     * @return
     * String representation of the date that matches the timezone of the user
     */
    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).format(date);
    }
}
