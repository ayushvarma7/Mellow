# Mellow — Backend API Guide

**Author:** Ayush Varma (Part 3: Backend + Data Layer)
**Audience:** UI teammates wiring up Fragments/Activities to the data layer
**Stack:** Java · MVVM · Hilt · Firebase Auth · Firestore · Cloud Storage

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [MellowResult\<T\>](#2-mellowresultt)
3. [Auth](#3-auth)
4. [Progress / Gamification](#4-progress--gamification)
5. [Mood Tracking](#5-mood-tracking)
6. [Buddy System](#6-buddy-system)
7. [Voice Notes](#7-voice-notes)
8. [Firestore Data Structure](#8-firestore-data-structure)
9. [Common Patterns](#9-common-patterns)

---

## 1. Architecture Overview

```
Activity / Fragment
      │  observe LiveData
      ▼
  ViewModel   (HiltViewModel — injected, survives rotation)
      │  calls methods / observes LiveData
      ▼
  Repository  (interface — Hilt provides the impl)
      │  reads/writes
      ▼
  Firebase (Auth / Firestore / Cloud Storage)
```

- **ViewModels** hold all LiveData. Activities/Fragments only observe — they never talk to repositories directly.
- **Repositories** are interfaces injected by Hilt. You never instantiate them.
- Every async result is wrapped in `MellowResult<T>` (see §2).
- Real-time Firestore listeners are exposed as `LiveData`. One-shot operations use a `MellowCallback<T>`.
- Call `startObserving()` on a ViewModel **once**, after you have confirmed the user is signed in.

---

## 2. MellowResult\<T\>

`edu.northeastern.mellow.data.util.MellowResult<T>`

All LiveData streams and callbacks carry a `MellowResult<T>`. It has three states:

| State | `isSuccess()` | `isError()` | `isLoading()` | `getData()` | `getMessage()` |
|-------|:---:|:---:|:---:|---|---|
| `SUCCESS` | true | false | false | the payload | null |
| `ERROR` | false | true | false | null | human-readable error string |
| `LOADING` | false | false | true | null | null |

```java
// Checking a result
if (result.isSuccess()) {
    UserProgress p = result.getData(); // may still be null for Void results
} else if (result.isError()) {
    showError(result.getMessage());
}
// getError() returns the raw Throwable — only use for logging
```

**Note:** `getData()` is `@Nullable`. Always null-check before use even on a SUCCESS result (e.g., `MellowResult<Void>` is always null data on success).

---

## 3. Auth

### AuthViewModel

`edu.northeastern.mellow.ui.auth.AuthViewModel`
Hilt-injected. Obtain via `new ViewModelProvider(this).get(AuthViewModel.class)`.

#### Destination enum

```java
public enum Destination { MAIN, ONBOARDING }
```

After sign-in, call `checkOnboardingStatus()`. Observe `getDestination()` and navigate accordingly:
- `ONBOARDING` — user has no username yet; send to the onboarding flow.
- `MAIN` — user is fully set up; send to the main screen.

#### Public methods

| Method | Description |
|--------|-------------|
| `signInWithGoogle(AuthCredential credential)` | Kicks off Google sign-in. Posts result to `getSignInResult()`. |
| `signInAsGuest()` | Firebase anonymous sign-in. Posts result to `getSignInResult()`. |
| `checkOnboardingStatus()` | Checks Firestore for a username. Posts to `getDestination()`. |
| `signOut()` | Signs out silently. No result posted. |
| `isSignedIn()` | Synchronous boolean — safe to call before observing any LiveData. |

#### LiveData

| Getter | Type | Emits |
|--------|------|-------|
| `getSignInResult()` | `LiveData<MellowResult<MellowUser>>` | SUCCESS with the signed-in user, or ERROR |
| `getIsLoading()` | `LiveData<Boolean>` | true while a sign-in call is in flight |
| `getDestination()` | `LiveData<Destination>` | MAIN or ONBOARDING after `checkOnboardingStatus()` |
| `getCurrentUser()` | `LiveData<MellowUser>` | Real-time Firebase Auth user (null when signed out) |

#### Typical sign-in flow

```java
// 1. User taps "Sign in with Google" → get an AuthCredential from Google Sign-In SDK
viewModel.signInWithGoogle(credential);

// 2. Observe result
viewModel.getSignInResult().observe(this, result -> {
    if (result.isSuccess()) {
        viewModel.checkOnboardingStatus();
    } else if (result.isError()) {
        showError(result.getMessage());
    }
});

// 3. Observe destination
viewModel.getDestination().observe(this, dest -> {
    if (dest == AuthViewModel.Destination.ONBOARDING) {
        // navigate to onboarding
    } else {
        // navigate to main
    }
});
```

### AuthRepository (reference)

You will not call this directly, but here are the methods the ViewModel delegates to:

| Method | Params | Callback payload |
|--------|--------|-----------------|
| `observeCurrentUser()` | — | `LiveData<MellowUser>` |
| `isSignedIn()` | — | `boolean` (synchronous) |
| `getCurrentUid()` | — | `@Nullable String` (synchronous) |
| `signInWithGoogle(credential, callback)` | `AuthCredential` | `MellowResult<MellowUser>` |
| `signInAsGuest(callback)` | — | `MellowResult<MellowUser>` |
| `signOut(callback)` | — | `MellowResult<Void>` |
| `checkOnboardingStatus(uid, callback)` | `String uid` | `MellowResult<Boolean>` — `true` = onboarding needed |
| `checkUsernameAvailable(username, callback)` | `String username` | `MellowResult<Boolean>` — `true` = available |
| `completeOnboarding(uid, username, goals, callback)` | `String, String, List<String>` | `MellowResult<Void>` |
| `fetchUserProfile(uid, callback)` | `String uid` | `MellowResult<MellowUser>` |

### MellowUser fields

| Getter | Type | Notes |
|--------|------|-------|
| `getUid()` | `String` | Firebase Auth UID |
| `getDisplayName()` | `String` | From Google / "Mellow User" for guests |
| `getEmail()` | `@Nullable String` | null for guests |
| `getPhotoUrl()` | `@Nullable String` | null for guests |
| `getCreatedAt()` | `long` | epoch ms; 0 when built from Firebase Auth directly |
| `getUsername()` | `@Nullable String` | null until onboarding is complete |
| `getGoals()` | `@Nullable List<String>` | null until onboarding is complete |
| `hasCompletedOnboarding()` | `boolean` | convenience — true when username is non-empty |

---

## 4. Progress / Gamification

### ProgressViewModel

`edu.northeastern.mellow.ui.progress.ProgressViewModel`

#### Setup

```java
// Call once after auth is confirmed
viewModel.startObserving();
```

`startObserving()` ensures the user's progress document exists (safe to call on every launch — uses merge, won't overwrite existing data), then attaches a real-time Firestore listener.

#### Public methods

| Method | Params | Description |
|--------|--------|-------------|
| `startObserving()` | — | Initializes and starts listening. Call once post-auth. |
| `performCheckIn(type, durationMs)` | `CheckInType, long` | Records a check-in. Guarded against double-tap. Posts result to `getLastCheckInResult()`. |
| `getContainerProgressFraction()` | — | Returns `float [0.0, 1.0]` — current container fill. Safe to call any time; returns 0 if data not loaded yet. |

#### LiveData

| Getter | Type | Emits |
|--------|------|-------|
| `getProgress()` | `LiveData<MellowResult<UserProgress>>` | Real-time user gamification state |
| `getLastCheckInResult()` | `LiveData<MellowResult<CheckInResult>>` | Result of the most recent `performCheckIn()` call |
| `getIsCheckingIn()` | `LiveData<Boolean>` | true while a check-in transaction is in flight |

### CheckInType enum

```java
public enum CheckInType {
    BREATHING,   // "breathing"
    GROUNDING,   // "grounding"
    GRATITUDE,   // "gratitude"
    CUSTOM       // "custom" — fallback for anything else
}
```

Pass one of these to `performCheckIn()`. The string stored in Firestore is the lowercase value shown above. Use `CheckInType.fromString(str)` to parse — falls back to `CUSTOM`.

### UserProgress fields

| Getter | Type | Description |
|--------|------|-------------|
| `getTotalCoins()` | `long` | All-time coins earned |
| `getCurrentContainerCoins()` | `long` | Coins in the current container |
| `getContainerCapacity()` | `long` | Capacity of the current container (randomized 5–10) |
| `getContainersOpened()` | `long` | Total containers ever filled |
| `getCurrentStreakDays()` | `int` | Active check-in streak |
| `getLongestStreakDays()` | `int` | All-time best streak |
| `getLastCheckInDate()` | `@Nullable String` | `"YYYY-MM-DD"` format |
| `isStreakGracePeriod()` | `boolean` | true = the grace period has already been used for this streak |

### CheckInResult fields

Delivered via `getLastCheckInResult()` after a successful `performCheckIn()`. Drive all post-check-in animations.

| Getter | Type | Description |
|--------|------|-------------|
| `getCoinsEarned()` | `int` | Coins awarded for this check-in |
| `getNewTotalCoins()` | `long` | Updated all-time total |
| `getContainerProgress()` | `float` | Container fill fraction after this check-in (0.0–1.0) |
| `isRewardUnlocked()` | `boolean` | true = container just filled — show reward animation |
| `isStreakUpdated()` | `boolean` | true = streak count incremented |
| `getNewStreakCount()` | `int` | Streak value to display |
| `isGracePeriodUsed()` | `boolean` | true = streak was saved by grace period |

---

## 5. Mood Tracking

### MoodViewModel

`edu.northeastern.mellow.ui.mood.MoodViewModel`

#### Setup

```java
viewModel.startObserving(); // call once post-auth
viewModel.refreshWeeklySummary(); // call to populate the summary card
```

#### Public methods

| Method | Params | Description |
|--------|--------|-------------|
| `startObserving()` | — | Attaches real-time listener for last 7 days of mood entries. |
| `logMood(score, note, linkedCheckInType)` | `int, @Nullable String, @Nullable String` | Writes a new mood entry. Score is clamped to 1–5. `linkedCheckInType` is the `firestoreValue` string of a `CheckInType` (or null). Automatically calls `refreshWeeklySummary()` on success. |
| `refreshWeeklySummary()` | — | Fetches and posts the weekly summary to `getWeeklySummary()`. |

#### LiveData

| Getter | Type | Emits |
|--------|------|-------|
| `getRecentMoods()` | `LiveData<MellowResult<List<MoodEntry>>>` | Real-time stream of last 7 days of entries, newest first |
| `getWeeklySummary()` | `LiveData<MellowResult<MoodSummary>>` | Updated on demand via `refreshWeeklySummary()` |
| `getIsLogging()` | `LiveData<Boolean>` | true while a write is in flight |

### MoodEntry fields

| Getter | Type | Description |
|--------|------|-------------|
| `getId()` | `String` | Firestore document ID |
| `getTimestamp()` | `long` | epoch ms |
| `getDate()` | `String` | `"YYYY-MM-DD"` |
| `getMoodScore()` | `int` | 1 (very bad) → 5 (very good) |
| `getNote()` | `String` | Nullable free-text note |
| `getLinkedCheckInType()` | `String` | Nullable — which activity preceded this mood log |

### MoodSummary fields

`MoodSummary` is computed locally from Firestore data — it is **not** stored as a document.

| Getter | Type | Description |
|--------|------|-------------|
| `getWeekLabel()` | `String` | Human-readable range, e.g. `"Mar 20 – 26"` |
| `getAverageScore()` | `float` | Mean score for the week |
| `getEntryCount()` | `int` | Number of entries logged this week |
| `getHighestScore()` | `int` | Highest single score this week |
| `getLowestScore()` | `int` | Lowest single score this week |
| `getTrend()` | `float` | `thisWeekAvg - lastWeekAvg`. Positive = improving, negative = declining, 0 = no change or no prior data. |
| `hasEntries()` | `boolean` | Convenience — `entryCount > 0` |
| `isTrendImproving()` | `boolean` | Convenience — `trend > 0` |

---

## 6. Buddy System

### BuddyViewModel

`edu.northeastern.mellow.ui.buddy.BuddyViewModel`

#### Setup

```java
viewModel.startObserving(); // call once post-auth
```

Starts two real-time listeners: one for incoming buddy requests, one for active buddy groups.

#### Public methods

| Method | Params | Description |
|--------|--------|-------------|
| `startObserving()` | — | Attaches Firestore listeners for requests and groups. |
| `sendRequest(toUsername)` | `String` | Looks up the username, writes a pending request. Guarded against double-tap. Result posted to `getSendRequestResult()`. The username is trimmed and lowercased automatically. |
| `acceptRequest(requestId)` | `String` | Accepts a pending request. Atomically creates a `BuddyGroup`. Groups LiveData updates automatically. |
| `declineRequest(requestId)` | `String` | Marks request as declined. |
| `removeBuddy(groupId)` | `String` | Deletes the buddy group document. Groups LiveData updates automatically. |

#### LiveData

| Getter | Type | Emits |
|--------|------|-------|
| `getIncomingRequests()` | `LiveData<MellowResult<List<BuddyRequest>>>` | Real-time list of pending incoming requests |
| `getBuddyGroups()` | `LiveData<MellowResult<List<BuddyGroup>>>` | Real-time list of active buddy groups |
| `getSendRequestResult()` | `LiveData<MellowResult<Void>>` | SUCCESS/ERROR after `sendRequest()` |
| `getIsSending()` | `LiveData<Boolean>` | true while a send is in flight |

### BuddyRequest fields

| Getter | Type | Description |
|--------|------|-------------|
| `getId()` | `String` | Firestore document ID — pass to accept/decline |
| `getFromUid()` | `String` | Sender UID |
| `getFromUsername()` | `String` | Sender username — display this in the UI |
| `getToUid()` | `String` | Recipient UID |
| `getToUsername()` | `String` | Recipient username |
| `getStatus()` | `BuddyRequest.Status` | `PENDING`, `ACCEPTED`, or `DECLINED` |
| `getCreatedAt()` | `long` | epoch ms |

### BuddyGroup fields

| Getter | Type | Description |
|--------|------|-------------|
| `getId()` | `String` | Firestore document ID — pass to `removeBuddy()` and `VoiceNoteActivity` |
| `getMembers()` | `List<String>` | List of member UIDs |
| `getMemberUsernames()` | `Map<String, String>` | uid → username mapping |
| `getCreatedAt()` | `long` | epoch ms |
| `getBuddyUsername(currentUid)` | `String` | Returns the **other** member's username. Pass the logged-in user's UID. Returns `"buddy"` if data is missing. |
| `getBuddyUid(currentUid)` | `String` | Returns the **other** member's UID. Returns `""` if data is missing. |

### Firestore collections

| Collection | Purpose |
|------------|---------|
| `buddyRequests` | One document per request. Queried by `toUid` for incoming requests. |
| `buddyGroups` | One document per active connection. Queried by `members` array-contains for the current user. |

---

## 7. Nudge

A one-tap haptic poke between buddies. No audio, no chat — just a signal that says "hey". Built on Firestore only (free tier).

### How it works

1. User **long-presses** the circular 👋 button on a buddy card in `BuddyActivity`
2. A `Nudge` document is written to the top-level `nudges` collection
3. The receiver's `BuddyViewModel` snapshot listener picks it up, marks it seen, and posts the sender's username to `getIncomingNudgeFrom()`
4. `BuddyActivity` observes that LiveData, calls `VibrationHelper.nudge()`, and shows a toast

The receiver must have the app open (foreground or background) to feel the vibration — no push notifications are used.

### Nudge fields

| Getter | Type | Description |
|--------|------|-------------|
| `getId()` | `String` | Firestore document ID |
| `getGroupId()` | `String` | The buddy group this nudge belongs to |
| `getSenderUid()` | `String` | Sender's UID |
| `getSenderUsername()` | `String` | Sender's username — shown in the toast |
| `getReceiverUid()` | `String` | Receiver's UID — used to query incoming nudges |
| `isSeen()` | `boolean` | false until the receiver's device processes it |
| `getTimestamp()` | `Timestamp` | Server-side write time |

### BuddyRepository nudge methods

| Method | Params | Description |
|--------|--------|-------------|
| `sendNudge(groupId, senderUid, senderUsername, receiverUid, callback)` | `String, String, String, String` | Writes a nudge doc with `seen: false`. |
| `observeIncomingNudges(receiverUid)` | `String` | LiveData snapshot listener — queries `nudges` where `receiverUid == uid` and `seen == false`. |
| `markNudgeSeen(nudgeId, callback)` | `String` | Sets `seen: true`. Called automatically by `BuddyViewModel` after vibrating. |

### BuddyViewModel nudge methods

| Method / Getter | Description |
|-----------------|-------------|
| `sendNudge(groupId, buddyUid)` | Resolves the current user's username from the loaded groups, then calls `sendNudge` on the repository. |
| `getIncomingNudgeFrom()` | `LiveData<String>` — posts the sender's username each time a new unseen nudge arrives. Observe this in `BuddyActivity` to vibrate and show a toast. Deduplication is handled internally via a `HashSet` of processed nudge IDs. |

### VibrationHelper

`edu.northeastern.mellow.data.util.VibrationHelper`

Three distinct haptic patterns — no runtime permission needed beyond `VIBRATE` in the manifest.

| Method | Pattern | When to call |
|--------|---------|--------------|
| `onPress(context)` | 30ms single tick | `ACTION_DOWN` on the nudge button — immediate touch confirmation |
| `onSent(context)` | 60ms → 60ms gap → 120ms | Inside `setOnLongClickListener` — confirms the nudge was dispatched |
| `nudge(context)` | 150ms → 80ms gap → 150ms | When `getIncomingNudgeFrom()` emits — plays on the receiver's device |

### Wiring example

```java
// In BuddyActivity.renderBuddies(), for each buddy card:
View btnNudge = item.findViewById(R.id.btn_nudge);

btnNudge.setOnTouchListener((v, event) -> {
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
        VibrationHelper.onPress(this);
    }
    return false;
});
btnNudge.setOnLongClickListener(v -> {
    VibrationHelper.onSent(this);
    viewModel.sendNudge(group.getId(), group.getBuddyUid(currentUid));
    return true;
});

// Once, in onCreate():
viewModel.getIncomingNudgeFrom().observe(this, senderUsername -> {
    VibrationHelper.nudge(this);
    Toast.makeText(this, "👋 @" + senderUsername + " nudged you!", Toast.LENGTH_SHORT).show();
});
```

### Firestore collection

| Collection | Purpose |
|------------|---------|
| `nudges` | Top-level. Each document is one nudge. Queried by `receiverUid` + `seen == false`. Sender can create; receiver can read and update (mark seen). |

---

## 8. Firestore Data Structure

```
Firestore root
│
├── users/
│   └── {uid}/
│       ├── displayName: String
│       ├── email: String
│       ├── username: String          ← set during onboarding
│       ├── goals: List<String>       ← set during onboarding
│       └── createdAt: Timestamp
│
├── usernames/
│   └── {username}/                   ← used for uniqueness check + uid lookup
│       └── uid: String
│
├── progress/
│   └── {uid}/                        ← one document per user
│       ├── totalCoins: Long
│       ├── currentContainerCoins: Long
│       ├── containerCapacity: Long
│       ├── containersOpened: Long
│       ├── currentStreakDays: Int
│       ├── longestStreakDays: Int
│       ├── lastCheckInDate: String   ← "YYYY-MM-DD"
│       └── streakGracePeriod: Boolean
│
├── checkIns/
│   └── {uid}/
│       └── history/
│           └── {checkInId}/          ← one document per check-in event
│               ├── type: String
│               ├── durationMs: Long
│               └── timestamp: Timestamp
│
├── moods/
│   └── {uid}/
│       └── entries/
│           └── {entryId}/
│               ├── moodScore: Int
│               ├── date: String      ← "YYYY-MM-DD"
│               ├── timestamp: Long
│               ├── note: String
│               └── linkedCheckInType: String
│
├── buddyRequests/
│   └── {requestId}/
│       ├── fromUid: String
│       ├── fromUsername: String
│       ├── toUid: String
│       ├── toUsername: String
│       ├── status: String            ← "pending" | "accepted" | "declined"
│       └── createdAt: Timestamp
│
├── buddyGroups/
│   └── {groupId}/
│       ├── members: List<String>     ← UIDs (array-contains queries)
│       ├── memberUsernames: Map      ← { uid: username }
│       └── createdAt: Timestamp
│
└── nudges/
    └── {nudgeId}/
        ├── groupId: String
        ├── senderUid: String
        ├── senderUsername: String
        ├── receiverUid: String       ← indexed for snapshot queries
        ├── seen: Boolean             ← false until receiver processes it
        └── timestamp: Timestamp
```

---

## 9. Common Patterns

### Observing a LiveData result in an Activity

```java
viewModel.getProgress().observe(this, result -> {
    if (result.isLoading()) {
        progressBar.setVisibility(View.VISIBLE);
        return;
    }
    progressBar.setVisibility(View.GONE);

    if (result.isSuccess() && result.getData() != null) {
        UserProgress p = result.getData();
        streakText.setText(String.valueOf(p.getCurrentStreakDays()));
        coinText.setText(String.valueOf(p.getTotalCoins()));
        containerView.setProgress(viewModel.getContainerProgressFraction());
    } else if (result.isError()) {
        Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
    }
});
```

### Reacting to a one-shot result (e.g., after a check-in)

```java
viewModel.getLastCheckInResult().observe(this, result -> {
    if (result == null) return;
    if (result.isSuccess() && result.getData() != null) {
        CheckInResult r = result.getData();
        if (r.isRewardUnlocked()) {
            // show confetti / container-open animation
        }
        if (r.isStreakUpdated()) {
            streakBadge.setText("🔥 " + r.getNewStreakCount());
        }
    } else if (result.isError()) {
        Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
    }
});

// Triggered by a button click:
checkInButton.setOnClickListener(v ->
    viewModel.performCheckIn(CheckInType.BREATHING, 120_000L));
```

### Disabling a button while an operation is in flight

```java
viewModel.getIsCheckingIn().observe(this, inFlight -> {
    checkInButton.setEnabled(!inFlight);
    checkInButton.setAlpha(inFlight ? 0.5f : 1f);
});
```

### Sending a buddy request and showing feedback

```java
sendButton.setOnClickListener(v -> {
    String username = usernameInput.getText().toString().trim();
    if (!username.isEmpty()) {
        viewModel.sendRequest(username);
    }
});

viewModel.getSendRequestResult().observe(this, result -> {
    if (result == null) return;
    if (result.isSuccess()) {
        Toast.makeText(this, "Request sent!", Toast.LENGTH_SHORT).show();
    } else if (result.isError()) {
        Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
    }
});
```

### Sending a nudge from a buddy card

```java
// In BuddyActivity.renderBuddies() — for each inflated item_buddy_group view:
String currentUid = authRepository.getCurrentUid();
String buddyUid   = group.getBuddyUid(currentUid);

item.findViewById(R.id.btn_nudge).setOnTouchListener((v, event) -> {
    if (event.getAction() == MotionEvent.ACTION_DOWN) VibrationHelper.onPress(this);
    return false;
});
item.findViewById(R.id.btn_nudge).setOnLongClickListener(v -> {
    VibrationHelper.onSent(this);
    viewModel.sendNudge(group.getId(), buddyUid);
    return true;
});
```

### Using MellowCallback directly (repository layer — advanced use only)

If you need to call a repository method outside of a ViewModel (not recommended):

```java
// MellowCallback<T> is a single-method functional interface:
// void onResult(MellowResult<T> result);

authRepository.checkUsernameAvailable("coolname", result -> {
    if (result.isSuccess() && Boolean.TRUE.equals(result.getData())) {
        // username is free
    }
});
```
