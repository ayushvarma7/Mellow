package edu.northeastern.mellow.ui.progress;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import edu.northeastern.mellow.data.model.CheckInResult;
import edu.northeastern.mellow.data.model.CheckInType;
import edu.northeastern.mellow.data.model.UserProgress;
import edu.northeastern.mellow.data.repository.AuthRepository;
import edu.northeastern.mellow.data.repository.ProgressRepository;
import edu.northeastern.mellow.data.util.MellowResult;

@HiltViewModel
public class ProgressViewModel extends ViewModel {

    private final ProgressRepository progressRepo;
    private final AuthRepository authRepo;

    private final MediatorLiveData<MellowResult<UserProgress>> progress = new MediatorLiveData<>();
    private final MutableLiveData<MellowResult<CheckInResult>> lastCheckInResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isCheckingIn = new MutableLiveData<>(false);

    private LiveData<MellowResult<UserProgress>> currentSource;

    @Inject
    public ProgressViewModel(ProgressRepository progressRepo, AuthRepository authRepo) {
        this.progressRepo = progressRepo;
        this.authRepo = authRepo;
    }

    /**
     * Call once after auth is confirmed.
     * Attaches the Firestore snapshot listener for the user's progress.
     */
    public void startObserving() {
        String uid = authRepo.getCurrentUid();
        if (uid == null) return;

        progressRepo.initializeProgress(uid, result -> {
            LiveData<MellowResult<UserProgress>> source = progressRepo.observeProgress(uid);
            if (currentSource != null) {
                progress.removeSource(currentSource);
            }
            currentSource = source;
            progress.addSource(source, progress::setValue);
        });
    }

    /**
     * Performs a generic check-in.
     */
    public void performCheckIn(@NonNull CheckInType type, long durationMs) {
        if (Boolean.TRUE.equals(isCheckingIn.getValue())) return;

        String uid = authRepo.getCurrentUid();
        if (uid == null) return;

        isCheckingIn.setValue(true);

        progressRepo.recordCheckIn(uid, type, durationMs, result -> {
            isCheckingIn.postValue(false);
            lastCheckInResult.postValue(result);
        });
    }

    /**
     * Convenience method for the breathing screen.
     * Replace CheckInType.BREATHING with your actual enum name
     * if your project uses something like BREATH, BREATHING_EXERCISE, etc.
     */
    public void completeBreathingCheckIn(long durationMs) {
        performCheckIn(CheckInType.BREATHING, durationMs);
    }

    /**
     * Re-attaches the progress listener if needed.
     */
    public void refreshProgress() {
        startObserving();
    }

    /**
     * Convenience method for the UI animation layer.
     * Returns current container fill as a float [0.0, 1.0].
     */
    public float getContainerProgressFraction() {
        MellowResult<UserProgress> current = progress.getValue();
        if (current == null || !current.isSuccess() || current.getData() == null) return 0f;

        UserProgress p = current.getData();
        if (p.getContainerCapacity() == 0) return 0f;

        return Math.min(1f, (float) p.getCurrentContainerCoins() / p.getContainerCapacity());
    }

    public LiveData<MellowResult<UserProgress>> getProgress() {
        return progress;
    }

    public LiveData<MellowResult<CheckInResult>> getLastCheckInResult() {
        return lastCheckInResult;
    }

    public LiveData<Boolean> getIsCheckingIn() {
        return isCheckingIn;
    }
}