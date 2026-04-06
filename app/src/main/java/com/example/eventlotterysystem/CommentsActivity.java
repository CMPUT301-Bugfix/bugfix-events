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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays and manages comments for a specific event.
 *
 * <p>This activity supports Reddit-style threaded comments with replies,
 * upvotes, downvotes, score-based ordering, and organizer comment deletion.</p>
 */
public class CommentsActivity extends AppCompatActivity {
    private static final String TAG = "CommentsActivity";
    public static final String EVENT_ID = "EVENT_ID";

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private EventRepository repository;

    private String eventId;
    private boolean canDeleteComments;
    private EditText commentInput;
    private TextView emptyState;
    private TextView replyingToText;
    private ListView commentsListView;
    private CommentAdapter commentAdapter;
    private final List<CommentItem> comments = new ArrayList<>();

    private String selectedParentCommentId = "";
    private int selectedReplyDepth = 0;

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
        firestore = FirebaseFirestore.getInstance();
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
        replyingToText = findViewById(R.id.commentsReplyingTo);
        Button postCommentButton = findViewById(R.id.commentsPostButton);

        commentAdapter = new CommentAdapter(this, comments, new CommentAdapter.CommentActionListener() {
            /**
             * this updates the comment composer to reply to the selected Comment
             * @param comment
             * the Comment that the user is replying to
             */
            @Override
            public void onReplyClicked(@NonNull CommentItem comment) {
                selectedParentCommentId = comment.getCommentId();
                selectedReplyDepth = comment.getDepth() + 1;
                replyingToText.setText(getString(R.string.comments_replying_to, safeUsername(comment)));
                replyingToText.setVisibility(View.VISIBLE);
                commentInput.requestFocus();
            }

            /**
             * this runs the upvote controller on the selected Comment
             * @param comment
             * the Comment that is being upvoted
             */
            @Override
            public void onUpvoteClicked(@NonNull CommentItem comment) {
                voteOnComment(comment, 1);
            }

            /**
             * this runs the downvote controller on the selected Comment
             * @param comment
             * the Comment that is being downvoted
             */
            @Override
            public void onDownvoteClicked(@NonNull CommentItem comment) {
                voteOnComment(comment, -1);
            }
        });

        commentsListView.setAdapter(commentAdapter);

        findViewById(R.id.commentsBackButton).setOnClickListener(v -> finish());
        postCommentButton.setOnClickListener(v -> postComment());

        commentsListView.setOnItemLongClickListener((parent, view, position, id) -> {
            if (!canDeleteComments || position < 0 || position >= comments.size()) {
                return false;
            }
            showDeleteCommentDialog(comments.get(position));
            return true;
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
                    String currentUid = currentUser == null ? null : currentUser.getUid();
                    if (currentUser == null) {
                        Toast.makeText(
                                CommentsActivity.this,
                                R.string.private_event_access_denied,
                                Toast.LENGTH_SHORT
                        ).show();
                        finish();
                        return;
                    }

                    loadModeratorStateAndComments(event, currentUser, currentUid);
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
     * loads the current user role so admins can moderate comments for any event
     * @param event the event whose comments are being viewed
     * @param currentUser the signed-in user for the current session
     * @param currentUid the signed-in user id
     */
    private void loadModeratorStateAndComments(
            @NonNull EventItem event,
            @NonNull FirebaseUser currentUser,
            @NonNull String currentUid
    ) {
        firestore.collection("users")
                .document(currentUid)
                .get()
                .addOnSuccessListener(userSnapshot -> {
                    boolean isAdmin = "admin".equalsIgnoreCase(userSnapshot.getString("accountType"));
                    if (isAdmin) {
                        canDeleteComments = true;
                        loadComments();
                        return;
                    }

                    loadRegularUserComments(event, currentUid);
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Failed to verify moderator role", exception);
                    Toast.makeText(
                            CommentsActivity.this,
                            buildLoadErrorMessage(exception),
                            Toast.LENGTH_LONG
                    ).show();
                    finish();
                });
    }

    /**
     * applies the normal event-access rules for non-admin users
     * @param event the event whose comments are being loaded
     * @param currentUid the signed-in user id
     */
    private void loadRegularUserComments(@NonNull EventItem event, @NonNull String currentUid) {
        repository.canUserAccessEvent(event, eventId, currentUid)
                .addOnSuccessListener(canAccess -> {
                    if (canAccess) {
                        repository.canUserManageEvent(event, currentUid)
                                .addOnSuccessListener(canManage -> {
                                    canDeleteComments = canManage;
                                    loadComments();
                                })
                                .addOnFailureListener(exception -> {
                                    Log.e(TAG, "Failed to verify comment moderation access", exception);
                                    Toast.makeText(
                                            CommentsActivity.this,
                                            buildLoadErrorMessage(exception),
                                            Toast.LENGTH_LONG
                                    ).show();
                                    finish();
                                });
                        return;
                    }

                    repository.hasUserCommentedOnEvent(eventId, currentUid)
                            .addOnSuccessListener(hasCommented -> {
                                if (!hasCommented) {
                                    Toast.makeText(
                                            CommentsActivity.this,
                                            R.string.private_event_access_denied,
                                            Toast.LENGTH_SHORT
                                    ).show();
                                    finish();
                                    return;
                                }

                                repository.canUserManageEvent(event, currentUid)
                                        .addOnSuccessListener(canManage -> {
                                            canDeleteComments = canManage;
                                            loadComments();
                                        })
                                        .addOnFailureListener(exception -> {
                                            Log.e(TAG, "Failed to verify comment moderation access", exception);
                                            Toast.makeText(
                                                    CommentsActivity.this,
                                                    buildLoadErrorMessage(exception),
                                                    Toast.LENGTH_LONG
                                            ).show();
                                            finish();
                                        });
                            })
                            .addOnFailureListener(exception -> {
                                Log.e(TAG, "Failed to verify comment access", exception);
                                Toast.makeText(
                                        CommentsActivity.this,
                                        buildLoadErrorMessage(exception),
                                        Toast.LENGTH_LONG
                                ).show();
                                finish();
                            });
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
    }

    /**
     * Posts either a top-level comment or a reply for the current event.
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

        repository.addComment(
                        eventId,
                        currentUser,
                        commentText,
                        selectedParentCommentId,
                        selectedReplyDepth
                )
                .addOnSuccessListener(unused -> {
                    commentInput.setText("");
                    clearReplyMode();
                    loadComments();
                    Toast.makeText(this, R.string.comments_posted, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Failed to post comment", exception);
                    Toast.makeText(this,
                            "Failed to post comment: " + exception.getClass().getSimpleName() + " - " + exception.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Loads all comments for the current event and refreshes the list and empty state.
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
     * Sends an upvote or downvote for the selected comment.
     *
     * @param comment the comment being voted on
     * @param voteValue the vote value, either 1 or -1
     */
    private void voteOnComment(@NonNull CommentItem comment, int voteValue) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, R.string.comments_sign_in_required, Toast.LENGTH_SHORT).show();
            return;
        }

        repository.voteOnComment(eventId, comment.getCommentId(), currentUser.getUid(), voteValue)
                .addOnSuccessListener(unused -> loadComments())
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Failed to vote on comment", exception);
                    Toast.makeText(this, R.string.comments_vote_failed, Toast.LENGTH_SHORT).show();
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
     * Clears the current reply target and hides the reply label.
     */
    private void clearReplyMode() {
        selectedParentCommentId = "";
        selectedReplyDepth = 0;
        replyingToText.setText("");
        replyingToText.setVisibility(View.GONE);
    }

    /**
     * Returns a safe display name for a comment author.
     *
     * @param comment the comment whose author name should be displayed
     * @return a non-empty display name
     */
    private String safeUsername(CommentItem comment) {
        String username = comment.getUsername();
        if (username == null || username.trim().isEmpty()) {
            return "Unknown user";
        }
        return username;
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
