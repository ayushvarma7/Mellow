package edu.northeastern.mellow.ui.auth;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.AuthCredential;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import edu.northeastern.mellow.data.model.MellowUser;
import edu.northeastern.mellow.data.repository.AuthRepository;
import edu.northeastern.mellow.data.util.MellowResult;

@HiltViewModel
public class AuthViewModel extends ViewModel {

    public enum Destination { MAIN, ONBOARDING }

    private final AuthRepository authRepository;

    private final MutableLiveData<MellowResult<MellowUser>> signInResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Destination> destination = new MutableLiveData<>();

    @Inject
    public AuthViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public LiveData<MellowUser> getCurrentUser() {
        return authRepository.observeCurrentUser();
    }

    public LiveData<MellowResult<MellowUser>> getSignInResult() { return signInResult; }
    public LiveData<Boolean>     getIsLoading()   { return isLoading; }
    public LiveData<Destination> getDestination() { return destination; }

    public boolean isSignedIn() {
        return authRepository.isSignedIn();
    }

    public void signInWithGoogle(@NonNull AuthCredential credential) {
        isLoading.setValue(true);
        authRepository.signInWithGoogle(credential, result -> {
            isLoading.postValue(false);
            signInResult.postValue(result);
        });
    }

    public void signInAsGuest() {
        isLoading.setValue(true);
        authRepository.signInAsGuest(result -> {
            isLoading.postValue(false);
            signInResult.postValue(result);
        });
    }

    /**
     * Checks Firestore to decide where to send the user after sign-in.
     * Posts ONBOARDING if no username is set yet, MAIN otherwise.
     */
    public void checkOnboardingStatus() {
        String uid = authRepository.getCurrentUid();
        if (uid == null) return;

        authRepository.checkOnboardingStatus(uid, result -> {
            if (result.isSuccess() && Boolean.TRUE.equals(result.getData())) {
                destination.postValue(Destination.ONBOARDING);
            } else {
                destination.postValue(Destination.MAIN);
            }
        });
    }

    public void signOut() {
        authRepository.signOut(result -> {});
    }
}
