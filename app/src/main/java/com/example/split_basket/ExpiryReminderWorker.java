package com.example.split_basket;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * WorkManager Worker for scheduling expiration reminders.
 * Replaces the legacy AlarmManager-based approach with reliable background task scheduling.
 */
public class ExpiryReminderWorker extends Worker {

    private static final String CHANNEL_ID = "expiry_notify_work";

    public ExpiryReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Get item details from input data
        String itemId = getInputData().getString("itemId");
        String itemName = getInputData().getString("itemName");

        if (itemName == null) {
            itemName = "Unknown Item";
        }

        Context context = getApplicationContext();

        // Check notification permission (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return Result.failure();
            }
        }

        ensureNotificationChannel(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Inventory Expiry Reminder")
                .setContentText("Expiring Soon: " + itemName)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(context).notify(
                    itemId != null ? itemId.hashCode() : (int) System.currentTimeMillis(),
                    builder.build()
            );
        } catch (SecurityException ignored) {
            return Result.failure();
        }

        return Result.success();
    }

    public static void ensureNotificationChannel(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Expiry Reminder (WorkManager)",
                    NotificationManager.IMPORTANCE_HIGH
            );
            nm.createNotificationChannel(channel);
        }
    }
}
