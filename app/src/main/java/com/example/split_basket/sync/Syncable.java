package com.example.split_basket.sync;

import java.util.Map;

/**
 * Interface for entities that can be synced to Firebase Firestore.
 * All Room entities that need cloud sync should implement this.
 */
public interface Syncable {

    /** Unique ID for this entity (maps to Firestore document ID) */
    String getSyncId();

    /** Current sync status */
    SyncStatus getSyncStatus();

    /** Set sync status */
    void setSyncStatus(SyncStatus status);

    /** Last modified timestamp (millis) for conflict resolution (last-writer-wins) */
    long getLastModified();

    /** Set last modified timestamp */
    void setLastModified(long timestamp);

    /** Collection name in Firestore where this entity is stored */
    String getFirestoreCollection();

    /**
     * Convert this entity to a Map for Firestore.
     * Subclasses should serialize all fields.
     */
    Map<String, Object> toFirestoreMap();
}
