package com.example.split_basket;

import android.app.Application;

import com.example.split_basket.sync.SyncRepository;

/**
 * Custom Application class for initializing global infrastructure:
 * - Firebase offline-first sync layer
 * - WorkManager configuration
 */
public class SplitBasketApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize the sync repository (offline-first Firebase sync)
        // This sets up Firestore offline persistence and schedules periodic sync
        try {
            SyncRepository.getInstance(this).initialize();
        } catch (Exception e) {
            // Firebase sync initialization is best-effort.
            // If google-services.json is not configured, sync will be gracefully skipped.
            e.printStackTrace();
        }
    }
}
