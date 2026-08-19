package edu.northeastern.mellow.data.repository.impl;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import edu.northeastern.mellow.data.mapper.BuddyMapper;
import edu.northeastern.mellow.data.model.BuddyGroup;
import edu.northeastern.mellow.data.model.BuddyRequest;
import edu.northeastern.mellow.data.model.Nudge;
import edu.northeastern.mellow.data.repository.BuddyRepository;
import edu.northeastern.mellow.data.util.MellowCallback;
import edu.northeastern.mellow.data.util.MellowResult;

@Singleton
public class BuddyRepositoryImpl implements BuddyRepository {

    private static final String TAG = "BuddyRepo";

    private final FirebaseFirestore firestore;

    @Inject
    public BuddyRepositoryImpl(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    // --- Observe incoming requests ---

    @Override
    public LiveData<MellowResult<List<BuddyRequest>>> observeIncomingRequests(@NonNull String uid) {
        return new RequestsLiveData(firestore, uid);
    }

    private static class RequestsLiveData extends LiveData<MellowResult<List<BuddyRequest>>> {

        private final FirebaseFirestore firestore;
        private final String uid;
        private ListenerRegistration registration;

        RequestsLiveData(FirebaseFirestore firestore, String uid) {
            this.firestore = firestore;
            this.uid = uid;
        }

        @Override
        protected void onActive() {
            registration = firestore.collection("buddyRequests")
                    .whereEqualTo("toUid", uid)
                    .addSnapshotListener((snapshots, error) -> {
                        if (error != null) {
                            Log.e("RequestsLiveData", "Snapshot error: " + error.getMessage());
                            setValue(MellowResult.error(error, error.getMessage()));
                            return;
                        }
                        List<BuddyRequest> requests = new ArrayList<>();
                        if (snapshots != null) {
                            snapshots.getDocuments().forEach(doc -> {
                                BuddyRequest r = BuddyMapper.requestFromSnapshot(doc);
                                // Only surface pending ones
                                if (r != null && r.getStatus() == BuddyRequest.Status.PENDING) {
                                    requests.add(r);
                                }
                            });
                        }
                        setValue(MellowResult.success(requests));
                    });
        }

        @Override
        protected void onInactive() {
            if (registration != null) {
                registration.remove();
                registration = null;
            }
        }
    }

    // --- Observe buddy groups ---

    @Override
    public LiveData<MellowResult<List<BuddyGroup>>> observeBuddyGroups(@NonNull String uid) {
        return new GroupsLiveData(firestore, uid);
    }

    private static class GroupsLiveData extends LiveData<MellowResult<List<BuddyGroup>>> {

        private final FirebaseFirestore firestore;
        private final String uid;
        private ListenerRegistration registration;

        GroupsLiveData(FirebaseFirestore firestore, String uid) {
            this.firestore = firestore;
            this.uid = uid;
        }

        @Override
        protected void onActive() {
            registration = firestore.collection("buddyGroups")
                    .whereArrayContains("members", uid)
                    .addSnapshotListener((snapshots, error) -> {
                        if (error != null) {
                            Log.e("GroupsLiveData", "Snapshot error: " + error.getMessage());
                            setValue(MellowResult.error(error, error.getMessage()));
                            return;
                        }
                        List<BuddyGroup> groups = new ArrayList<>();
                        if (snapshots != null) {
                            snapshots.getDocuments().forEach(doc -> {
                                BuddyGroup g = BuddyMapper.groupFromSnapshot(doc);
                                if (g != null) groups.add(g);
                            });
                        }
                        setValue(MellowResult.success(groups));
                    });
        }

        @Override
        protected void onInactive() {
            if (registration != null) {
                registration.remove();
                registration = null;
            }
        }
    }

    // --- Send request ---

    @Override
    public void sendBuddyRequest(@NonNull String fromUid, @NonNull String toUsername,
                                 @NonNull MellowCallback<Void> callback) {
        // Step 1: resolve toUsername → toUid
        firestore.collection("usernames").document(toUsername).get()
                .addOnSuccessListener(usernameDoc -> {
                    if (!usernameDoc.exists()) {
                        callback.onResult(MellowResult.error(
                                new Exception("User not found"),
                                "No user found with username @" + toUsername));
                        return;
                    }

                    String toUid = usernameDoc.getString("uid");

                    if (toUid == null || toUid.equals(fromUid)) {
                        callback.onResult(MellowResult.error(
                                new Exception("Invalid target"),
                                toUid == null ? "Invalid user" : "You can't add yourself as a buddy"));
                        return;
                    }

                    // Step 2: get sender's username from their user doc
                    firestore.collection("users").document(fromUid).get()
                            .addOnSuccessListener(fromDoc -> {
                                String fromUsername = fromDoc.getString("username");
                                if (fromUsername == null) fromUsername = fromUid;

                                // Step 3: write the request
                                Map<String, Object> data = new HashMap<>();
                                data.put("fromUid",      fromUid);
                                data.put("fromUsername", fromUsername);
                                data.put("toUid",        toUid);
                                data.put("toUsername",   toUsername);
                                data.put("status",       BuddyRequest.Status.PENDING.getValue());
                                data.put("createdAt",    FieldValue.serverTimestamp());

                                firestore.collection("buddyRequests").add(data)
                                        .addOnSuccessListener(ref -> {
                                            Log.d(TAG, "Buddy request sent to @" + toUsername);
                                            callback.onResult(MellowResult.success(null));
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "sendBuddyRequest failed: " + e.getMessage(), e);
                                            callback.onResult(MellowResult.error(e, e.getMessage()));
                                        });
                            })
                            .addOnFailureListener(e ->
                                    callback.onResult(MellowResult.error(e, e.getMessage())));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Username lookup failed: " + e.getMessage(), e);
                    callback.onResult(MellowResult.error(e, e.getMessage()));
                });
    }

    // --- Accept request ---

    @Override
    public void acceptBuddyRequest(@NonNull String requestId,
                                   @NonNull MellowCallback<Void> callback) {
        DocumentReference requestRef = firestore.collection("buddyRequests").document(requestId);

        requestRef.get().addOnSuccessListener(requestDoc -> {
            BuddyRequest request = BuddyMapper.requestFromSnapshot(requestDoc);
            if (request == null) {
                callback.onResult(MellowResult.error(
                        new Exception("Request not found"), "Request not found"));
                return;
            }

            // Build the new group
            List<String> members = Arrays.asList(request.getFromUid(), request.getToUid());
            Map<String, String> memberUsernames = new HashMap<>();
            memberUsernames.put(request.getFromUid(), request.getFromUsername());
            memberUsernames.put(request.getToUid(),   request.getToUsername());

            Map<String, Object> groupData = new HashMap<>();
            groupData.put("members",         members);
            groupData.put("memberUsernames", memberUsernames);
            groupData.put("createdAt",       FieldValue.serverTimestamp());

            // Atomic: update request status + create group
            WriteBatch batch = firestore.batch();
            batch.update(requestRef, "status", BuddyRequest.Status.ACCEPTED.getValue());
            batch.set(firestore.collection("buddyGroups").document(), groupData);

            batch.commit()
                    .addOnSuccessListener(v -> {
                        Log.d(TAG, "Buddy request accepted: " + requestId);
                        callback.onResult(MellowResult.success(null));
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "acceptBuddyRequest failed: " + e.getMessage(), e);
                        callback.onResult(MellowResult.error(e, e.getMessage()));
                    });

        }).addOnFailureListener(e ->
                callback.onResult(MellowResult.error(e, e.getMessage())));
    }

    // --- Decline request ---

    @Override
    public void declineBuddyRequest(@NonNull String requestId,
                                    @NonNull MellowCallback<Void> callback) {
        firestore.collection("buddyRequests").document(requestId)
                .update("status", BuddyRequest.Status.DECLINED.getValue())
                .addOnSuccessListener(v -> {
                    Log.d(TAG, "Buddy request declined: " + requestId);
                    callback.onResult(MellowResult.success(null));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "declineBuddyRequest failed: " + e.getMessage(), e);
                    callback.onResult(MellowResult.error(e, e.getMessage()));
                });
    }

    // --- Remove buddy ---

    @Override
    public void removeBuddy(@NonNull String groupId,
                            @NonNull MellowCallback<Void> callback) {
        firestore.collection("buddyGroups").document(groupId)
                .delete()
                .addOnSuccessListener(v -> {
                    Log.d(TAG, "Buddy group removed: " + groupId);
                    callback.onResult(MellowResult.success(null));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "removeBuddy failed: " + e.getMessage(), e);
                    callback.onResult(MellowResult.error(e, e.getMessage()));
                });
    }

    // --- Nudge ---

    @Override
    public void sendNudge(@NonNull String groupId,
                          @NonNull String senderUid,
                          @NonNull String senderUsername,
                          @NonNull String receiverUid,
                          @NonNull MellowCallback<Void> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("groupId",        groupId);
        data.put("senderUid",      senderUid);
        data.put("senderUsername", senderUsername);
        data.put("receiverUid",    receiverUid);
        data.put("seen",           false);
        data.put("timestamp",      FieldValue.serverTimestamp());

        firestore.collection("nudges").add(data)
                .addOnSuccessListener(ref -> {
                    Log.d(TAG, "Nudge sent: " + ref.getId());
                    callback.onResult(MellowResult.success(null));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "sendNudge failed: " + e.getMessage(), e);
                    callback.onResult(MellowResult.error(e, e.getMessage()));
                });
    }

    @Override
    public LiveData<MellowResult<List<Nudge>>> observeIncomingNudges(@NonNull String receiverUid) {
        return new NudgesLiveData(firestore, receiverUid);
    }

    @Override
    public void markNudgeSeen(@NonNull String nudgeId,
                              @NonNull MellowCallback<Void> callback) {
        firestore.collection("nudges").document(nudgeId)
                .update("seen", true)
                .addOnSuccessListener(v -> callback.onResult(MellowResult.success(null)))
                .addOnFailureListener(e -> callback.onResult(MellowResult.error(e, e.getMessage())));
    }

    private static class NudgesLiveData extends LiveData<MellowResult<List<Nudge>>> {

        private final FirebaseFirestore firestore;
        private final String receiverUid;
        private ListenerRegistration registration;

        NudgesLiveData(FirebaseFirestore firestore, String receiverUid) {
            this.firestore   = firestore;
            this.receiverUid = receiverUid;
        }

        @Override
        protected void onActive() {
            registration = firestore.collection("nudges")
                    .whereEqualTo("receiverUid", receiverUid)
                    .whereEqualTo("seen", false)
                    .addSnapshotListener((snapshots, error) -> {
                        if (error != null) {
                            setValue(MellowResult.error(error, error.getMessage()));
                            return;
                        }
                        List<Nudge> nudges = new ArrayList<>();
                        if (snapshots != null) {
                            for (var doc : snapshots.getDocuments()) {
                                Nudge n = new Nudge();
                                n.setId(doc.getId());
                                n.setGroupId(doc.getString("groupId"));
                                n.setSenderUid(doc.getString("senderUid"));
                                n.setSenderUsername(doc.getString("senderUsername"));
                                n.setReceiverUid(doc.getString("receiverUid"));
                                n.setSeen(Boolean.TRUE.equals(doc.getBoolean("seen")));
                                n.setTimestamp(doc.getTimestamp("timestamp"));
                                nudges.add(n);
                            }
                        }
                        setValue(MellowResult.success(nudges));
                    });
        }

        @Override
        protected void onInactive() {
            if (registration != null) {
                registration.remove();
                registration = null;
            }
        }
    }
}
