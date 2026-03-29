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
 * Adapter used to display event comments in a list.
 */
public class CommentAdapter extends ArrayAdapter<CommentItem> {
    private static final String DATE_PATTERN = "MMM d, yyyy h:mm a";
    private final LayoutInflater inflater;

    /**
     * Creates the adapter for a list of comments.
     *
     * @param context the current activity context
     * @param items the comments to display
     */
    public CommentAdapter(@NonNull Context context, @NonNull List<CommentItem> items) {
        super(context, 0, items);
        inflater = LayoutInflater.from(context);
    }

    /**
     * Creates or reuses a row view and fills it with comment information.
     *
     * @param position the index of the current comment item
     * @param convertView a recycled row view if one is available
     * @param parent the parent view group containing the list
     * @return a populated row view for the current comment
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.item_comment, parent, false);
        }

        CommentItem item = getItem(position);
        if (item == null) {
            return view;
        }

        TextView usernameView = view.findViewById(R.id.commentUsername);
        TextView textView = view.findViewById(R.id.commentText);
        TextView dateView = view.findViewById(R.id.commentDate);

        String username = item.getUsername();
        if (username == null || username.trim().isEmpty()) {
            username = "Unknown user";
        }

        usernameView.setText(username);
        textView.setText(item.getText());
        dateView.setText(formatDate(item.getCreatedAt()));

        return view;
    }

    /**
     * Formats the comment timestamp for display.
     *
     * @param date the comment creation date
     * @return a formatted date string, or an empty string if the date is null
     */
    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).format(date);
    }
}