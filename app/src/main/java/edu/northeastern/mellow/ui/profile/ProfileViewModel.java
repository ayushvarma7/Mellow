package edu.northeastern.mellow.ui.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import edu.northeastern.mellow.data.model.MellowUser;
import edu.northeastern.mellow.data.model.UserProgress;
import edu.northeastern.mellow.data.repository.AuthRepository;
import edu.northeastern.mellow.data.repository.ProgressRepository;
import edu.northeastern.mellow.data.util.MellowResult;

@HiltViewModel
public class ProfileViewModel extends ViewModel {

    private final AuthRepository authRepo;
    private final ProgressRepository progressRepo;

    private final MutableLiveData<MellowResult<MellowUser>> userProfile = new MutableLiveData<>();
    private final MediatorLiveData<MellowResult<UserProgress>> progress = new MediatorLiveData<>();

    @Inject
    public ProfileViewModel(AuthRepository authRepo, ProgressRepository progressRepo) {
        this.authRepo = authRepo;
        this.progressRepo = progressRepo;
    }

    public void loadProfile() {
        String uid = authRepo.getCurrentUid();
        if (uid == null) return;

        authRepo.fetchUserProfile(uid, result -> userProfile.postValue(result));

        LiveData<MellowResult<UserProgress>> source = progressRepo.observeProgress(uid);
        progress.addSource(source, progress::setValue);
    }

    public void signOut() {
        authRepo.signOut(result -> {});
    }

    public LiveData<MellowResult<MellowUser>>   getUserProfile() { return userProfile; }
    public LiveData<MellowResult<UserProgress>> getProgress()    { return progress; }
}
