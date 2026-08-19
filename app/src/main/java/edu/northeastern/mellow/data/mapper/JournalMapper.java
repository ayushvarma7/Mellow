package edu.northeastern.mellow.data.mapper;

import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

import edu.northeastern.mellow.data.model.JournalEntry;

public class JournalMapper {

    private JournalMapper() {}

    @Nullable
    public static JournalEntry journalFromSnapshot(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;

        Timestamp ts = doc.getTimestamp("timestamp");
        long timestamp = ts != null ? ts.toDate().getTime() : 0L;

        return new JournalEntry(
                doc.getId(),
                timestamp,
                getString(doc, "date", ""),
                doc.getString("title"),
                getString(doc, "content", ""),
                getInt(doc, "moodScore", 3)
        );
    }

    public static Map<String, Object> journalToMap(JournalEntry entry) {
        Map<String, Object> map = new HashMap<>();
        map.put("timestamp", new Timestamp(new java.util.Date(entry.getTimestamp())));
        map.put("date", entry.getDate());
        map.put("title", entry.getTitle());
        map.put("content", entry.getContent());
        map.put("moodScore", entry.getMoodScore());
        return map;
    }

    // --- Null-safe helpers ---

    private static String getString(DocumentSnapshot doc, String field, String defaultValue) {
        String value = doc.getString(field);
        return value != null ? value : defaultValue;
    }

    private static int getInt(DocumentSnapshot doc, String field, int defaultValue) {
        Long value = doc.getLong(field);
        return value != null ? value.intValue() : defaultValue;
    }
}
