package edu.northeastern.mellow.ui.onboarding;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import edu.northeastern.mellow.data.repository.AuthRepository;
import edu.northeastern.mellow.data.util.MellowResult;
import edu.northeastern.mellow.data.util.UsernameGenerator;

@HiltViewModel
public class OnboardingViewModel extends ViewModel {

    private final AuthRepository authRepo;

    // Collected across the onboarding steps before the single save at the end.
    private String name;
    private int age;
    private List<String> happyThings;

    private final MutableLiveData<Boolean> isSaving = new MutableLiveData<>(false);
    private final MutableLiveData<MellowResult<Void>> saveResult = new MutableLiveData<>();

    @Inject
    public OnboardingViewModel(AuthRepository authRepo) {
        this.authRepo = authRepo;
    }

    public void setName(String name)                    { this.name = name; }
    public String getName()                             { return name; }
    public void setAge(int age)                         { this.age = age; }
    public int getAge()                                 { return age; }
    public void setHappyThings(List<String> things)     { this.happyThings = things; }
    public List<String> getHappyThings()                { return happyThings; }

    /**
     * Generates a username automatically and saves it with everything collected
     * during onboarding — name, age, the things that make you happy, and goals.
     */
    public void completeOnboarding(List<String> goals) {
        String uid = authRepo.getCurrentUid();
        if (uid == null) return;

        String username = UsernameGenerator.generate();
        isSaving.setValue(true);

        authRepo.completeOnboarding(uid, username, goals, name, age, happyThings, result -> {
            isSaving.postValue(false);
            saveResult.postValue(result);
        });
    }

    public LiveData<Boolean>            getIsSaving()   { return isSaving; }
    public LiveData<MellowResult<Void>> getSaveResult() { return saveResult; }
}
