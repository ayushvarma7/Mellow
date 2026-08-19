package edu.northeastern.mellow.data.repository.impl;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.inject.Inject;
import javax.inject.Singleton;

import edu.northeastern.mellow.data.mapper.ProgressMapper;
import edu.northeastern.mellow.data.model.CheckIn;
import edu.northeastern.mellow.data.model.CheckInResult;
import edu.northeastern.mellow.data.model.CheckInType;
import edu.northeastern.mellow.data.model.UserProgress;
import edu.northeastern.mellow.data.repository.ProgressRepository;
import edu.northeastern.mellow.data.util.DateUtils;
import edu.northeastern.mellow.data.util.MellowCallback;
import edu.northeastern.mellow.data.util.MellowResult;
import edu.northeastern.mellow.domain.engine.GamificationEngine;

@Singleton
public class ProgressRepositoryImpl implements ProgressRepository {

    private static final String TAG = "ProgressRepo";

    private final FirebaseFirestore firestore;
    private final Random random = new Random();

    @Inject
    public ProgressRepositoryImpl(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    private DocumentReference progressRef(String uid) {
        return firestore.collection("users").document(uid)
                .collection("progress").document(uid);
    }

    private int getCoinsForCheckIn(@NonNull CheckInType type) {
        String typeName = type.name().toUpperCase();

        if (typeName.contains("BREATH")) {
            return 13;
        }

        return 3;
    }

    @Override
    public LiveData<MellowResult<UserProgress>> observeProgress(@NonNull String uid) {
        return new ProgressLiveData(progressRef(uid));
    }

    private static class ProgressLiveData extends LiveData<MellowResult<UserProgress>> {

        private final DocumentReference ref;
        private ListenerRegistration registration;

        ProgressLiveData(DocumentReference ref) {
            this.ref = ref;
        }

        @Override
        protected void onActive() {
            registration = ref.addSnapshotListener((snapshot, error) -> {
                if (error != null) {
                    Log.e("ProgressLiveData", "Snapshot error: " + error.getMessage());
                    setValue(MellowResult.error(error, error.getMessage()));
                    return;
                }
                UserProgress progress = ProgressMapper.progressFromSnapshot(snapshot);
                setValue(MellowResult.success(progress != null ? progress : new UserProgress()));
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

    @Override
    public void initializeProgress(@NonNull String uid, @NonNull MellowCallback<Void> callback) {
        DocumentReference ref = progressRef(uid);

        ref.get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Log.d(TAG, "Progress document already exists for " + uid);
                        callback.onResult(MellowResult.success(null));
                    } else {
                        ref.set(ProgressMapper.progressToMap(new UserProgress()))
                                .addOnSuccessListener(v -> {
                                    Log.d(TAG, "Progress initialized for " + uid);
                                    callback.onResult(MellowResult.success(null));
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to initialize progress: " + e.getMessage());
                                    callback.onResult(MellowResult.error(e, e.getMessage()));
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check progress existence: " + e.getMessage());
                    callback.onResult(MellowResult.error(e, e.getMessage()));
                });
    }

    @Override
    public void recordCheckIn(@NonNull String uid, @NonNull CheckInType type,
                              long durationMs, @NonNull MellowCallback<CheckInResult> callback) {

        DocumentReference ref = progressRef(uid);
        String today = DateUtils.today();
        int coinsEarned = getCoinsForCheckIn(type);

        firestore.runTransaction(transaction -> {
            UserProgress current = ProgressMapper.progressFromSnapshot(transaction.get(ref));
            if (current == null) current = new UserProgress();

            GamificationEngine.StreakUpdate streakUpdate = GamificationEngine.calculateStreakUpdate(
                    current.getLastCheckInDate(), today,
                    current.getCurrentStreakDays(), current.getLongestStreakDays(),
                    current.isStreakGracePeriod()
            );

            GamificationEngine.ContainerUpdate containerUpdate = GamificationEngine.calculateContainerUpdate(
                    current.getCurrentContainerCoins(), current.getContainerCapacity(),
                    current.getContainersOpened(), coinsEarned, random
            );

            UserProgress updated = GamificationEngine.applyUpdates(
                    current, today, streakUpdate, containerUpdate, coinsEarned
            );

            CheckInResult result = GamificationEngine.buildCheckInResult(
                    coinsEarned, current.getTotalCoins(), containerUpdate, streakUpdate,
                    current.getContainerCapacity()
            );

            transaction.set(ref, ProgressMapper.progressToMap(updated));
            return result;

        }).addOnSuccessListener(checkInResult -> {
            Log.d(TAG, "Check-in recorded. Coins earned: " + checkInResult.getCoinsEarned()
                    + " Reward: " + checkInResult.isRewardUnlocked());

            CheckIn checkIn = new CheckIn(
                    "", System.currentTimeMillis(), type,
                    durationMs, checkInResult.getCoinsEarned(), checkInResult.isRewardUnlocked()
            );

            firestore.collection("users").document(uid)
                    .collection("checkins")
                    .add(ProgressMapper.checkInToMap(checkIn));

            callback.onResult(MellowResult.success(checkInResult));

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Check-in transaction failed: " + e.getMessage(), e);
            callback.onResult(MellowResult.error(e, e.getMessage()));
        });
    }

    @Override
    public void getCheckInHistory(@NonNull String uid, int limit,
                                  @NonNull MellowCallback<List<CheckIn>> callback) {
        firestore.collection("users").document(uid).collection("checkins")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(query -> {
                    List<CheckIn> checkIns = new ArrayList<>();
                    query.getDocuments().forEach(doc -> {
                        CheckIn c = ProgressMapper.checkInFromSnapshot(doc);
                        if (c != null) checkIns.add(c);
                    });
                    callback.onResult(MellowResult.success(checkIns));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load history: " + e.getMessage());
                    callback.onResult(MellowResult.error(e, e.getMessage()));
                });
    }
}