package edu.northeastern.mellow.data.model;

public class BuddyRequest {

    public enum Status {
        PENDING("pending"),
        ACCEPTED("accepted"),
        DECLINED("declined");

        private final String value;
        Status(String value) { this.value = value; }
        public String getValue() { return value; }

        public static Status fromString(String value) {
            for (Status s : values()) {
                if (s.value.equals(value)) return s;
            }
            return PENDING;
        }
    }

    private final String id;
    private final String fromUid;
    private final String fromUsername;
    private final String toUid;
    private final String toUsername;
    private final Status status;
    private final long createdAt;

    public BuddyRequest() {
        this("", "", "", "", "", Status.PENDING, 0L);
    }

    public BuddyRequest(String id, String fromUid, String fromUsername,
                        String toUid, String toUsername, Status status, long createdAt) {
        this.id = id;
        this.fromUid = fromUid;
        this.fromUsername = fromUsername;
        this.toUid = toUid;
        this.toUsername = toUsername;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId()             { return id; }
    public String getFromUid()        { return fromUid; }
    public String getFromUsername()   { return fromUsername; }
    public String getToUid()          { return toUid; }
    public String getToUsername()     { return toUsername; }
    public Status getStatus()         { return status; }
    public long   getCreatedAt()      { return createdAt; }
}
