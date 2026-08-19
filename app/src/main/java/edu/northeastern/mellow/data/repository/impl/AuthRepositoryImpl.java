package edu.northeastern.mellow.data.repository.impl;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import edu.northeastern.mellow.data.model.MellowUser;
import edu.northeastern.mellow.data.repository.AuthRepository;
import edu.northeastern.mellow.data.util.MellowCallback;
import edu.northeastern.mellow.data.util.MellowResult;

@Singleton
public class AuthRepositoryImpl implements AuthRepository {

    private static final String TAG = "AuthRepo";

    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;

    @Inject
    public AuthRepositoryImpl(FirebaseAuth firebaseAuth, FirebaseFirestore firestore) {
        this.firebaseAuth = firebaseAuth;
        this.firestore = firestore;
    }

    @Override
    public LiveData<MellowUser> observeCurrentUser() {
        return new AuthStateLiveData(firebaseAuth);
    }

    @Override
    public boolean isSignedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    @Override
    @Nullable
    public String getCurrentUid() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    // --- Google sign-in ---

    @Override
    public void signInWithGoogle(@NonNull AuthCredential credential,
                                 @NonNull MellowCallback<MellowUser> callback) {
        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        callback.onResult(MellowResult.error(
                                new Exception("No user returned"), "Sign in failed. Please try again."));
                        return;
                    }
                    MellowUser user = MellowUser.fromFirebaseUser(firebaseUser);
                    boolean isNewUser = authResult.getAdditionalUserInfo() != null
                            && Boolean.TRUE.equals(authResult.getAdditionalUserInfo().isNewUser());
                    saveUserToFirestore(firebaseUser, isNewUser,
                            saveResult -> callback.onResult(MellowResult.success(user)));
                })
                .addOnFailureListener(e ->
                        callback.onResult(MellowResult.error(e, "Sign in failed: " + e.getMessage())));
    }

    // --- Guest sign-in ---

    @Override
    public void signInAsGuest(@NonNull MellowCallback<MellowUser> callback) {
        firebaseAuth.signInAnonymously()
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        callback.onResult(MellowResult.error(
                                new Exception("No user returned"), "Guest sign in failed. Please try again."));
                        return;
                    }
                    MellowUser guest = new MellowUser(
                            firebaseUser.getUid(), "Guest", null, null, 0L, null, null);
                    saveGuestToFirestore(firebaseUser.getUid(),
                            saveResult -> callback.onResult(MellowResult.success(guest)));
                })
                .addOnFailureListener(e ->
                        callback.onResult(MellowResult.error(e, "Guest sign in failed: " + e.getMessage())));
    }

    // --- Sign-out ---

    @Override
    public void signOut(@NonNull MellowCallback<Void> callback) {
        firebaseAuth.signOut();
        callback.onResult(MellowResult.success(null));
    }

    // --- Onboarding ---

    @Override
    public void checkOnboardingStatus(@NonNull String uid,
                                      @NonNull MellowCallback<Boolean> callback) {
        firestore.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    String username = doc.getString("username");
                    boolean needsOnboarding = username == null || username.isEmpty();
                    callback.onResult(MellowResult.success(needsOnboarding));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "checkOnboardingStatus failed: " + e.getMessage());
                    callback.onResult(MellowResult.success(true)); // default to onboarding on error
                });
    }

    @Override
    public void checkUsernameAvailable(@NonNull String username,
                                       @NonNull MellowCallback<Boolean> callback) {
        firestore.collection("usernames").document(username).get()
                .addOnSuccessListener(doc ->
                        callback.onResult(MellowResult.success(!doc.exists())))
                .addOnFailureListener(e ->
                        callback.onResult(MellowResult.error(e, e.getMessage())));
    }

    @Override
    public void completeOnboarding(@NonNull String uid, @NonNull String username,
                                   @NonNull List<String> goals,
                                   @NonNull MellowCallback<Void> callback) {
        completeOnboarding(uid, username, goals, null, 0, null, callback);
    }

    @Override
    public void completeOnboarding(@NonNull String uid, @NonNull String username,
                                   @NonNull List<String> goals,
                                   @Nullable String displayName, int age,
                                   @Nullable List<String> happyThings,
                                   @NonNull MellowCallback<Void> callback) {
        WriteBatch batch = firestore.batch();

        // Merge the profile into the user doc (set+merge works even if it doesn't exist yet)
        Map<String, Object> userUpdate = new HashMap<>();
        userUpdate.put("username", username);
        userUpdate.put("goals", goals);
        if (displayName != null && !displayName.trim().isEmpty()) {
            userUpdate.put("displayName", displayName.trim());
        }
        if (age > 0) userUpdate.put("age", age);
        if (happyThings != null && !happyThings.isEmpty()) {
            userUpdate.put("happyThings", happyThings);
        }
        batch.set(firestore.collection("users").document(uid), userUpdate, SetOptions.merge());

        // Claim the username — maps username → uid for buddy lookups
        Map<String, Object> usernameDoc = new HashMap<>();
        usernameDoc.put("uid", uid);
        batch.set(firestore.collection("usernames").document(username), usernameDoc);

        batch.commit()
                .addOnSuccessListener(v -> {
                    Log.d(TAG, "Onboarding complete for " + uid + " (@" + username + ")");
                    callback.onResult(MellowResult.success(null));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "completeOnboarding failed: " + e.getMessage(), e);
                    callback.onResult(MellowResult.error(e, e.getMessage()));
                });
    }

    // --- Fetch full profile ---

    @Override
    @SuppressWarnings("unchecked")
    public void fetchUserProfile(@NonNull String uid,
                                 @NonNull MellowCallback<MellowUser> callback) {
        firestore.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        callback.onResult(MellowResult.error(
                                new Exception("Profile not found"), "Profile not found"));
                        return;
                    }
                    com.google.firebase.Timestamp ts = doc.getTimestamp("createdAt");
                    long createdAt = ts != null ? ts.toDate().getTime() : 0L;

                    String displayName = doc.getString("displayName");
                    List<String> goals = (List<String>) doc.get("goals");

                    MellowUser user = new MellowUser(
                            uid,
                            displayName != null ? displayName : "Mellow User",
                            doc.getString("email"),
                            doc.getString("photoUrl"),
                            createdAt,
                            doc.getString("username"),
                            goals
                    );
                    callback.onResult(MellowResult.success(user));
                })
                .addOnFailureListener(e ->
                        callback.onResult(MellowResult.error(e, e.getMessage())));
    }

    // --- Firestore writes ---

    private void saveUserToFirestore(@NonNull FirebaseUser firebaseUser,
                                     boolean isNewUser,
                                     @NonNull MellowCallback<Void> callback) {
        String uid = firebaseUser.getUid();

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("displayName", firebaseUser.getDisplayName() != null
                ? firebaseUser.getDisplayName() : "Mellow User");
        data.put("email", firebaseUser.getEmail());
        data.put("photoUrl", firebaseUser.getPhotoUrl() != null
                ? firebaseUser.getPhotoUrl().toString() : null);
        data.put("lastActiveAt", FieldValue.serverTimestamp());

        if (isNewUser) {
            data.put("createdAt", FieldValue.serverTimestamp());
            data.put("totalCoins", 0L);
            data.put("currentStreakDays", 0);
            // username is intentionally not set here — set after onboarding
        }

        firestore.collection("users").document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v -> callback.onResult(MellowResult.success(null)))
                .addOnFailureListener(e -> callback.onResult(MellowResult.success(null)));
    }

    private void saveGuestToFirestore(@NonNull String uid, @NonNull MellowCallback<Void> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("displayName", "Guest");
        data.put("isAnonymous", true);
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("lastActiveAt", FieldValue.serverTimestamp());
        data.put("totalCoins", 0L);
        data.put("currentStreakDays", 0);

        firestore.collection("users").document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v -> callback.onResult(MellowResult.success(null)))
                .addOnFailureListener(e -> callback.onResult(MellowResult.success(null)));
    }

    // --- Auth state LiveData ---

    private static class AuthStateLiveData extends LiveData<MellowUser> {

        private final FirebaseAuth firebaseAuth;
        private final FirebaseAuth.AuthStateListener authStateListener;

        AuthStateLiveData(FirebaseAuth firebaseAuth) {
            this.firebaseAuth = firebaseAuth;
            this.authStateListener = auth -> {
                FirebaseUser user = auth.getCurrentUser();
                setValue(user != null ? MellowUser.fromFirebaseUser(user) : null);
            };
        }

        @Override
        protected void onActive() {
            firebaseAuth.addAuthStateListener(authStateListener);
        }

        @Override
        protected void onInactive() {
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
    }
}
