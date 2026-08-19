package edu.northeastern.mellow.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import java.util.List;

import edu.northeastern.mellow.data.model.BuddyGroup;
import edu.northeastern.mellow.data.model.BuddyRequest;
import edu.northeastern.mellow.data.model.Nudge;
import edu.northeastern.mellow.data.util.MellowCallback;
import edu.northeastern.mellow.data.util.MellowResult;

public interface BuddyRepository {

    // Lifecycle-aware listener for incoming pending requests.
    LiveData<MellowResult<List<BuddyRequest>>> observeIncomingRequests(@NonNull String uid);

    // Lifecycle-aware listener for the user's active buddy groups.
    LiveData<MellowResult<List<BuddyGroup>>> observeBuddyGroups(@NonNull String uid);

    // Send a request to a user by their username.
    // Looks up the username → uid mapping, then writes the request doc.
    void sendBuddyRequest(@NonNull String fromUid, @NonNull String toUsername,
                          @NonNull MellowCallback<Void> callback);

    // Accept a request: atomically updates request status + creates a BuddyGroup.
    void acceptBuddyRequest(@NonNull String requestId,
                            @NonNull MellowCallback<Void> callback);

    // Decline a request: updates status to declined.
    void declineBuddyRequest(@NonNull String requestId,
                             @NonNull MellowCallback<Void> callback);

    // Remove a buddy: deletes the buddy group document.
    void removeBuddy(@NonNull String groupId,
                     @NonNull MellowCallback<Void> callback);

    // Send a nudge to a buddy. Writes to the top-level nudges collection.
    void sendNudge(@NonNull String groupId,
                   @NonNull String senderUid,
                   @NonNull String senderUsername,
                   @NonNull String receiverUid,
                   @NonNull MellowCallback<Void> callback);

    // Lifecycle-aware stream of unseen incoming nudges for a user.
    LiveData<MellowResult<List<Nudge>>> observeIncomingNudges(@NonNull String receiverUid);

    // Mark a nudge as seen so it doesn't re-trigger vibration.
    void markNudgeSeen(@NonNull String nudgeId, @NonNull MellowCallback<Void> callback);
}
