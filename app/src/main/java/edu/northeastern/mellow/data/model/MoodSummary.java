package edu.northeastern.mellow.data.model;

/**
 * Computed stats for a 7-day window. Built by MoodAnalytics, not stored in Firestore.
 * trend > 0 means improving vs the previous week, < 0 means declining.
 */
public class MoodSummary {

    private final String weekLabel;      // e.g. "Mar 20 – 26"
    private final float averageScore;
    private final int entryCount;
    private final int highestScore;
    private final int lowestScore;
    private final float trend;           // averageScore - previousWeekAverage

    public MoodSummary(String weekLabel, float averageScore, int entryCount,
                       int highestScore, int lowestScore, float trend) {
        this.weekLabel = weekLabel;
        this.averageScore = averageScore;
        this.entryCount = entryCount;
        this.highestScore = highestScore;
        this.lowestScore = lowestScore;
        this.trend = trend;
    }

    public String getWeekLabel()    { return weekLabel; }
    public float getAverageScore()  { return averageScore; }
    public int getEntryCount()      { return entryCount; }
    public int getHighestScore()    { return highestScore; }
    public int getLowestScore()     { return lowestScore; }
    public float getTrend()         { return trend; }

    public boolean hasEntries()     { return entryCount > 0; }
    public boolean isTrendImproving() { return trend > 0; }
}
