package edu.northeastern.mellow.data.mapper;

import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.northeastern.mellow.data.model.BuddyGroup;
import edu.northeastern.mellow.data.model.BuddyRequest;

public class BuddyMapper {

    private BuddyMapper() {}

    // --- BuddyRequest ---

    @Nullable
    public static BuddyRequest requestFromSnapshot(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;

        Timestamp ts = doc.getTimestamp("createdAt");
        long createdAt = ts != null ? ts.toDate().getTime() : 0L;

        return new BuddyRequest(
                doc.getId(),
                getString(doc, "fromUid", ""),
                getString(doc, "fromUsername", ""),
                getString(doc, "toUid", ""),
                getString(doc, "toUsername", ""),
                BuddyRequest.Status.fromString(doc.getString("status")),
                createdAt
        );
    }

    public static Map<String, Object> requestToMap(BuddyRequest request) {
        Map<String, Object> map = new HashMap<>();
        map.put("fromUid",      request.getFromUid());
        map.put("fromUsername", request.getFromUsername());
        map.put("toUid",        request.getToUid());
        map.put("toUsername",   request.getToUsername());
        map.put("status",       request.getStatus().getValue());
        map.put("createdAt",    new Timestamp(new java.util.Date(request.getCreatedAt())));
        return map;
    }

    // --- BuddyGroup ---

    @Nullable
    @SuppressWarnings("unchecked")
    public static BuddyGroup groupFromSnapshot(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;

        Timestamp ts = doc.getTimestamp("createdAt");
        long createdAt = ts != null ? ts.toDate().getTime() : 0L;

        List<String> members = (List<String>) doc.get("members");
        if (members == null) members = new ArrayList<>();

        Map<String, String> memberUsernames = new HashMap<>();
        Object raw = doc.get("memberUsernames");
        if (raw instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) raw).entrySet()) {
                memberUsernames.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
        }

        return new BuddyGroup(doc.getId(), members, memberUsernames, createdAt);
    }

    public static Map<String, Object> groupToMap(BuddyGroup group) {
        Map<String, Object> map = new HashMap<>();
        map.put("members",         group.getMembers());
        map.put("memberUsernames", group.getMemberUsernames());
        map.put("createdAt",       new Timestamp(new java.util.Date(group.getCreatedAt())));
        return map;
    }

    // --- Null-safe helpers ---

    private static String getString(DocumentSnapshot doc, String field, String defaultValue) {
        String value = doc.getString(field);
        return value != null ? value : defaultValue;
    }
}
