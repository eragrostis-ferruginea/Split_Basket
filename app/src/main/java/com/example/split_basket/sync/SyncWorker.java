package com.example.split_basket.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.split_basket.data.BillDao;
import com.example.split_basket.data.InventoryDao;
import com.example.split_basket.data.SplitBasketDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * WorkManager Worker that periodically syncs pending local changes to Firebase.
 * Triggered by WorkManager's periodic sync request.
 */
public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorker";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        FirebaseSyncManager syncManager = FirebaseSyncManager.getInstance();
        if (!syncManager.isInitialized()) {
            return Result.retry();
        }

        Context context = getApplicationContext();
        SplitBasketDatabase db = SplitBasketDatabase.getInstance(context);

        try {
            // Gather all entities with pending sync status
            List<Syncable> pendingUpload = new ArrayList<>();

            // Inventory items
            List<com.example.split_basket.InventoryItem> inventoryItems = db.inventoryDao().getAllItems();
            for (com.example.split_basket.InventoryItem item : inventoryItems) {
                if (item.getSyncStatus() != SyncStatus.SYNCED) {
                    pendingUpload.add(item);
                }
            }

            if (pendingUpload.isEmpty()) {
                return Result.success();
            }

            // Upload in batches
            syncManager.uploadBatch(pendingUpload);

            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.retry();
        }
    }
}
