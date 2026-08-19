package edu.northeastern.mellow.data.model;

import java.util.List;
import java.util.Map;

/**
 * Represents a buddy connection. Modelled as a group from day one —
 * currently 2 members, but the members list supports more in the future.
 */
public class BuddyGroup {

    private final String id;
    private final List<String> members;                  // UIDs
    private final Map<String, String> memberUsernames;   // uid → username
    private final long createdAt;

    public BuddyGroup() {
        this("", null, null, 0L);
    }

    public BuddyGroup(String id, List<String> members,
                      Map<String, String> memberUsernames, long createdAt) {
        this.id = id;
        this.members = members;
        this.memberUsernames = memberUsernames;
        this.createdAt = createdAt;
    }

    /** Returns the username of the other member (not currentUid). */
    public String getBuddyUsername(String currentUid) {
        if (memberUsernames == null) return "buddy";
        for (Map.Entry<String, String> entry : memberUsernames.entrySet()) {
            if (!entry.getKey().equals(currentUid)) return entry.getValue();
        }
        return "buddy";
    }

    /** Returns the UID of the other member. */
    public String getBuddyUid(String currentUid) {
        if (members == null) return "";
        for (String uid : members) {
            if (!uid.equals(currentUid)) return uid;
        }
        return "";
    }

    public String              getId()              { return id; }
    public List<String>        getMembers()         { return members; }
    public Map<String, String> getMemberUsernames() { return memberUsernames; }
    public long                getCreatedAt()       { return createdAt; }
}
