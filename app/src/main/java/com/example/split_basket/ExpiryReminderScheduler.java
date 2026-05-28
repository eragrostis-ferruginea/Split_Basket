package com.example.split_basket;

import android.content.Context;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/**
 * Schedules expiration reminders using WorkManager.
 * Replaces the legacy AlarmManager approach with reliable,
 * battery-friendly background task scheduling.
 */
public class ExpiryReminderScheduler {

    /**
     * Schedule an expiration reminder using WorkManager.
     * Uses a unique work name based on itemId to prevent duplicate reminders.
     *
     * @param ctx             Application context
     * @param itemId          Unique item identifier
     * @param itemName        Display name of the item
     * @param triggerAtMillis Future time in millis when the reminder should fire
     */
    public static void scheduleReminder(Context ctx, String itemId, String itemName, long triggerAtMillis) {
        long delayMillis = triggerAtMillis - System.currentTimeMillis();
        if (delayMillis <= 0) {
            // Already expired — fire immediately
            delayMillis = 1;
        }

        Data inputData = new Data.Builder()
                .putString("itemId", itemId)
                .putString("itemName", itemName)
                .build();

        OneTimeWorkRequest reminderWork = new OneTimeWorkRequest.Builder(ExpiryReminderWorker.class)
                .setInputData(inputData)
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .addTag("expiry_reminder")
                .addTag("expiry_" + itemId)
                .build();

        WorkManager.getInstance(ctx)
                .enqueueUniqueWork(
                        "expiry_reminder_" + itemId,
                        ExistingWorkPolicy.REPLACE,
                        reminderWork
                );

        // Also ensure notification channel exists
        ExpiryReminderWorker.ensureNotificationChannel(ctx);
    }

    /**
     * Cancel a pending expiration reminder.
     *
     * @param ctx    Application context
     * @param itemId Unique item identifier
     */
    public static void cancelReminder(Context ctx, String itemId) {
        WorkManager.getInstance(ctx)
                .cancelUniqueWork("expiry_reminder_" + itemId);
    }
}