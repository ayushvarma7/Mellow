package edu.northeastern.mellow.ui.journal;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import edu.northeastern.mellow.data.model.JournalEntry;
import edu.northeastern.mellow.data.repository.AuthRepository;
import edu.northeastern.mellow.data.repository.JournalRepository;
import edu.northeastern.mellow.data.util.DateUtils;
import edu.northeastern.mellow.data.util.MellowResult;

@HiltViewModel
public class JournalViewModel extends ViewModel {

    private static final String TAG = "JournalViewModel";

    private final JournalRepository journalRepo;
    private final AuthRepository authRepo;

    private final MediatorLiveData<MellowResult<List<JournalEntry>>> recentJournals = new MediatorLiveData<>();
    private final MutableLiveData<Boolean> isSaving = new MutableLiveData<>(false);

    private LiveData<MellowResult<List<JournalEntry>>> currentSource;

    @Inject
    public JournalViewModel(JournalRepository journalRepo, AuthRepository authRepo) {
        this.journalRepo = journalRepo;
        this.authRepo = authRepo;
    }

    /**
     * Call once after auth is confirmed to start listening to recent journals.
     */
    public void startObserving() {
        String uid = authRepo.getCurrentUid();
        if (uid == null) return;

        LiveData<MellowResult<List<JournalEntry>>> source = journalRepo.observeRecentJournals(uid);
        if (currentSource != null) {
            recentJournals.removeSource(currentSource);
        }
        currentSource = source;
        recentJournals.addSource(source, recentJournals::setValue);
    }

    /**
     * Save a journal entry with optional title, required content, and mood score (1-5).
     */
    /** Save a journal entry dated today. */
    public void saveJournal(@Nullable String title, String content, int moodScore) {
        saveJournal(title, content, moodScore, null);
    }

    /**
     * Save a journal entry for a specific day (YYYY-MM-DD). null = today.
     * Future dates are clamped to today — past days can be back-filled.
     */
    public void saveJournal(@Nullable String title, String content, int moodScore, @Nullable String date) {
        if (Boolean.TRUE.equals(isSaving.getValue())) return;

        String uid = authRepo.getCurrentUid();
        if (uid == null) {
            Log.e(TAG, "Cannot save journal: user not authenticated");
            return;
        }

        String today = DateUtils.today();
        String effective = (date == null) ? today : date;
        java.time.LocalDate parsed = DateUtils.parseDate(effective);
        if (parsed == null || parsed.isAfter(java.time.LocalDate.now())) {
            effective = today;
        }
        long timestamp = effective.equals(today)
                ? System.currentTimeMillis()
                : startOfDayMillis(effective);

        isSaving.setValue(true);
        Log.d(TAG, "Saving journal for uid: " + uid + " date: " + effective);

        JournalEntry entry = new JournalEntry(
                "", timestamp, effective, title, content,
                Math.max(1, Math.min(5, moodScore))
        );

        journalRepo.saveJournal(uid, entry, result -> {
            isSaving.postValue(false);
            if (result.isSuccess()) {
                Log.d(TAG, "Journal saved successfully");
            } else {
                Log.e(TAG, "Failed to save journal: " + result.getMessage());
            }
        });
    }

    private static long startOfDayMillis(String ymd) {
        try {
            return java.time.LocalDate.parse(ymd)
                    .atStartOfDay(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }

    public LiveData<MellowResult<List<JournalEntry>>> getRecentJournals() {
        return recentJournals;
    }

    public LiveData<Boolean> getIsSaving() {
        return isSaving;
    }


        public void insertJournal(JournalEntry entry) {
            if (entry == null) return;

            String uid = authRepo.getCurrentUid();
            if (uid == null) {
                Log.e(TAG, "Cannot insert journal: user not authenticated");
                return;
            }

            isSaving.setValue(true);

            journalRepo.saveJournal(uid, entry, result -> {
                isSaving.postValue(false);
                if (result.isSuccess()) {
                    Log.d(TAG, "Journal inserted successfully");
                } else {
                    Log.e(TAG, "Failed to insert journal: " + result.getMessage());
                }
            });
        }
    }

