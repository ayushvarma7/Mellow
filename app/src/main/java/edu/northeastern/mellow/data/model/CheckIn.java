package edu.northeastern.mellow.data.model;

/**
 * Represents a single completed check-in event.
 * Written to Firestore under users/{uid}/checkins/{autoId}.
 */
public class CheckIn {

    private final String id;
    private final long timestamp;
    private final CheckInType type;
    private final long durationMs;
    private final int coinsEarned;
    private final boolean rewardTriggered;

    public CheckIn(String id, long timestamp, CheckInType type,
                   long durationMs, int coinsEarned, boolean rewardTriggered) {
        this.id = id;
        this.timestamp = timestamp;
        this.type = type;
        this.durationMs = durationMs;
        this.coinsEarned = coinsEarned;
        this.rewardTriggered = rewardTriggered;
    }

    public String      getId()              { return id; }
    public long        getTimestamp()       { return timestamp; }
    public CheckInType getType()            { return type; }
    public long        getDurationMs()      { return durationMs; }
    public int         getCoinsEarned()     { return coinsEarned; }
    public boolean     isRewardTriggered()  { return rewardTriggered; }
}
