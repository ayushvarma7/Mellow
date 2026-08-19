package edu.northeastern.mellow.domain.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Random;

import edu.northeastern.mellow.data.model.CheckInResult;
import edu.northeastern.mellow.data.model.UserProgress;
import edu.northeastern.mellow.data.util.DateUtils;

/**
 * Pure gamification logic — no Firebase, no Android dependencies.
 * All methods are static. Takes state in, returns computed results out.
 * This is the single source of truth for how streaks and coins work.
 */
public class GamificationEngine {

    private GamificationEngine() {}

    // --- Result types ---

    public static class StreakUpdate {
        public final int newStreakDays;
        public final int longestStreak;
        public final boolean gracePeriodUsed;
        public final boolean streakBroken;

        public StreakUpdate(int newStreakDays, int longestStreak,
                            boolean gracePeriodUsed, boolean streakBroken) {
            this.newStreakDays = newStreakDays;
            this.longestStreak = longestStreak;
            this.gracePeriodUsed = gracePeriodUsed;
            this.streakBroken = streakBroken;
        }
    }

    public static class ContainerUpdate {
        public final long newContainerCoins;
        public final boolean rewardTriggered;
        public final long newCapacity;       // only meaningful if rewardTriggered
        public final long newContainersOpened;

        public ContainerUpdate(long newContainerCoins, boolean rewardTriggered,
                               long newCapacity, long newContainersOpened) {
            this.newContainerCoins = newContainerCoins;
            this.rewardTriggered = rewardTriggered;
            this.newCapacity = newCapacity;
            this.newContainersOpened = newContainersOpened;
        }
    }

    // --- Streak logic ---

    /**
     * Calculates streak changes based on last check-in date vs today.
     *
     * Rules:
     * - First ever check-in (lastCheckInDate null) → streak = 1
     * - Same day as last check-in → no change (multiple daily check-ins are fine)
     * - Consecutive day → streak + 1
     * - Exactly 1 missed day AND grace not yet used → streak + 1, mark grace used
     * - Anything else → streak resets to 1
     * - longestStreak always tracks the highest streak ever reached
     */
    public static StreakUpdate calculateStreakUpdate(@Nullable String lastCheckInDate,
                                                     @NonNull String today,
                                                     int currentStreakDays,
                                                     int longestStreakDays,
                                                     boolean streakGracePeriodUsed) {
        // First ever check-in
        if (lastCheckInDate == null) {
            return new StreakUpdate(1, Math.max(1, longestStreakDays), false, false);
        }

        // Already checked in today — no streak change
        if (lastCheckInDate.equals(today)) {
            return new StreakUpdate(currentStreakDays, longestStreakDays, streakGracePeriodUsed, false);
        }

        // Consecutive day
        if (DateUtils.isConsecutiveDay(lastCheckInDate, today)) {
            int newStreak = currentStreakDays + 1;
            return new StreakUpdate(newStreak, Math.max(newStreak, longestStreakDays), false, false);
        }

        // Grace period — missed exactly one day and haven't used grace yet
        if (DateUtils.isWithinGracePeriod(lastCheckInDate, today) && !streakGracePeriodUsed) {
            int newStreak = currentStreakDays + 1;
            return new StreakUpdate(newStreak, Math.max(newStreak, longestStreakDays), true, false);
        }

        // Streak broken — reset to 1
        return new StreakUpdate(1, longestStreakDays, false, true);
    }

    // --- Container + coin logic ---

    /**
     * Calculates container state after adding coins.
     *
     * Rules:
     * - Add coinsEarned to currentContainerCoins
     * - If total >= capacity → reward triggers, container resets to 0,
     *   new random capacity generated in range [5, 10]
     * - Coins do NOT carry over after a reward (overflow is discarded)
     */
    public static ContainerUpdate calculateContainerUpdate(long currentContainerCoins,
                                                            long containerCapacity,
                                                            long containersOpened,
                                                            int coinsEarned,
                                                            @NonNull Random random) {
        long newTotal = currentContainerCoins + coinsEarned;

        if (newTotal >= containerCapacity) {
            // Reward! Reset container and pick a new random capacity (5–10 inclusive)
            long newCapacity = 5 + random.nextInt(6);
            return new ContainerUpdate(0L, true, newCapacity, containersOpened + 1);
        }

        return new ContainerUpdate(newTotal, false, containerCapacity, containersOpened);
    }

    // --- Result assembly ---

    /**
     * Assembles the CheckInResult that the UI observes.
     * containerProgress is clamped to [0.0, 1.0].
     */
    public static CheckInResult buildCheckInResult(int coinsEarned,
                                                    long previousTotalCoins,
                                                    @NonNull ContainerUpdate containerUpdate,
                                                    @NonNull StreakUpdate streakUpdate,
                                                    long containerCapacity) {
        long effectiveCapacity = containerUpdate.rewardTriggered
                ? containerUpdate.newCapacity
                : containerCapacity;

        float progress = effectiveCapacity > 0
                ? Math.min(1.0f, (float) containerUpdate.newContainerCoins / effectiveCapacity)
                : 0f;

        return new CheckInResult(
                coinsEarned,
                previousTotalCoins + coinsEarned,
                progress,
                containerUpdate.rewardTriggered,
                streakUpdate.newStreakDays != streakUpdate.longestStreak
                        || streakUpdate.newStreakDays > 0,
                streakUpdate.newStreakDays,
                streakUpdate.gracePeriodUsed
        );
    }

    /**
     * Applies all computed updates to a UserProgress, returning a new instance.
     * The original UserProgress is never mutated.
     */
    public static UserProgress applyUpdates(@NonNull UserProgress current,
                                             @NonNull String today,
                                             @NonNull StreakUpdate streakUpdate,
                                             @NonNull ContainerUpdate containerUpdate,
                                             int coinsEarned) {
        return UserProgress.builder(current)
                .totalCoins(current.getTotalCoins() + coinsEarned)
                .currentContainerCoins(containerUpdate.newContainerCoins)
                .containerCapacity(containerUpdate.rewardTriggered
                        ? containerUpdate.newCapacity
                        : current.getContainerCapacity())
                .containersOpened(containerUpdate.newContainersOpened)
                .currentStreakDays(streakUpdate.newStreakDays)
                .longestStreakDays(streakUpdate.longestStreak)
                .lastCheckInDate(today)
                .streakGracePeriod(streakUpdate.gracePeriodUsed)
                .build();
    }
}
