package com.example.eventlotterysystem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

/**
 * adapter for displaying invitation status in RecyclerView
 * manages the mapping between invitation data and view holders
 */
public class InvitationAdapter extends RecyclerView.Adapter<InvitationAdapter.InvitationViewHolder> {

    private final List<Map<String, Object>> invitations;

    /**
     * constructs InvitationAdapter with a list of invitation maps
     * @param invitations List of maps containing invitation details like name, email, and status
     */
    public InvitationAdapter(List<Map<String, Object>> invitations) {
        this.invitations = invitations;
    }

    /**
     * creates a new InvitationViewHolder
     * @param parent The viewGroup into which the new View will be added
     * @param viewType The view type of the new View
     * @return new InvitationViewHolder that holds the View for each list item
     */
    @NonNull
    @Override
    public InvitationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_invitation, parent, false);
        return new InvitationViewHolder(view);
    }

    /**
     * stitches invitation data to the view holder at the specified position
     * @param holder The InvitationViewHolder which should be updated
     * @param position The position of the item within the adapter's data set
     */
    @Override
    public void onBindViewHolder(@NonNull InvitationViewHolder holder, int position) {
        Map<String, Object> invite = invitations.get(position);
        String name = (String) invite.get("name");
        String email = (String) invite.get("email");
        String username = (String) invite.get("username");
        String status = (String) invite.get("status");

        holder.name.setText(name != null ? name : "Unknown User");
        holder.detail.setText((username != null ? "@" + username : "") + (email != null ? " (" + email + ")" : ""));
        holder.status.setText(status != null ? status : "PENDING");
    }

    /**
     * returns the total number of items in the data set held by the adapter
     * @return size of invitations list (amount of invitations sent)
     */
    @Override
    public int getItemCount() {
        return invitations.size();
    }

    /**
     * ViewHolder class for invitation list items
     * connects UI components to data
     */
    public static class InvitationViewHolder extends RecyclerView.ViewHolder {
        TextView name, detail, status;

        /**
         * Initializes the InvitationViewHolder with the item view
         * @param itemView The inflated layout view for a single item
         */
        public InvitationViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.invitationUserName);
            detail = itemView.findViewById(R.id.invitationUserDetail);
            status = itemView.findViewById(R.id.invitationStatusText);
        }
    }
}
