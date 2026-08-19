package edu.northeastern.mellow.data.model;

import androidx.annotation.Nullable;

/**
 * Represents the user's full gamification state stored in Firestore.
 * Immutable — use Builder to create modified copies.
 */
public class UserProgress {

    private final long totalCoins;
    private final long currentContainerCoins;
    private final long containerCapacity;       // randomized 5–10 per reward cycle
    private final long containersOpened;
    private final int currentStreakDays;
    private final int longestStreakDays;
    @Nullable private final String lastCheckInDate; // "YYYY-MM-DD"
    private final boolean streakGracePeriod;        // true = grace already used this streak

    // No-arg constructor required for Firestore deserialization
    public UserProgress() {
        this(0L, 0L, 7L, 0L, 0, 0, null, false);
    }

    public UserProgress(long totalCoins,
                        long currentContainerCoins,
                        long containerCapacity,
                        long containersOpened,
                        int currentStreakDays,
                        int longestStreakDays,
                        @Nullable String lastCheckInDate,
                        boolean streakGracePeriod) {
        this.totalCoins = totalCoins;
        this.currentContainerCoins = currentContainerCoins;
        this.containerCapacity = containerCapacity;
        this.containersOpened = containersOpened;
        this.currentStreakDays = currentStreakDays;
        this.longestStreakDays = longestStreakDays;
        this.lastCheckInDate = lastCheckInDate;
        this.streakGracePeriod = streakGracePeriod;
    }

    public long    getTotalCoins()            { return totalCoins; }
    public long    getCurrentContainerCoins() { return currentContainerCoins; }
    public long    getContainerCapacity()     { return containerCapacity; }
    public long    getContainersOpened()      { return containersOpened; }
    public int     getCurrentStreakDays()     { return currentStreakDays; }
    public int     getLongestStreakDays()     { return longestStreakDays; }
    @Nullable
    public String  getLastCheckInDate()       { return lastCheckInDate; }
    public boolean isStreakGracePeriod()      { return streakGracePeriod; }

    // --- Builder ---

    public static Builder builder(UserProgress source) {
        return new Builder(source);
    }

    public static class Builder {
        private long totalCoins;
        private long currentContainerCoins;
        private long containerCapacity;
        private long containersOpened;
        private int currentStreakDays;
        private int longestStreakDays;
        private String lastCheckInDate;
        private boolean streakGracePeriod;

        Builder(UserProgress source) {
            this.totalCoins            = source.totalCoins;
            this.currentContainerCoins = source.currentContainerCoins;
            this.containerCapacity     = source.containerCapacity;
            this.containersOpened      = source.containersOpened;
            this.currentStreakDays     = source.currentStreakDays;
            this.longestStreakDays     = source.longestStreakDays;
            this.lastCheckInDate       = source.lastCheckInDate;
            this.streakGracePeriod     = source.streakGracePeriod;
        }

        public Builder totalCoins(long v)            { this.totalCoins = v; return this; }
        public Builder currentContainerCoins(long v) { this.currentContainerCoins = v; return this; }
        public Builder containerCapacity(long v)     { this.containerCapacity = v; return this; }
        public Builder containersOpened(long v)      { this.containersOpened = v; return this; }
        public Builder currentStreakDays(int v)      { this.currentStreakDays = v; return this; }
        public Builder longestStreakDays(int v)      { this.longestStreakDays = v; return this; }
        public Builder lastCheckInDate(String v)     { this.lastCheckInDate = v; return this; }
        public Builder streakGracePeriod(boolean v)  { this.streakGracePeriod = v; return this; }

        public UserProgress build() {
            return new UserProgress(totalCoins, currentContainerCoins, containerCapacity,
                    containersOpened, currentStreakDays, longestStreakDays,
                    lastCheckInDate, streakGracePeriod);
        }
    }
}
