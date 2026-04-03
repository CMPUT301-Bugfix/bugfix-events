package com.example.eventlotterysystem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying a list of NotificationItem objects in a RecyclerView.
 * This class handles the layout inflation and display for individual notification items,
 * and provides click listeners for interaction.
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    /**
     * Interface for handling click and long-click events on notification items.
     */
    public interface OnNotificationClickListener {
        /**
         * Triggered when a notification is clicked.
         * @param notification  NotificationItem that was clicked.
         */
        void onNotificationClick(NotificationItem notification);

        /**
         * Triggered when a notification is long-clicked.
         * @param notification NotificationItem that was long-clicked.
         */
        void onNotificationLongClick(NotificationItem notification);
    }

    private final List<NotificationItem> notifications;
    private final OnNotificationClickListener listener;
    private static final String DATE_PATTERN = "MMM d, yyyy h:mm a";

    /**
     * Constructs a new NotificationAdapter with the provided list of notifications and click listener
     *
     * @param notifications list of NotificationItem objects to display.
     * @param listener      click listener for notification interactions.
     */
    public NotificationAdapter(List<NotificationItem> notifications, OnNotificationClickListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    /**
     * creates the ViewHolder for a notification row
     * @param parent
     * the parent view group for the row
     * @param viewType
     * the type of row view being created
     * @return
     * the created NotificationViewHolder
     */
    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    /**
     * fills a notification row with the current notification data
     * @param holder
     * the ViewHolder being updated
     * @param position
     * the position of the notification in the list
     */
    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationItem item = notifications.get(position);
        holder.title.setText(item.getTitle());
        holder.message.setText(item.getMessage());
        
        if (item.getTimestamp() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN, Locale.getDefault());
            holder.timestamp.setText(sdf.format(new Date(item.getTimestamp())));
        } else {
            holder.timestamp.setText("");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(item);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onNotificationLongClick(item);
            }
            return true;
        });
    }

    /**
     * gets the number of notifications in the adapter
     * @return
     * the number of notifications in the adapter
     */
    @Override
    public int getItemCount() {
        return notifications.size();
    }

    /**
     * ViewHolder class for individual notification list items
     */
    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView title, message, timestamp;

        /**
         * Initializes the ViewHolder by finding the relevant views in the item layout
         * @param itemView The root view of the individual list item
         */
        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.notificationEventTitle);
            message = itemView.findViewById(R.id.notificationMessage);
            timestamp = itemView.findViewById(R.id.notificationTimestamp);
        }
    }
}
