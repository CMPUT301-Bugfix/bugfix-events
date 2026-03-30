package com.example.eventlotterysystem;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays and manages comments for a specific event.
 */
public class CommentsActivity extends AppCompatActivity {
    private static final String TAG = "CommentsActivity";
    public static final String EVENT_ID = "EVENT_ID";

    private FirebaseAuth auth;
    private EventRepository repository;

    private String eventId;
    private boolean canDeleteComments;
    private EditText commentInput;
    private TextView emptyState;
    private ListView commentsListView;
    private CommentAdapter commentAdapter;
    private final List<CommentItem> comments = new ArrayList<>();

    /**
     * Initializes the comments screen, validates the event ID, and binds UI actions.
     *
     * @param savedInstanceState the previously saved activity state, if any
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        auth = FirebaseAuth.getInstance();
        repository = new EventRepository();

        eventId = getIntent().getStringExtra(EVENT_ID);
        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this, R.string.missing_event_id, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        commentInput = findViewById(R.id.commentsInput);
        emptyState = findViewById(R.id.commentsEmptyState);
        commentsListView = findViewById(R.id.commentsListView);
        Button postCommentButton = findViewById(R.id.commentsPostButton);

        commentAdapter = new CommentAdapter(this, comments);
        commentsListView.setAdapter(commentAdapter);

        findViewById(R.id.commentsBackButton).setOnClickListener(v -> finish());
        postCommentButton.setOnClickListener(v -> postComment());
        commentsListView.setOnItemClickListener((parent, view, position, id) -> {
            if (!canDeleteComments || position < 0 || position >= comments.size()) {
                return;
            }
            showDeleteCommentDialog(comments.get(position));
        });
    }

    /**
     * Refreshes event access state and reloads comments whenever the screen becomes visible.
     */
    @Override
    protected void onStart() {
        super.onStart();
        loadEventState();
    }

    /**
     * Loads the event, verifies the current user can view it, and determines whether
     * comment deletion should be enabled for this session.
     */
    private void loadEventState() {
        repository.getEventById(eventId)
                .addOnSuccessListener(event -> {
                    FirebaseUser currentUser = auth.getCurrentUser();
                    repository.canUserAccessEvent(
                                    event,
                                    eventId,
                                    currentUser == null ? null : currentUser.getUid()
                            )
                            .addOnSuccessListener(canAccess -> {
                                if (!canAccess) {
                                    Toast.makeText(
                                            CommentsActivity.this,
                                            R.string.private_event_access_denied,
                                            Toast.LENGTH_SHORT
                                    ).show();
                                    finish();
                                    return;
                                }

                                canDeleteComments = currentUser != null
                                        && EventRepository.canManageEvent(event, currentUser.getUid());
                                loadComments();
                            })
                            .addOnFailureListener(exception -> {
                                Log.e(TAG, "Failed to verify event access", exception);
                                Toast.makeText(
                                        CommentsActivity.this,
                                        buildLoadErrorMessage(exception),
                                        Toast.LENGTH_LONG
                                ).show();
                                finish();
                            });
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Failed to load event for comments", exception);
                    Toast.makeText(
                            CommentsActivity.this,
                            buildLoadErrorMessage(exception),
                            Toast.LENGTH_LONG
                    ).show();
                    finish();
                });
    }

    /**
     * Posts a new comment for the current event and refreshes the list on success.
     */
    private void postComment() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, R.string.comments_sign_in_required, Toast.LENGTH_SHORT).show();
            return;
        }

        String commentText = commentInput.getText().toString().trim();
        if (commentText.isEmpty()) {
            Toast.makeText(this, R.string.comments_empty_error, Toast.LENGTH_SHORT).show();
            return;
        }

        repository.addComment(eventId, currentUser, commentText)
                .addOnSuccessListener(unused -> {
                    commentInput.setText("");
                    loadComments();
                    Toast.makeText(this, R.string.comments_posted, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Failed to post comment", exception);
                    Toast.makeText(this, R.string.comments_post_failed, Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Loads all comments for the current event and updates the list and empty state.
     */
    private void loadComments() {
        repository.getComments(eventId)
                .addOnSuccessListener(commentItems -> {
                    comments.clear();
                    comments.addAll(commentItems);
                    commentAdapter.notifyDataSetChanged();
                    updateEmptyState();
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Failed to load comments", exception);
                    Toast.makeText(this, R.string.comments_load_failed, Toast.LENGTH_SHORT).show();
                    comments.clear();
                    commentAdapter.notifyDataSetChanged();
                    updateEmptyState();
                });
    }

    /**
     * Deletes the selected comment and reloads the current comment list.
     *
     * @param comment the comment to remove
     */
    private void deleteComment(CommentItem comment) {
        repository.deleteComment(eventId, comment.getCommentId())
                .addOnSuccessListener(unused -> {
                    loadComments();
                    Toast.makeText(this, R.string.comments_deleted, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Failed to delete comment", exception);
                    Toast.makeText(this, R.string.comments_delete_failed, Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Shows a confirmation dialog before deleting a comment.
     *
     * @param comment the selected comment
     */
    private void showDeleteCommentDialog(CommentItem comment) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.comments_delete_title)
                .setMessage(R.string.comments_delete_message)
                .setNegativeButton(R.string.comments_cancel, null)
                .setPositiveButton(R.string.comments_delete_action, (dialog, which) -> deleteComment(comment))
                .show();
    }

    /**
     * Toggles the empty-state message depending on whether any comments are loaded.
     */
    private void updateEmptyState() {
        boolean hasComments = !comments.isEmpty();
        commentsListView.setVisibility(hasComments ? View.VISIBLE : View.GONE);
        emptyState.setVisibility(hasComments ? View.GONE : View.VISIBLE);
    }

    /**
     * Builds a readable event-load error message for display.
     *
     * @param exception the exception raised during event loading or access verification
     * @return a user-facing error message
     */
    private String buildLoadErrorMessage(Exception exception) {
        if (exception != null && exception.getMessage() != null && !exception.getMessage().trim().isEmpty()) {
            return getString(R.string.failed_to_load_event) + ": " + exception.getMessage().trim();
        }
        return getString(R.string.failed_to_load_event);
    }
}
