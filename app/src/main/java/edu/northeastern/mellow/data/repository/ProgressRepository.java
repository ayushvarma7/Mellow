package edu.northeastern.mellow.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import java.util.List;

import edu.northeastern.mellow.data.model.CheckIn;
import edu.northeastern.mellow.data.model.CheckInResult;
import edu.northeastern.mellow.data.model.CheckInType;
import edu.northeastern.mellow.data.model.UserProgress;
import edu.northeastern.mellow.data.util.MellowCallback;
import edu.northeastern.mellow.data.util.MellowResult;

public interface ProgressRepository {

    /** Live stream of the user's progress document. Updates in real time. */
    LiveData<MellowResult<UserProgress>> observeProgress(@NonNull String uid);

    /** Creates a default progress document for new users. */
    void initializeProgress(@NonNull String uid, @NonNull MellowCallback<Void> callback);

    /**
     * Records a check-in inside a Firestore transaction.
     * Returns a CheckInResult that drives the UI animations.
     */
    void recordCheckIn(@NonNull String uid, @NonNull CheckInType type,
                       long durationMs, @NonNull MellowCallback<CheckInResult> callback);

    void getCheckInHistory(@NonNull String uid, int limit,
                           @NonNull MellowCallback<List<CheckIn>> callback);
}
