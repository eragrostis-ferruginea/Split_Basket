package com.example.split_basket.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.split_basket.EventLogManager;
import com.example.split_basket.InventoryItem;
import com.example.split_basket.sync.SyncRepository;
import com.example.split_basket.sync.SyncStatus;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class InventoryRepository {

    private static volatile InventoryRepository INSTANCE;
    private final InventoryDao inventoryDao;
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private final Context appContext;
    private final EventLogManager eventLogManager;
    private volatile boolean seeded = false;

    private InventoryRepository(@NonNull Context context) {
        appContext = context.getApplicationContext();
        SplitBasketDatabase database = SplitBasketDatabase.getInstance(appContext);
        inventoryDao = database.inventoryDao();
        eventLogManager = EventLogManager.getInstance(appContext);
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    public static InventoryRepository getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (InventoryRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new InventoryRepository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    public void ensureSeedData() {
        if (seeded)
            return;
        executorService.execute(() -> {
            if (inventoryDao.countItems() == 0) {
                // Add default inventory items
                long currentTime = System.currentTimeMillis();
                InventoryItem milk = new InventoryItem("milk-001", "Milk", 2, "Dairy",
                        currentTime + (30L * 24 * 60 * 60 * 1000), currentTime, null);
                InventoryItem bread = new InventoryItem("bread-001", "Bread", 1, "Bakery",
                        currentTime + (7L * 24 * 60 * 60 * 1000), currentTime, null);
                InventoryItem eggs = new InventoryItem("eggs-001", "Eggs", 12, "Dairy",
                        currentTime + (14L * 24 * 60 * 60 * 1000), currentTime, null);
                InventoryItem apple = new InventoryItem("apple-001", "Apple", 5, "Fruit",
                        currentTime + (10L * 24 * 60 * 60 * 1000), currentTime, null);
                InventoryItem yogurt = new InventoryItem("yogurt-001", "Yogurt", 3, "Dairy",
                        currentTime + (21L * 24 * 60 * 60 * 1000), currentTime, null);

                inventoryDao.insert(milk);
                inventoryDao.insert(bread);
                inventoryDao.insert(eggs);
                inventoryDao.insert(apple);
                inventoryDao.insert(yogurt);
            }
            seeded = true;
        });
    }

    public LiveData<List<InventoryItem>> observeItems() {
        return inventoryDao.observeItems();
    }

    public List<InventoryItem> getItems() {
        List<InventoryItem> result = null;
        try {
            result = executorService.submit(inventoryDao::getAllItems).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result != null ? result : java.util.Collections.emptyList();
    }

    public Future<Void> addItem(@NonNull InventoryItem item) {
        return executorService.submit(() -> {
            item.setSyncStatus(SyncStatus.PENDING_CREATE);
            item.setLastModified(System.currentTimeMillis());
            inventoryDao.insert(item);
            // Add log record
            eventLogManager.addLog(
                    EventLogManager.EVENT_TYPE_INVENTORY_ADD,
                    item.name + " x" + item.quantity + " | " + item.category,
                    "xxx" // Default user
            );
            // Trigger background sync
            triggerSync();
            return null;
        });
    }

    public void updateItem(@NonNull InventoryItem updated) {
        executorService.execute(() -> {
            updated.setSyncStatus(SyncStatus.PENDING_UPLOAD);
            updated.setLastModified(System.currentTimeMillis());
            inventoryDao.update(updated);
            // Add log record
            eventLogManager.addLog(
                    EventLogManager.EVENT_TYPE_INVENTORY_UPDATE,
                    updated.name + " x" + updated.quantity + " | " + updated.category,
                    "xxx" // Default user
            );
            triggerSync();
        });
    }

    public void removeItem(String id) {
        executorService.execute(() -> {
            // Get the item first to log details
            InventoryItem item = inventoryDao.getItemById(id);
            if (item != null) {
                item.setSyncStatus(SyncStatus.PENDING_DELETE);
                item.setLastModified(System.currentTimeMillis());
                // For delete, we mark and then delete locally
                inventoryDao.deleteById(id);
                eventLogManager.addLog(
                        EventLogManager.EVENT_TYPE_INVENTORY_REMOVE,
                        item.name + " x" + item.quantity + " | " + item.category,
                        "xxx" // Default user
                );
                triggerSync();
            } else {
                inventoryDao.deleteById(id);
            }
        });
    }

    private void triggerSync() {
        try {
            SyncRepository.getInstance(appContext).triggerImmediateSync();
        } catch (Exception e) {
            // Sync is best-effort; don't crash if Firebase isn't configured
            e.printStackTrace();
        }
    }

    public void clearAll() {
        executorService.execute(() -> {
            inventoryDao.clearAll();
        });
    }

    // Get logs - this method returns all logs for inventory items
    public List<String> getLogs() {
        // Get logs from EventLogManager and convert to the expected format
        return eventLogManager.getAllLogs().stream()
                .filter(logEntry -> logEntry.actionType().startsWith("INVENTORY"))
                .map(logEntry -> {
                    // Convert the event log to the old format: "time | type | message | category"
                    String description = logEntry.description();
                    String category = "";
                    String formattedMessage = description;

                    // Extract category from message (old format: "name xqty | category")
                    if (description.contains(" | ")) {
                        String[] parts = description.split(" \\| ", 2);
                        formattedMessage = parts[0];
                        category = parts[1];
                    }

                    // Map event type to old format (IN/OUT/UPDATE)
                    String oldType = "";
                    switch (logEntry.actionType()) {
                        case EventLogManager.EVENT_TYPE_INVENTORY_ADD:
                            oldType = "IN";
                            break;
                        case EventLogManager.EVENT_TYPE_INVENTORY_REMOVE:
                            oldType = "OUT";
                            break;
                        case EventLogManager.EVENT_TYPE_INVENTORY_UPDATE:
                            oldType = "UPDATE";
                            break;
                        default:
                            oldType = logEntry.actionType();
                    }

                    // Return in the expected format
                    return String.format("%d | %s | %s | %s", logEntry.timestamp(), oldType, formattedMessage,
                            category);
                })
                .collect(Collectors.toList());
    }

    private void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(3, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
