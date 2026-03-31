package com.example.eventlotterysystem;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
 * Displays the messages inside a single chat thread.
 */
public class ChatMessageAdapter extends ArrayAdapter<ChatMessageItem> {
    private static final String DATE_PATTERN = "MMM d, yyyy h:mm a";

    private final LayoutInflater inflater;
    private final String currentUserUid;

    public ChatMessageAdapter(
            @NonNull Context context,
            @NonNull List<ChatMessageItem> messages,
            @NonNull String currentUserUid
    ) {
        super(context, 0, messages);
        inflater = LayoutInflater.from(context);
        this.currentUserUid = currentUserUid;
    }

    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.item_chat_message, parent, false);
        }

        ChatMessageItem item = getItem(position);
        if (item == null) {
            return view;
        }

        boolean outgoing = currentUserUid.equals(item.getSenderUid());

        LinearLayout bubbleContainer = view.findViewById(R.id.chatMessageBubbleContainer);
        TextView senderView = view.findViewById(R.id.chatMessageSender);
        TextView textView = view.findViewById(R.id.chatMessageText);
        TextView timestampView = view.findViewById(R.id.chatMessageTimestamp);

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) bubbleContainer.getLayoutParams();
        params.gravity = outgoing ? Gravity.END : Gravity.START;
        bubbleContainer.setLayoutParams(params);
        bubbleContainer.setBackgroundResource(outgoing ? R.drawable.bgoutcoming : R.drawable.bgincoming);

        senderView.setText(item.getSenderName());
        textView.setText(item.getText());
        if (item.getSentAt() <= 0L) {
            timestampView.setText("");
        } else {
            timestampView.setText(new SimpleDateFormat(DATE_PATTERN, Locale.getDefault())
                    .format(new Date(item.getSentAt())));
        }
        return view;
    }
}
