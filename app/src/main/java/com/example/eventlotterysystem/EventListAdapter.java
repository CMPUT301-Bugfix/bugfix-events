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

public class EventListAdapter extends ArrayAdapter<EventItem> {

    private static final String DATE_PATTERN = "MMM d, yyyy";
    private final LayoutInflater inflater;

    public EventListAdapter(@NonNull Context context, @NonNull List<EventItem> items) {
        super(context, 0, items);
        inflater = LayoutInflater.from(context);
    }

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

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).format(date);
    }
}
