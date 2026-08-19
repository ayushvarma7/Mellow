package edu.northeastern.mellow.data.model;

public class JournalEntry {

    private final String id;
    private final long timestamp;
    private final String date; // "YYYY-MM-DD"
    private final String title; // nullable
    private final String content;
    private final int moodScore; // 1 (very bad) to 5 (very good)

    public JournalEntry() {
        this("", 0L, "", null, "", 3);
    }

    public JournalEntry(String id, long timestamp, String date,
                        String title, String content, int moodScore) {
        this.id = id;
        this.timestamp = timestamp;
        this.date = date;
        this.title = title;
        this.content = content;
        this.moodScore = moodScore;
    }

    public String getId()       { return id; }
    public long getTimestamp()  { return timestamp; }
    public String getDate()     { return date; }
    public String getTitle()    { return title; }
    public String getContent()  { return content; }
    public int getMoodScore()   { return moodScore; }
}
