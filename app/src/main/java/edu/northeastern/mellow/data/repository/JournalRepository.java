package edu.northeastern.mellow.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import java.util.List;

import edu.northeastern.mellow.data.model.JournalEntry;
import edu.northeastern.mellow.data.util.MellowCallback;
import edu.northeastern.mellow.data.util.MellowResult;

public interface JournalRepository {

    // Lifecycle-aware snapshot listener for recent journal entries.
    LiveData<MellowResult<List<JournalEntry>>> observeRecentJournals(@NonNull String uid);

    // Save a new journal entry and atomically award 5 coins.
    void saveJournal(@NonNull String uid, @NonNull JournalEntry entry,
                     @NonNull MellowCallback<Void> callback);

    // Fetch last `limit` journal entries, ordered by most recent first.
    void getJournalHistory(@NonNull String uid, int limit,
                           @NonNull MellowCallback<List<JournalEntry>> callback);
}
