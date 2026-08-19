package edu.northeastern.mellow.ui.mood;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import edu.northeastern.mellow.data.model.MoodEntry;
import edu.northeastern.mellow.data.model.MoodSummary;
import edu.northeastern.mellow.data.repository.AuthRepository;
import edu.northeastern.mellow.data.repository.MoodRepository;
import edu.northeastern.mellow.data.util.DateUtils;
import edu.northeastern.mellow.data.util.MellowResult;

@HiltViewModel
public class MoodViewModel extends ViewModel {

    private final MoodRepository moodRepo;
    private final AuthRepository authRepo;

    private final MediatorLiveData<MellowResult<List<MoodEntry>>> recentMoods = new MediatorLiveData<>();
    private final MutableLiveData<MellowResult<MoodSummary>> weeklySummary = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLogging = new MutableLiveData<>(false);
    private final MutableLiveData<MellowResult<Void>> logResult = new MutableLiveData<>();

    private LiveData<MellowResult<List<MoodEntry>>> currentSource;

    @Inject
    public MoodViewModel(MoodRepository moodRepo, AuthRepository authRepo) {
        this.moodRepo = moodRepo;
        this.authRepo = authRepo;
    }

    /**
     * Call once after auth is confirmed to start listening to recent moods.
     */
    public void startObserving() {
        String uid = authRepo.getCurrentUid();
        if (uid == null) return;

        LiveData<MellowResult<List<MoodEntry>>> source = moodRepo.observeRecentMoods(uid);
        if (currentSource != null) {
            recentMoods.removeSource(currentSource);
        }
        currentSource = source;
        recentMoods.addSource(source, recentMoods::setValue);
    }

    /**
     * Log a mood. score must be 1–5. note and linkedCheckInType are optional.
     */
    /** Log a mood for today. */
    public void logMood(int score, @Nullable String note, @Nullable String linkedCheckInType) {
        logMood(score, null, note, linkedCheckInType);
    }

    /**
     * Log a mood for a specific day (YYYY-MM-DD). null = today. Future dates are
     * clamped to today — you can back-fill past days but never log ahead.
     */
    public void logMood(int score, @Nullable String date,
                        @Nullable String note, @Nullable String linkedCheckInType) {
        if (Boolean.TRUE.equals(isLogging.getValue())) return;

        String uid = authRepo.getCurrentUid();
        if (uid == null) return;

        String today = DateUtils.today();
        String effective = (date == null) ? today : date;
        java.time.LocalDate parsed = DateUtils.parseDate(effective);
        if (parsed == null || parsed.isAfter(java.time.LocalDate.now())) {
            effective = today; // never allow the future
        }
        long timestamp = effective.equals(today)
                ? System.currentTimeMillis()
                : startOfDayMillis(effective);

        isLogging.setValue(true);

        MoodEntry entry = new MoodEntry(
                "", timestamp, effective,
                Math.max(1, Math.min(5, score)),
                note, linkedCheckInType
        );

        moodRepo.logMood(uid, entry, result -> {
            isLogging.postValue(false);
            logResult.postValue(result);
            if (result.isSuccess()) {
                refreshWeeklySummary();
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

    /**
     * Fetches the weekly summary and posts it to weeklySummary LiveData.
     */
    public void refreshWeeklySummary() {
        String uid = authRepo.getCurrentUid();
        if (uid == null) return;

        moodRepo.getWeeklySummary(uid, result ->
                weeklySummary.postValue(result));
    }

    public LiveData<MellowResult<List<MoodEntry>>> getRecentMoods() { return recentMoods; }
    public LiveData<MellowResult<MoodSummary>>     getWeeklySummary() { return weeklySummary; }
    public LiveData<Boolean>                       getIsLogging()    { return isLogging; }
    public LiveData<MellowResult<Void>>            getLogResult()    { return logResult; }
}
