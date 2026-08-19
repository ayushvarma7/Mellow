package edu.northeastern.mellow.data.model;

public enum CheckInType {
    BREATHING("breathing"),
    GROUNDING("grounding"),
    GRATITUDE("gratitude"),
    CUSTOM("custom");

    private final String firestoreValue;

    CheckInType(String firestoreValue) {
        this.firestoreValue = firestoreValue;
    }

    public String getFirestoreValue() {
        return firestoreValue;
    }

    /** Parses the string stored in Firestore back to an enum. Falls back to CUSTOM. */
    public static CheckInType fromString(String value) {
        if (value == null) return CUSTOM;
        for (CheckInType type : values()) {
            if (type.firestoreValue.equals(value)) return type;
        }
        return CUSTOM;
    }
}
