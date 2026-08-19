# Changelog

All notable changes to Mellow are documented here.
Written by Ayush Varma — Part 3 (Backend + Data Layer).

---

## [e222282] — 2026-03-26 — Onboarding, Buddy System, and Profile

Phase 3 and 4 of my backend work. Covers mood tracking, a full onboarding flow with auto-generated usernames, a buddy connection system (send/accept/decline/remove), and a profile page. Tested end-to-end across two accounts — requests send, arrive as live notifications, and accepting them creates a visible connection on both sides.

### What I built

**MellowUser model updates**

Extended `MellowUser` with three new fields:
- `username` — auto-assigned funny fruit name (e.g. `happykiwi342`)
- `goals` — list of strings selected during onboarding (e.g. "Reduce stress", "Sleep better")
- `onboardingComplete` — boolean flag, used to route first-time users to onboarding

---

**Mood Tracking**

Full mood logging pipeline:

- `MoodEntry.java` — a single mood log: score (1–5), optional note, optional tag, timestamp
- `MoodSummary.java` — aggregate over a week: average score, entry count, trend direction, and a human-readable week label
- `MoodMapper.java` — null-safe Firestore ↔ model conversion
- `MoodAnalytics.java` — pure static class. Zero Firebase dependencies. Computes `MoodSummary` from a list of `MoodEntry` objects. Trend is calculated as a simple slope — positive means improving, negative means declining, flat means stable.
- `MoodRepository` + `MoodRepositoryImpl` — `logMood()` writes to `users/{uid}/moods`. `observeRecentMoods()` is a lifecycle-aware LiveData snapshot listener scoped to the last 7 days. `fetchWeeklySummary()` pulls entries and runs them through `MoodAnalytics`.
- `MoodViewModel` — `logMood()` guarded against double-tap with `isLogging` flag. `startObserving()` attaches the snapshot listener. `refreshWeeklySummary()` triggers a one-shot fetch.

Firestore rules updated to allow `users/{uid}/moods` read/write for the authenticated user only.

---

**Username System**

- `UsernameGenerator.java` — generates fun usernames in the format `adjective + fruit + 3 digits` (e.g. `sillymango807`). Hardcoded word lists, purely random. No network call.
- `usernames/{username}` top-level Firestore collection — maps username → uid. Enables O(1) buddy lookup without scanning all users. Written atomically with the user doc during onboarding via `WriteBatch`.

---

**Onboarding Flow**

Two-screen ViewPager2 flow shown exactly once, on first sign-in:

1. **Welcome screen** — introduces Mellow with a tagline and a "Get Started" button
2. **Goals screen** — grid of selectable goal cards (tap to toggle). User picks any combination.

`OnboardingViewModel.completeOnboarding()` auto-generates a username, then writes username, goals, and `onboardingComplete: true` to Firestore atomically via `WriteBatch.set(merge)`. On success, navigates to `MainActivity`.

`AuthViewModel` now exposes a `Destination` enum (`MAIN` / `ONBOARDING`). `SignInActivity` checks onboarding status after sign-in and routes accordingly. First-time users go to `OnboardingActivity`; returning users go straight to `MainActivity`.

ViewPager2 swipe is disabled — navigation is button-only only.

---

**Buddy System — Data Layer**

- `BuddyRequest.java` — represents a connection request. Fields: `fromUid`, `fromUsername`, `toUid`, `toUsername`, `status` (enum: `PENDING` / `ACCEPTED` / `DECLINED`), `createdAt`.
- `BuddyGroup.java` — represents a confirmed connection. Stores `members` as a `List<String>` (uids) and `memberUsernames` as a `Map<String, String>` (uid → username). Designed for 2+ members from day one so future group expansion requires no data model changes. `getBuddyUsername(currentUid)` returns the other person's username.
- `BuddyMapper.java` — null-safe Firestore ↔ model conversion for both types.
- `BuddyRepository` interface + `BuddyRepositoryImpl`:
  - `sendBuddyRequest()` — looks up the target username in the `usernames` collection, fetches the sender's username from their user doc, then writes the request
  - `acceptBuddyRequest()` — atomically updates request status to `ACCEPTED` and creates a `BuddyGroup` document in a single `WriteBatch`
  - `declineBuddyRequest()` — updates request status to `DECLINED`
  - `removeBuddy()` — deletes the `buddyGroups/{groupId}` document
  - `observeIncomingRequests()` — lifecycle-aware LiveData, surfaces only `PENDING` requests to the current user
  - `observeBuddyGroups()` — lifecycle-aware LiveData, queries groups where `members` array contains current uid

Hilt binding added to `AuthModule`.

---

**Profile Page**

- `ProfileViewModel` — loads `MellowUser` via `AuthRepository.fetchUserProfile()` and `UserProgress` via `ProgressRepository`
- `ProfileActivity` — displays:
  - Avatar circle with initials
  - Display name, `@username`, email
  - Stats card: total coins, current streak, best streak (all-time longest)
  - Goals rendered as Material chips with primary color stroke
  - Sign out button (clears task stack, routes back to `SignInActivity`)

---

**Buddy UI**

- `BuddyActivity` — two dynamic sections:
  - **Incoming requests** — inflates `item_buddy_request.xml` per pending request. Each card shows the sender's username with Accept and Decline buttons.
  - **My buddies** — inflates `item_buddy_group.xml` per active connection. Each card shows the buddy's avatar initial, username, and a red Remove button.
- Both lists update in real time via snapshot listeners — accepting a request makes it disappear from requests and appear in buddies instantly, on both devices.
- `BuddyViewModel` wires the repository to the UI with `sendRequest()`, `acceptRequest()`, `declineRequest()`, and `removeBuddy()`.
- Navigation buttons for Profile and Buddies added to `MainActivity`.

---

**Firestore Security Rules**

Four new rule blocks:
- `users/{uid}/moods` — self-only read/write
- `usernames/{username}` — authenticated read; create only if `uid` matches auth; delete only by owner
- `buddyRequests/{requestId}` — read/create by sender; update (status change) by receiver only
- `buddyGroups/{groupId}` — read/create/update/delete by members only (`request.auth.uid in resource.data.members`)

---

## [fb045b3] — 2026-03-25 — Gamification Engine + Progress Backend

Phase 2 of my backend work. Everything from the coin system to streak tracking to Firestore transactions is in here. Tested end-to-end — check-ins write correctly to Firestore and the UI updates live.

### What I built

**Data models**

Four new POJOs that define the shape of all gamification data:

- `UserProgress.java` — the user's full game state: total coins, container fill, container capacity, containers opened, current streak, longest streak, last check-in date, and whether the grace period has been used. Uses a Builder pattern for creating modified copies without mutating the original.
- `CheckIn.java` — a single completed check-in event written to Firestore.
- `CheckInType.java` — enum for the four check-in types: `BREATHING`, `GROUNDING`, `GRATITUDE`, `CUSTOM`. Has a `fromString()` factory that parses Firestore string values back to enum, falling back to `CUSTOM` if unknown.
- `CheckInResult.java` — what the UI gets back after a check-in. Carries coins earned, new total, container progress as a float (0.0–1.0), whether a reward was triggered, streak count, and whether grace period was used. This is what drives the animation layer.

**DateUtils**

All the date math for streak logic lives here. Using `java.time.LocalDate` (API 26+). Stores and compares dates as `"YYYY-MM-DD"` strings rather than timestamps — because "did I check in yesterday?" is a calendar question, not a time question. `isConsecutiveDay()`, `isWithinGracePeriod()`, `daysBetween()`, `today()`.

**GamificationEngine**

The most important class I wrote. Zero Firebase, zero Android dependencies — pure static methods that take state in and return computed results out. Fully testable in isolation.

Three core calculations:
- `calculateStreakUpdate()` — handles first check-in, same-day repeat, consecutive day, grace period (exactly one missed day), and streak reset. Longest streak always tracks the all-time high.
- `calculateContainerUpdate()` — adds coins to the container. When total hits capacity, triggers a reward, resets container to 0, and picks a new random capacity between 5 and 10. Takes a `Random` instance as a parameter so tests can seed it.
- `buildCheckInResult()` + `applyUpdates()` — assembles the result for the UI and produces the updated `UserProgress` via the Builder.

**ProgressMapper**

Handles all Firestore field conversion for `UserProgress` and `CheckIn`. Every field read is null-safe — Firestore can return null for any field if an older app version wrote the document without it.

**ProgressRepository + implementation**

The repository wires the engine to Firebase. Three main operations:

- `observeProgress()` — lifecycle-aware LiveData backed by a Firestore snapshot listener. Uses a custom inner `ProgressLiveData` class that attaches the listener in `onActive()` and removes it in `onInactive()`. Same pattern as `AuthStateLiveData`.
- `initializeProgress()` — writes a default `UserProgress` document using `SetOptions.merge()`. Safe to call on every launch — won't overwrite existing data.
- `recordCheckIn()` — the critical one. Runs inside a **Firestore transaction**: read current progress → run engine calculations → write updated progress, all atomically. After the transaction commits, writes the check-in document outside the transaction (it's append-only, doesn't need atomicity). Also logs the actual Firebase exception on failure instead of swallowing it.

**ProgressViewModel**

What the UI team calls. `startObserving()` first initializes the progress document (ensuring it exists), then attaches the snapshot listener. `performCheckIn()` is guarded against double-tap with an `isCheckingIn` flag. `getContainerProgressFraction()` returns a `float` directly for the animation layer.

**Firestore security rules**

Added `firestore.rules` to the repo. Users can only read/write their own `users/{uid}`, `users/{uid}/progress`, and `users/{uid}/checkins` documents. Rules are deployed manually via Firebase Console — the file is version-controlled so they don't drift.

**Debugging the check-in failure**

The check-in was failing silently because: (1) Firestore was created in production mode which blocks all writes by default, and (2) the error message was generic rather than surfacing the actual Firebase exception. Fixed by deploying proper security rules and adding `Log.e` throughout `ProgressRepositoryImpl` so the real error shows in Logcat.

**Temp test UI in MainActivity**

Added a temporary smoke test screen to verify the full flow end-to-end: shows current coins, container fill, streak, and a check-in button. Confirmed coins increment, container fills, streak updates, and the reward triggers after the random capacity is reached. All data visible live in Firestore Console. Will be replaced by the real UI from Mahimanjali's branch.

---

## [Unreleased] — Firebase Auth + Backend Foundation

### What I built

**Firebase + Hilt project setup**

Added the Firebase BOM (`34.11.0`), `firebase-auth`, `firebase-firestore`, and `play-services-auth` to `app/build.gradle`. Also added Hilt (`2.51.1`) for dependency injection. Fixed a duplicate plugin issue left over from the initial setup.

**MellowApplication.java**

Custom Application class annotated with `@HiltAndroidApp`. Configures Firestore offline persistence at startup with unlimited cache size.

**Data utilities: MellowResult + MellowCallback**

- `MellowResult<T>` — generic wrapper with `SUCCESS`, `ERROR`, `LOADING` states. Every repository callback returns one of these.
- `MellowCallback<T>` — functional interface for one-shot async operations.

**MellowUser model**

Immutable POJO for the signed-in user. Static factory `fromFirebaseUser()` maps a Firebase `FirebaseUser` to our model.

**AuthRepository + implementation**

Google Sign-In and anonymous (guest) sign-in. `AuthStateLiveData` inner class attaches/detaches `FirebaseAuth.AuthStateListener` based on active observers. New users get a full Firestore document written on first login. Returning users only update `lastActiveAt`.

**Hilt module, AuthViewModel, SignInActivity**

Standard Hilt wiring. `SignInActivity` uses `ActivityResultLauncher` for the Google sign-in intent. Both Google and guest paths tested and confirmed working. Brand colors added to `colors.xml`.

---

## [4afc089] — 2026-03-25 — Initial Android project setup

**Author:** Karan Lakshminarayanan

Base Android project scaffolding — `MainActivity`, Material3 theme, launcher icons, Gradle config. Min SDK 26, target SDK 36.

---

## [86283f1] — 2026-02-06 — Initial commit

**Author:** GitHub Classroom

Auto-generated repo creation. `.gitignore` and placeholder `README.md`.

---

*This changelog covers Part 3 (Backend + Data) of the Mellow project. Changes from other team members will be in their own branches.*
