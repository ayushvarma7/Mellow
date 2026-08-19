package edu.northeastern.mellow.data.mapper;

import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

import edu.northeastern.mellow.data.model.MoodEntry;

public class MoodMapper {

    private MoodMapper() {}

    @Nullable
    public static MoodEntry moodFromSnapshot(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;

        Timestamp ts = doc.getTimestamp("timestamp");
        long timestamp = ts != null ? ts.toDate().getTime() : 0L;

        return new MoodEntry(
                doc.getId(),
                timestamp,
                getString(doc, "date", ""),
                getInt(doc, "moodScore", 3),
                doc.getString("note"),
                doc.getString("linkedCheckInType")
        );
    }

    public static Map<String, Object> moodToMap(MoodEntry entry) {
        Map<String, Object> map = new HashMap<>();
        map.put("timestamp", new Timestamp(new java.util.Date(entry.getTimestamp())));
        map.put("date", entry.getDate());
        map.put("moodScore", entry.getMoodScore());
        map.put("note", entry.getNote());
        map.put("linkedCheckInType", entry.getLinkedCheckInType());
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
