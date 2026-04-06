package com.example.eventlotterysystem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter used to display threaded event comments in a list.
 *
 * <p>This adapter renders comments in a Reddit-style flat thread view.
 * Nested replies are visually indented based on their depth.</p>
 */
public class CommentAdapter extends ArrayAdapter<CommentItem> {
    private static final String DATE_PATTERN = "MMM d, yyyy h:mm a";
    private static final int INDENT_DP = 20;

    /**
     * Listener for user interactions on a comment row.
     */
    public interface CommentActionListener {
        /**
         * Called when the user taps Reply on a comment.
         *
         * @param comment the selected comment
         */
        void onReplyClicked(@NonNull CommentItem comment);

        /**
         * Called when the user taps the upvote control on a comment.
         *
         * @param comment the selected comment
         */
        void onUpvoteClicked(@NonNull CommentItem comment);

        /**
         * Called when the user taps the downvote control on a comment.
         *
         * @param comment the selected comment
         */
        void onDownvoteClicked(@NonNull CommentItem comment);
    }

    private final LayoutInflater inflater;
    private final CommentActionListener listener;

    /**
     * Creates an adapter for displaying threaded comments.
     *
     * @param context the current activity context
     * @param items the comments to display
     * @param listener callback receiver for comment actions
     */
    public CommentAdapter(
            @NonNull Context context,
            @NonNull List<CommentItem> items,
            @NonNull CommentActionListener listener
    ) {
        super(context, 0, items);
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
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

        LinearLayout root = view.findViewById(R.id.commentRoot);
        TextView usernameView = view.findViewById(R.id.commentUsername);
        TextView textView = view.findViewById(R.id.commentText);
        TextView dateView = view.findViewById(R.id.commentDate);
        TextView scoreView = view.findViewById(R.id.commentScore);
        TextView upvoteView = view.findViewById(R.id.commentUpvote);
        TextView downvoteView = view.findViewById(R.id.commentDownvote);
        TextView replyView = view.findViewById(R.id.commentReply);

        String username = item.getUsername();
        if (username == null || username.trim().isEmpty()) {
            username = "Unknown user";
        }

        usernameView.setText(username);
        textView.setText(item.getText());
        dateView.setText(formatDate(item.getCreatedAt()));
        scoreView.setText(String.valueOf(item.getScore()));

        int indentPx = (int) (INDENT_DP * item.getDepth() * view.getResources().getDisplayMetrics().density);
        root.setPadding(
                12 + indentPx,
                12,
                12,
                12
        );

        upvoteView.setOnClickListener(v -> listener.onUpvoteClicked(item));
        downvoteView.setOnClickListener(v -> listener.onDownvoteClicked(item));
        replyView.setOnClickListener(v -> listener.onReplyClicked(item));

        return view;
    }
    /**
     * Formats a comment timestamp for display.
     *
     * @param date the comment creation date
     * @return a formatted date string, or an empty string if the date is null
     */
    private String formatDate(@Nullable Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).format(date);
    }
}