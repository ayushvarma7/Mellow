package edu.northeastern.mellow.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class MellowUser {

    @NonNull  private final String uid;
    @NonNull  private final String displayName;
    @Nullable private final String email;
    @Nullable private final String photoUrl;
    private final long createdAt;
    @Nullable private final String username;      // null until onboarding is complete
    @Nullable private final List<String> goals;   // null until onboarding is complete

    public MellowUser(@NonNull String uid,
                      @NonNull String displayName,
                      @Nullable String email,
                      @Nullable String photoUrl,
                      long createdAt,
                      @Nullable String username,
                      @Nullable List<String> goals) {
        this.uid = uid;
        this.displayName = displayName;
        this.email = email;
        this.photoUrl = photoUrl;
        this.createdAt = createdAt;
        this.username = username;
        this.goals = goals;
    }

    /** Builds a MellowUser right after sign-in. username/goals not available from Firebase Auth. */
    public static MellowUser fromFirebaseUser(@NonNull FirebaseUser firebaseUser) {
        String name = firebaseUser.getDisplayName();
        return new MellowUser(
                firebaseUser.getUid(),
                name != null ? name : "Mellow User",
                firebaseUser.getEmail(),
                firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : null,
                0L, null, null
        );
    }

    public boolean hasCompletedOnboarding() {
        return username != null && !username.isEmpty();
    }

    @NonNull  public String       getUid()         { return uid; }
    @NonNull  public String       getDisplayName() { return displayName; }
    @Nullable public String       getEmail()       { return email; }
    @Nullable public String       getPhotoUrl()    { return photoUrl; }
    public    long                getCreatedAt()   { return createdAt; }
    @Nullable public String       getUsername()    { return username; }
    @Nullable public List<String> getGoals()       { return goals; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MellowUser)) return false;
        return uid.equals(((MellowUser) o).uid);
    }

    @Override
    public int hashCode() { return uid.hashCode(); }
}
