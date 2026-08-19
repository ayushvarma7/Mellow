package edu.northeastern.mellow.ui.buddy;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import edu.northeastern.mellow.data.model.BuddyGroup;
import edu.northeastern.mellow.data.model.BuddyRequest;
import edu.northeastern.mellow.data.model.Nudge;
import edu.northeastern.mellow.data.repository.AuthRepository;
import edu.northeastern.mellow.data.repository.BuddyRepository;
import edu.northeastern.mellow.data.util.MellowResult;

@HiltViewModel
public class BuddyViewModel extends ViewModel {

    private final BuddyRepository buddyRepo;
    private final AuthRepository authRepo;

    private final MediatorLiveData<MellowResult<List<BuddyRequest>>> incomingRequests =
            new MediatorLiveData<>();
    private final MediatorLiveData<MellowResult<List<BuddyGroup>>> buddyGroups =
            new MediatorLiveData<>();

    private final MutableLiveData<MellowResult<Void>> sendRequestResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isSending = new MutableLiveData<>(false);

    // Nudge LiveData — posts the sender username whenever a new nudge arrives
    private final MutableLiveData<String> incomingNudgeFrom = new MutableLiveData<>();
    private final Set<String> processedNudgeIds = new HashSet<>();

    private LiveData<MellowResult<List<BuddyRequest>>> requestsSource;
    private LiveData<MellowResult<List<BuddyGroup>>> groupsSource;
    private LiveData<MellowResult<List<Nudge>>> nudgesSource;

    @Inject
    public BuddyViewModel(BuddyRepository buddyRepo, AuthRepository authRepo) {
        this.buddyRepo = buddyRepo;
        this.authRepo  = authRepo;
    }

    /**
     * Call once after auth is confirmed.
     */
    public void startObserving() {
        String uid = authRepo.getCurrentUid();
        if (uid == null) return;

        LiveData<MellowResult<List<BuddyRequest>>> reqSource =
                buddyRepo.observeIncomingRequests(uid);
        if (requestsSource != null) incomingRequests.removeSource(requestsSource);
        requestsSource = reqSource;
        incomingRequests.addSource(reqSource, incomingRequests::setValue);

        LiveData<MellowResult<List<BuddyGroup>>> grpSource =
                buddyRepo.observeBuddyGroups(uid);
        if (groupsSource != null) buddyGroups.removeSource(groupsSource);
        groupsSource = grpSource;
        buddyGroups.addSource(grpSource, buddyGroups::setValue);

        // Observe incoming nudges
        LiveData<MellowResult<List<Nudge>>> nudgeSource =
                buddyRepo.observeIncomingNudges(uid);
        if (nudgesSource != null) nudgesSource = null; // remove handled by GC; source isn't MediatoLiveData
        nudgesSource = nudgeSource;
        nudgesSource.observeForever(result -> {
            if (!result.isSuccess() || result.getData() == null) return;
            for (Nudge nudge : result.getData()) {
                if (!processedNudgeIds.contains(nudge.getId())) {
                    processedNudgeIds.add(nudge.getId());
                    incomingNudgeFrom.postValue(nudge.getSenderUsername());
                    buddyRepo.markNudgeSeen(nudge.getId(), r -> {});
                }
            }
        });
    }

    /**
     * Send a buddy request by username. UI passes the username string the user typed.
     */
    public void sendRequest(String toUsername) {
        if (Boolean.TRUE.equals(isSending.getValue())) return;
        String uid = authRepo.getCurrentUid();
        if (uid == null) return;

        isSending.setValue(true);
        buddyRepo.sendBuddyRequest(uid, toUsername.trim().toLowerCase(), result -> {
            isSending.postValue(false);
            sendRequestResult.postValue(result);
        });
    }

    public void acceptRequest(String requestId) {
        buddyRepo.acceptBuddyRequest(requestId, result -> {
            // Groups LiveData updates automatically via snapshot listener
        });
    }

    public void declineRequest(String requestId) {
        buddyRepo.declineBuddyRequest(requestId, result -> {});
    }

    public void removeBuddy(String groupId) {
        buddyRepo.removeBuddy(groupId, result -> {});
    }

    public void sendNudge(String groupId, String buddyUid) {
        String uid = authRepo.getCurrentUid();
        if (uid == null) return;
        // Fetch sender username from the buddy groups data
        MellowResult<List<BuddyGroup>> groups = buddyGroups.getValue();
        String senderUsername = uid; // fallback
        if (groups != null && groups.isSuccess() && groups.getData() != null) {
            for (BuddyGroup g : groups.getData()) {
                if (g.getId().equals(groupId)) {
                    senderUsername = g.getBuddyUsername(buddyUid); // the OTHER uid's mapping = current user's username
                    break;
                }
            }
        }
        // Simpler: just look up memberUsernames[uid]
        if (groups != null && groups.isSuccess() && groups.getData() != null) {
            for (BuddyGroup g : groups.getData()) {
                if (g.getId().equals(groupId) && g.getMemberUsernames() != null) {
                    String name = g.getMemberUsernames().get(uid);
                    if (name != null) senderUsername = name;
                    break;
                }
            }
        }
        final String finalUsername = senderUsername;
        buddyRepo.sendNudge(groupId, uid, finalUsername, buddyUid, result -> {});
    }

    public LiveData<MellowResult<List<BuddyRequest>>> getIncomingRequests()  { return incomingRequests; }
    public LiveData<MellowResult<List<BuddyGroup>>>   getBuddyGroups()       { return buddyGroups; }
    public LiveData<MellowResult<Void>>               getSendRequestResult() { return sendRequestResult; }
    public LiveData<Boolean>                          getIsSending()         { return isSending; }
    public LiveData<String>                           getIncomingNudgeFrom() { return incomingNudgeFrom; }
}
