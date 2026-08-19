package edu.northeastern.mellow.data.repository.impl;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import java.util.Random;

import javax.inject.Inject;
import javax.inject.Singleton;

import edu.northeastern.mellow.data.mapper.MoodMapper;
import edu.northeastern.mellow.data.mapper.ProgressMapper;
import edu.northeastern.mellow.data.model.MoodEntry;
import edu.northeastern.mellow.data.model.MoodSummary;
import edu.northeastern.mellow.data.model.UserProgress;
import edu.northeastern.mellow.data.repository.MoodRepository;
import edu.northeastern.mellow.data.util.DateUtils;
import edu.northeastern.mellow.data.util.MellowCallback;
import edu.northeastern.mellow.data.util.MellowResult;
import edu.northeastern.mellow.domain.analytics.MoodAnalytics;
import edu.northeastern.mellow.domain.engine.GamificationEngine;

@Singleton
public class MoodRepositoryImpl implements MoodRepository {

    private static final String TAG = "MoodRepo";

    private final FirebaseFirestore firestore;
    private final Random random = new Random();

    @Inject
    public MoodRepositoryImpl(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    private CollectionReference moodsRef(String uid) {
        return firestore.collection("users").document(uid).collection("moods");
    }

    private DocumentReference progressRef(String uid) {
        return firestore.collection("users").document(uid)
                .collection("progress").document(uid);
    }

    // --- Observe recent moods ---

    @Override
    public LiveData<MellowResult<List<MoodEntry>>> observeRecentMoods(@NonNull String uid) {
        // Listen to the last 7 days ordered by timestamp descending.
        // The date filter is applied in-memory after the snapshot — Firestore
        // compound queries would need a composite index, which we want to avoid.
        Query query = moodsRef(uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50); // fetch enough to cover 7 days even if multiple per day
        return new MoodLiveData(query, 7);
    }

    private static class MoodLiveData extends LiveData<MellowResult<List<MoodEntry>>> {

        private final Query query;
        private final int windowDays;
        private ListenerRegistration registration;

        MoodLiveData(Query query, int windowDays) {
            this.query = query;
            this.windowDays = windowDays;
        }

        @Override
        protected void onActive() {
            LocalDate cutoff = LocalDate.now().minusDays(windowDays - 1L);
            registration = query.addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    Log.e("MoodLiveData", "Snapshot error: " + error.getMessage());
                    setValue(MellowResult.error(error, error.getMessage()));
                    return;
                }
                List<MoodEntry> entries = new ArrayList<>();
                if (snapshots != null) {
                    snapshots.getDocuments().forEach(doc -> {
                        MoodEntry entry = MoodMapper.moodFromSnapshot(doc);
                        if (entry != null) {
                            try {
                                LocalDate entryDate = LocalDate.parse(entry.getDate());
                                if (!entryDate.isBefore(cutoff)) entries.add(entry);
                            } catch (Exception ignored) {}
                        }
                    });
                }
                setValue(MellowResult.success(entries));
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

    // --- Log mood ---

    @Override
    public void logMood(@NonNull String uid, @NonNull MoodEntry entry,
                        @NonNull MellowCallback<Void> callback) {
        // First check if a mood already exists for today
        String today = entry.getDate();

        moodsRef(uid)
                .whereEqualTo("date", today)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Mood already logged for today
                        Log.d(TAG, "Mood already logged for " + today);
                        callback.onResult(MellowResult.error(
                                new IllegalStateException("Already logged"),
                                "You've already logged your mood today!"
                        ));
                        return;
                    }

                    // No existing mood, proceed with logging
                    int coinsEarned = Math.max(1, Math.min(5, entry.getMoodScore()));

                    moodsRef(uid)
                            .add(MoodMapper.moodToMap(entry))
                            .addOnSuccessListener(ref -> {
                                Log.d(TAG, "Mood logged: score=" + entry.getMoodScore());

                                // Award coins based on mood score
                                DocumentReference progressDocRef = progressRef(uid);
                                progressDocRef.get().addOnSuccessListener(snapshot -> {
                                    UserProgress current = ProgressMapper.progressFromSnapshot(snapshot);
                                    if (current == null) current = new UserProgress();

                                    // Use GamificationEngine to properly handle container logic
                                    GamificationEngine.ContainerUpdate containerUpdate =
                                            GamificationEngine.calculateContainerUpdate(
                                                    current.getCurrentContainerCoins(),
                                                    current.getContainerCapacity(),
                                                    current.getContainersOpened(),
                                                    coinsEarned,
                                                    random
                                            );

                                    UserProgress updated = UserProgress.builder(current)
                                            .totalCoins(current.getTotalCoins() + coinsEarned)
                                            .currentContainerCoins(containerUpdate.newContainerCoins)
                                            .containerCapacity(containerUpdate.newCapacity)
                                            .containersOpened(containerUpdate.newContainersOpened)
                                            .build();

                                    progressDocRef.set(ProgressMapper.progressToMap(updated))
                                            .addOnSuccessListener(v -> {
                                                Log.d(TAG, "✅ Coins awarded for mood: " + coinsEarned);
                                                callback.onResult(MellowResult.success(null));
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "❌ Failed to award coins for mood: " + e.getMessage());
                                                // Still return success since mood was saved
                                                callback.onResult(MellowResult.success(null));
                                            });
                                });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to log mood: " + e.getMessage(), e);
                                callback.onResult(MellowResult.error(e, e.getMessage()));
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check existing mood: " + e.getMessage(), e);
                    callback.onResult(MellowResult.error(e, e.getMessage()));
                });
    }

    // --- Mood history ---

    @Override
    public void getMoodHistory(@NonNull String uid, int limit,
                               @NonNull MellowCallback<List<MoodEntry>> callback) {
        moodsRef(uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(query -> {
                    List<MoodEntry> entries = new ArrayList<>();
                    query.getDocuments().forEach(doc -> {
                        MoodEntry entry = MoodMapper.moodFromSnapshot(doc);
                        if (entry != null) entries.add(entry);
                    });
                    callback.onResult(MellowResult.success(entries));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load mood history: " + e.getMessage(), e);
                    callback.onResult(MellowResult.error(e, e.getMessage()));
                });
    }

    // --- Weekly summary ---

    @Override
    public void getWeeklySummary(@NonNull String uid,
                                 @NonNull MellowCallback<MoodSummary> callback) {
        // Fetch 14 days to compute both this week and last week for trend.
        LocalDate today = LocalDate.now();
        LocalDate fourteenDaysAgo = today.minusDays(13);
        Timestamp cutoffTs = new Timestamp(
                new Date(fourteenDaysAgo.toEpochDay() * 86400 * 1000));

        moodsRef(uid)
                .whereGreaterThanOrEqualTo("timestamp", cutoffTs)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(query -> {
                    List<MoodEntry> all = new ArrayList<>();
                    query.getDocuments().forEach(doc -> {
                        MoodEntry entry = MoodMapper.moodFromSnapshot(doc);
                        if (entry != null) all.add(entry);
                    });

                    LocalDate lastWeekEnd = today.minusDays(7);

                    List<MoodEntry> thisWeek = all.stream()
                            .filter(e -> MoodAnalytics.isInWindow(e, today))
                            .collect(Collectors.toList());

                    List<MoodEntry> lastWeek = all.stream()
                            .filter(e -> MoodAnalytics.isInWindow(e, lastWeekEnd))
                            .collect(Collectors.toList());

                    String label = MoodAnalytics.buildWeekLabel(today);
                    MoodSummary summary = MoodAnalytics.computeWeeklySummary(
                            thisWeek, lastWeek, label);
                    callback.onResult(MellowResult.success(summary));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to compute weekly summary: " + e.getMessage(), e);
                    callback.onResult(MellowResult.error(e, e.getMessage()));
                });
    }
}
