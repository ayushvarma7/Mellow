package edu.northeastern.mellow.data.model;

import com.google.firebase.Timestamp;

public class Nudge {

    private String id;
    private String groupId;
    private String senderUid;
    private String senderUsername;
    private String receiverUid;
    private boolean seen;
    private Timestamp timestamp;

    public Nudge() {}

    public String    getId()              { return id; }
    public void      setId(String v)      { this.id = v; }

    public String    getGroupId()         { return groupId; }
    public void      setGroupId(String v) { this.groupId = v; }

    public String    getSenderUid()              { return senderUid; }
    public void      setSenderUid(String v)      { this.senderUid = v; }

    public String    getSenderUsername()         { return senderUsername; }
    public void      setSenderUsername(String v) { this.senderUsername = v; }

    public String    getReceiverUid()            { return receiverUid; }
    public void      setReceiverUid(String v)    { this.receiverUid = v; }

    public boolean   isSeen()            { return seen; }
    public void      setSeen(boolean v)  { this.seen = v; }

    public Timestamp getTimestamp()          { return timestamp; }
    public void      setTimestamp(Timestamp v) { this.timestamp = v; }
}
