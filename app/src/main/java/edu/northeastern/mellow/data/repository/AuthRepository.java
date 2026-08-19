package edu.northeastern.mellow.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

 import com.google.firebase.auth.AuthCredential;

import java.util.List;

import edu.northeastern.mellow.data.model.MellowUser;
import edu.northeastern.mellow.data.util.MellowCallback;

public interface AuthRepository {

    LiveData<MellowUser> observeCurrentUser();

    boolean isSignedIn();

    @Nullable
    String getCurrentUid();

    void signInWithGoogle(@NonNull AuthCredential credential,
                          @NonNull MellowCallback<MellowUser> callback);

    void signInAsGuest(@NonNull MellowCallback<MellowUser> callback);

    void signOut(@NonNull MellowCallback<Void> callback);

    // Returns true if the user still needs to complete onboarding (no username set yet).
    void checkOnboardingStatus(@NonNull String uid, @NonNull MellowCallback<Boolean> callback);

    // Returns true if the username is not taken.
    void checkUsernameAvailable(@NonNull String username, @NonNull MellowCallback<Boolean> callback);

    // Batch-writes the username + goals to both users/{uid} and usernames/{username}.
    void completeOnboarding(@NonNull String uid, @NonNull String username,
                            @NonNull List<String> goals,
                            @Nullable String displayName, int age,
                            @Nullable List<String> happyThings,
                            @NonNull MellowCallback<Void> callback);

    // Legacy overload — goals only.
    void completeOnboarding(@NonNull String uid, @NonNull String username,
                            @NonNull List<String> goals, @NonNull MellowCallback<Void> callback);

    // Reads the full user profile from Firestore (includes username + goals).
    void fetchUserProfile(@NonNull String uid, @NonNull MellowCallback<MellowUser> callback);
}
