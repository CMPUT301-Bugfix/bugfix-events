package com.example.eventlotterysystem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter for displaying user search results in a RecyclerView
 * provides a way to add users to a batch for private event invitations
 */
public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.UserSearchViewHolder> {

    /**
     * interface for handling user addition events
     */
    public interface OnUserAddListener {
        /**
         * Triggered when add button is clicked for a user
         * @param user UserProfile to be added
         */
        void onUserAdd(UserProfile user);
    }

    private final List<UserProfile> users;
    private final OnUserAddListener listener;

    /**
     * constructs new UserSearchAdapter
     * @param users list of UserProfile results to display
     * @param listener listener for add button clicks
     */
    public UserSearchAdapter(List<UserProfile> users, OnUserAddListener listener) {
        this.users = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserSearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_search, parent, false);
        return new UserSearchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserSearchViewHolder holder, int position) {
        UserProfile user = users.get(position);
        holder.name.setText(user.getName());
        holder.username.setText(user.getUsername());
        holder.email.setText(user.getEmail());

        holder.addButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserAdd(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    /**
     * viewHolder for user search result items, yippee
     */
    public static class UserSearchViewHolder extends RecyclerView.ViewHolder {
        TextView name, username, email;
        Button addButton;

        /**
         * initializes the ViewHolder with item views
         * @param itemView root view of the item layout
         */
        public UserSearchViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.userSearchName);
            username = itemView.findViewById(R.id.userSearchUsername);
            email = itemView.findViewById(R.id.userSearchEmail);
            addButton = itemView.findViewById(R.id.addUserToBatchButton);
        }
    }
}
