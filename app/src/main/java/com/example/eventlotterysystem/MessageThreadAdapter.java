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
 * Adapter used to display message threads in a list.
 */
public class MessageThreadAdapter extends ArrayAdapter<MessageThreadItem> {
    private static final String DATE_PATTERN = "MMM d, yyyy h:mm a";
    private final LayoutInflater inflater;
    private final String currentUserUid;

    public MessageThreadAdapter(
            @NonNull Context context,
            @NonNull List<MessageThreadItem> items,
            @NonNull String currentUserUid
    ) {
        super(context, 0, items);
        inflater = LayoutInflater.from(context);
        this.currentUserUid = currentUserUid;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.item_message_thread, parent, false);
        }

        MessageThreadItem item = getItem(position);
        if (item == null) {
            return view;
        }

        TextView counterpartView = view.findViewById(R.id.messageThreadCounterpart);
        TextView previewView = view.findViewById(R.id.messageThreadPreview);
        TextView timestampView = view.findViewById(R.id.messageThreadTimestamp);

        String otherName;
        if (currentUserUid.equals(item.getUserOneUid())) {
            otherName = item.getUserTwoName();
        } else {
            otherName = item.getUserOneName();
        }

        counterpartView.setText(otherName);
        previewView.setText(item.getLastMessageText());
        if (item.getLastMessageAt() <= 0L) {
            timestampView.setText("");
        } else {
            timestampView.setText(new SimpleDateFormat(DATE_PATTERN, Locale.getDefault())
                    .format(new Date(item.getLastMessageAt())));
        }

        return view;
    }
}
