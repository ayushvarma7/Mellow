package edu.northeastern.mellow.data.model;

public class MoodEntry {

    private final String id;
    private final long timestamp;
    private final String date; // "YYYY-MM-DD"
    private final int moodScore; // 1 (very bad) to 5 (very good)
    private final String note; // nullable
    private final String linkedCheckInType; // nullable — which activity they did before logging

    public MoodEntry() {
        this("", 0L, "", 3, null, null);
    }

    public MoodEntry(String id, long timestamp, String date,
                     int moodScore, String note, String linkedCheckInType) {
        this.id = id;
        this.timestamp = timestamp;
        this.date = date;
        this.moodScore = moodScore;
        this.note = note;
        this.linkedCheckInType = linkedCheckInType;
    }

    public String getId()                { return id; }
    public long getTimestamp()           { return timestamp; }
    public String getDate()              { return date; }
    public int getMoodScore()            { return moodScore; }
    public String getNote()              { return note; }
    public String getLinkedCheckInType() { return linkedCheckInType; }
}
