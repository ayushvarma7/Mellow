package edu.northeastern.mellow.data.repository.impl;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.Random;

import edu.northeastern.mellow.data.mapper.JournalMapper;
import edu.northeastern.mellow.data.mapper.ProgressMapper;
import edu.northeastern.mellow.data.model.JournalEntry;
import edu.northeastern.mellow.data.model.UserProgress;
import edu.northeastern.mellow.data.repository.JournalRepository;
import edu.northeastern.mellow.data.util.MellowCallback;
import edu.northeastern.mellow.data.util.MellowResult;
import edu.northeastern.mellow.domain.engine.GamificationEngine;

@Singleton
public class JournalRepositoryImpl implements JournalRepository {

    private static final String TAG = "JournalRepo";
    private static final int MIN_JOURNAL_COINS = 10;
    private static final int MAX_JOURNAL_COINS = 15;

    private final FirebaseFirestore firestore;
    private final Random random = new Random();

    @Inject
    public JournalRepositoryImpl(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    private CollectionReference journalsRef(String uid) {
        return firestore.collection("users").document(uid).collection("journals");
    }

    private DocumentReference progressRef(String uid) {
        return firestore.collection("users").document(uid)
                .collection("progress").document(uid);
    }

    // --- Observe recent journals ---

    @Override
    public LiveData<MellowResult<List<JournalEntry>>> observeRecentJournals(@NonNull String uid) {
        Query query = journalsRef(uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(20);
        return new JournalLiveData(query);
    }

    private static class JournalLiveData extends LiveData<MellowResult<List<JournalEntry>>> {

        private final Query query;
        private ListenerRegistration registration;

        JournalLiveData(Query query) {
            this.query = query;
        }

        @Override
        protected void onActive() {
            registration = query.addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    Log.e("JournalLiveData", "Snapshot error: " + error.getMessage());
                    setValue(MellowResult.error(error, error.getMessage()));
                    return;
                }
                List<JournalEntry> entries = new ArrayList<>();
                if (snapshots != null) {
                    snapshots.getDocuments().forEach(doc -> {
                        JournalEntry entry = JournalMapper.journalFromSnapshot(doc);
                        if (entry != null) entries.add(entry);
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

    // --- Save journal (atomic with coin award) ---

    @Override
    public void saveJournal(@NonNull String uid, @NonNull JournalEntry entry,
                            @NonNull MellowCallback<Void> callback) {
        DocumentReference journalRef = journalsRef(uid).document();

        Log.d(TAG, "Attempting to save journal for uid: " + uid);
        Log.d(TAG, "Journal content: " + entry.getContent());
        Log.d(TAG, "Journal path: " + journalRef.getPath());

        // Calculate random coin reward (10-15)
        int coinsEarned = MIN_JOURNAL_COINS + random.nextInt(MAX_JOURNAL_COINS - MIN_JOURNAL_COINS + 1);

        // First, just try to save the journal without the transaction
        journalRef.set(JournalMapper.journalToMap(entry))
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "✅ Journal saved successfully to: " + journalRef.getPath());

                    // Now update coins separately
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
                                    Log.d(TAG, "✅ Coins awarded: " + coinsEarned);
                                    callback.onResult(MellowResult.success(null));
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "❌ Failed to award coins: " + e.getMessage());
                                    // Still return success since journal was saved
                                    callback.onResult(MellowResult.success(null));
                                });
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to save journal: " + e.getMessage(), e);
                    callback.onResult(MellowResult.error(e, e.getMessage()));
                });
    }

    // --- Journal history ---

    @Override
    public void getJournalHistory(@NonNull String uid, int limit,
                                   @NonNull MellowCallback<List<JournalEntry>> callback) {
        journalsRef(uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(query -> {
                    List<JournalEntry> entries = new ArrayList<>();
                    query.getDocuments().forEach(doc -> {
                        JournalEntry entry = JournalMapper.journalFromSnapshot(doc);
                        if (entry != null) entries.add(entry);
                    });
                    callback.onResult(MellowResult.success(entries));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load journal history: " + e.getMessage(), e);
                    callback.onResult(MellowResult.error(e, e.getMessage()));
                });
    }
}
