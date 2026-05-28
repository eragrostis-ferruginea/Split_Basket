package com.example.split_basket.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages offline-first data synchronization with Firebase Firestore.
 * <p>
 * Architecture:
 * - Local Room DB is the single source of truth
 * - Firebase Firestore acts as the cloud sync layer
 * - Last-writer-wins conflict resolution via timestamps
 * - WorkManager periodically triggers background sync
 * - Real-time listeners update local data when remote changes are detected
 */
public class FirebaseSyncManager {

    private static final String TAG = "FirebaseSync";
    private static volatile FirebaseSyncManager INSTANCE;

    private final FirebaseFirestore firestore;
    private final ExecutorService syncExecutor;
    private boolean initialized = false;

    private FirebaseSyncManager() {
        firestore = FirebaseFirestore.getInstance();
        syncExecutor = Executors.newSingleThreadExecutor();

        // Enable offline persistence (Firestore handles local cache automatically)
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build();
        firestore.setFirestoreSettings(settings);
    }

    public static synchronized FirebaseSyncManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new FirebaseSyncManager();
        }
        return INSTANCE;
    }

    /**
     * Initialize sync — call this once from Application.onCreate()
     */
    public synchronized void initialize(Context context) {
        if (initialized) return;
        initialized = true;
        Log.d(TAG, "FirebaseSyncManager initialized with offline persistence");
    }

    public boolean isInitialized() {
        return initialized;
    }

    // ==================== Upload (Local → Cloud) ====================

    /**
     * Upload a single entity to Firestore.
     * Creates or overwrites the document (last-writer-wins).
     */
    public Task<Void> uploadEntity(Syncable entity) {
        CollectionReference collection = firestore.collection(entity.getFirestoreCollection());
        return collection.document(entity.getSyncId())
                .set(entity.toFirestoreMap())
                .addOnSuccessListener(aVoid -> {
                    entity.setSyncStatus(SyncStatus.SYNCED);
                    Log.d(TAG, "Uploaded " + entity.getFirestoreCollection() + "/" + entity.getSyncId());
                })
                .addOnFailureListener(e -> {
                    entity.setSyncStatus(SyncStatus.SYNC_FAILED);
                    Log.e(TAG, "Failed to upload " + entity.getFirestoreCollection() + "/" + entity.getSyncId(), e);
                });
    }

    /**
     * Delete a document from Firestore.
     */
    public Task<Void> deleteEntity(Syncable entity) {
        return firestore.collection(entity.getFirestoreCollection())
                .document(entity.getSyncId())
                .delete()
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Deleted " + entity.getFirestoreCollection() + "/" + entity.getSyncId()))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to delete " + entity.getFirestoreCollection() + "/" + entity.getSyncId(), e));
    }

    /**
     * Upload a batch of entities.
     */
    public Task<Void> uploadBatch(List<? extends Syncable> entities) {
        if (entities == null || entities.isEmpty()) {
            return Tasks.forResult(null);
        }
        List<Task<Void>> tasks = new ArrayList<>();
        for (Syncable entity : entities) {
            tasks.add(uploadEntity(entity));
        }
        return Tasks.whenAll(tasks);
    }

    // ==================== Download (Cloud → Local) ====================

    /**
     * Fetch all documents from a Firestore collection.
     * Uses cache-first (offline-first) strategy.
     */
    public Task<QuerySnapshot> fetchCollection(String collectionName) {
        return firestore.collection(collectionName)
                .orderBy("lastModified", Query.Direction.DESCENDING)
                .get(Source.CACHE)
                .continueWithTask(task -> {
                    // If cache has data, return it; otherwise fetch from server
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        return Tasks.forResult(task.getResult());
                    }
                    return firestore.collection(collectionName)
                            .orderBy("lastModified", Query.Direction.DESCENDING)
                            .get(Source.SERVER);
                });
    }

    /**
     * Fetch document by ID from Firestore.
     */
    public Task<DocumentSnapshot> fetchDocument(String collectionName, String documentId) {
        return firestore.collection(collectionName)
                .document(documentId)
                .get(Source.CACHE)
                .continueWithTask(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        return Tasks.forResult(task.getResult());
                    }
                    return firestore.collection(collectionName)
                            .document(documentId)
                            .get(Source.SERVER);
                });
    }

    // ==================== Real-time Listeners ====================

    /**
     * Listen for real-time changes on a collection.
     * Returns a registration object that can be removed.
     */
    public com.google.firebase.firestore.ListenerRegistration listenToCollection(
            String collectionName,
            com.google.firebase.firestore.EventListener<QuerySnapshot> listener) {
        return firestore.collection(collectionName)
                .addSnapshotListener(listener);
    }

    // ==================== Utility ====================

    public FirebaseFirestore getFirestore() {
        return firestore;
    }

    /**
     * Convert a Firestore document snapshot to a Map.
     */
    @NonNull
    public static Map<String, Object> docToMap(@Nullable DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            return new HashMap<>();
        }
        Map<String, Object> data = doc.getData();
        return data != null ? data : new HashMap<>();
    }
}
