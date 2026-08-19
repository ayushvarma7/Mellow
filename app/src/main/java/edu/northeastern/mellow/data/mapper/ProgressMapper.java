package edu.northeastern.mellow.data.mapper;

import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

import edu.northeastern.mellow.data.model.CheckIn;
import edu.northeastern.mellow.data.model.CheckInType;
import edu.northeastern.mellow.data.model.UserProgress;

public class ProgressMapper {

    private ProgressMapper() {}

    // --- UserProgress ---

    @Nullable
    public static UserProgress progressFromSnapshot(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;

        return new UserProgress(
                getLong(doc, "totalCoins", 0L),
                getLong(doc, "currentContainerCoins", 0L),
                getLong(doc, "containerCapacity", 7L),
                getLong(doc, "containersOpened", 0L),
                getInt(doc, "currentStreakDays", 0),
                getInt(doc, "longestStreakDays", 0),
                doc.getString("lastCheckInDate"),
                getBoolean(doc, "streakGracePeriod", false)
        );
    }

    public static Map<String, Object> progressToMap(UserProgress progress) {
        Map<String, Object> map = new HashMap<>();
        map.put("totalCoins", progress.getTotalCoins());
        map.put("currentContainerCoins", progress.getCurrentContainerCoins());
        map.put("containerCapacity", progress.getContainerCapacity());
        map.put("containersOpened", progress.getContainersOpened());
        map.put("currentStreakDays", progress.getCurrentStreakDays());
        map.put("longestStreakDays", progress.getLongestStreakDays());
        map.put("lastCheckInDate", progress.getLastCheckInDate());
        map.put("streakGracePeriod", progress.isStreakGracePeriod());
        return map;
    }

    // --- CheckIn ---

    @Nullable
    public static CheckIn checkInFromSnapshot(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;

        Timestamp ts = doc.getTimestamp("timestamp");
        long timestamp = ts != null ? ts.toDate().getTime() : 0L;

        return new CheckIn(
                doc.getId(),
                timestamp,
                CheckInType.fromString(doc.getString("type")),
                getLong(doc, "durationMs", 0L),
                (int) getLong(doc, "coinsEarned", 1L),
                getBoolean(doc, "rewardTriggered", false)
        );
    }

    public static Map<String, Object> checkInToMap(CheckIn checkIn) {
        Map<String, Object> map = new HashMap<>();
        map.put("timestamp", new Timestamp(new java.util.Date(checkIn.getTimestamp())));
        map.put("type", checkIn.getType().getFirestoreValue());
        map.put("durationMs", checkIn.getDurationMs());
        map.put("coinsEarned", checkIn.getCoinsEarned());
        map.put("rewardTriggered", checkIn.isRewardTriggered());
        return map;
    }

    // --- Null-safe field helpers ---

    private static long getLong(DocumentSnapshot doc, String field, long defaultValue) {
        Long value = doc.getLong(field);
        return value != null ? value : defaultValue;
    }

    private static int getInt(DocumentSnapshot doc, String field, int defaultValue) {
        Long value = doc.getLong(field);
        return value != null ? value.intValue() : defaultValue;
    }

    private static boolean getBoolean(DocumentSnapshot doc, String field, boolean defaultValue) {
        Boolean value = doc.getBoolean(field);
        return value != null ? value : defaultValue;
    }
}
