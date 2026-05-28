package com.example.split_basket.sync;

/**
 * Sync status for entities in the offline-first architecture.
 * Tracks whether a local entity needs to be synced to Firebase.
 */
public enum SyncStatus {
    /** Entity is in sync with the cloud (no pending changes) */
    SYNCED,
    /** Entity has local changes pending upload to the cloud */
    PENDING_UPLOAD,
    /** Entity is new and needs to be created in the cloud */
    PENDING_CREATE,
    /** Entity has been deleted locally and needs cloud deletion */
    PENDING_DELETE,
    /** Sync failed due to network or conflict error */
    SYNC_FAILED
}
