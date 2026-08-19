package edu.northeastern.mellow.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import java.util.List;

import edu.northeastern.mellow.data.model.MoodEntry;
import edu.northeastern.mellow.data.model.MoodSummary;
import edu.northeastern.mellow.data.util.MellowCallback;
import edu.northeastern.mellow.data.util.MellowResult;

public interface MoodRepository {

    // Lifecycle-aware snapshot listener for the last 7 days of moods.
    LiveData<MellowResult<List<MoodEntry>>> observeRecentMoods(@NonNull String uid);

    // Write a new mood entry to Firestore.
    void logMood(@NonNull String uid, @NonNull MoodEntry entry,
                 @NonNull MellowCallback<Void> callback);

    // Fetch last `limit` mood entries, ordered by most recent first.
    void getMoodHistory(@NonNull String uid, int limit,
                        @NonNull MellowCallback<List<MoodEntry>> callback);

    // Compute this week's MoodSummary with trend vs last week.
    void getWeeklySummary(@NonNull String uid,
                          @NonNull MellowCallback<MoodSummary> callback);
}
