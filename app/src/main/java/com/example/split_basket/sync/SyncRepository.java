package com.example.split_basket.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/**
 * Central repository for managing sync operations.
 * Provides high-level methods for triggering syncs and registering listeners.
 */
public class SyncRepository {

    private static final String TAG = "SyncRepository";
    private static final String PERIODIC_SYNC_NAME = "periodic_firebase_sync";

    private static volatile SyncRepository INSTANCE;
    private final FirebaseSyncManager syncManager;
    private final Context appContext;
    private boolean initialized = false;

    private SyncRepository(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.syncManager = FirebaseSyncManager.getInstance();
    }

    public static synchronized SyncRepository getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            INSTANCE = new SyncRepository(context);
        }
        return INSTANCE;
    }

    /**
     * Initialize sync infrastructure.
     * Call this once from Application.onCreate().
     */
    public synchronized void initialize() {
        if (initialized) return;
        initialized = true;

        syncManager.initialize(appContext);

        // Schedule periodic background sync (every 15 minutes, minimum interval)
        schedulePeriodicSync();

        Log.d(TAG, "SyncRepository initialized");
    }

    /**
     * Schedule periodic WorkManager sync job.
     */
    private void schedulePeriodicSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
                SyncWorker.class,
                15, TimeUnit.MINUTES
        )
                .setConstraints(constraints)
                .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        1, TimeUnit.MINUTES
                )
                .build();

        WorkManager.getInstance(appContext)
                .enqueueUniquePeriodicWork(
                        PERIODIC_SYNC_NAME,
                        ExistingPeriodicWorkPolicy.KEEP,
                        syncRequest
                );

        Log.d(TAG, "Periodic sync scheduled every 15 minutes");
    }

    /**
     * Trigger an immediate one-time sync.
     */
    public void triggerImmediateSync() {
        androidx.work.OneTimeWorkRequest syncRequest =
                new androidx.work.OneTimeWorkRequest.Builder(SyncWorker.class)
                        .setConstraints(new Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build())
                        .build();

        WorkManager.getInstance(appContext)
                .enqueue(syncRequest);

        Log.d(TAG, "Immediate sync triggered");
    }
}
