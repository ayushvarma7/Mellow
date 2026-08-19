package edu.northeastern.mellow.data.model;

/**
 * Returned to the UI after a successful check-in.
 * Drives all post-check-in animations — coin drop, container fill, reward unlock.
 */
public class CheckInResult {

    private final int coinsEarned;
    private final long newTotalCoins;
    private final float containerProgress; // 0.0 to 1.0 — how full the container is
    private final boolean rewardUnlocked;
    private final boolean streakUpdated;
    private final int newStreakCount;
    private final boolean gracePeriodUsed;

    public CheckInResult(int coinsEarned, long newTotalCoins, float containerProgress,
                         boolean rewardUnlocked, boolean streakUpdated,
                         int newStreakCount, boolean gracePeriodUsed) {
        this.coinsEarned = coinsEarned;
        this.newTotalCoins = newTotalCoins;
        this.containerProgress = containerProgress;
        this.rewardUnlocked = rewardUnlocked;
        this.streakUpdated = streakUpdated;
        this.newStreakCount = newStreakCount;
        this.gracePeriodUsed = gracePeriodUsed;
    }

    public int     getCoinsEarned()       { return coinsEarned; }
    public long    getNewTotalCoins()     { return newTotalCoins; }
    public float   getContainerProgress() { return containerProgress; }
    public boolean isRewardUnlocked()     { return rewardUnlocked; }
    public boolean isStreakUpdated()      { return streakUpdated; }
    public int     getNewStreakCount()    { return newStreakCount; }
    public boolean isGracePeriodUsed()   { return gracePeriodUsed; }
}
