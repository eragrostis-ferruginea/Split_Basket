package com.example.split_basket;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.example.split_basket.sync.SyncStatus;
import com.example.split_basket.sync.Syncable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Entity(tableName = "shopping_items")
public class ShoppingItem implements Syncable {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    @ColumnInfo(name = "name")
    private String name = "";

    @NonNull
    @ColumnInfo(name = "added_by")
    private String addedBy = "";

    @ColumnInfo(name = "quantity")
    private int quantity = 1;

    @ColumnInfo(name = "purchased")
    private boolean purchased = false;

    @ColumnInfo(name = "created_at")
    private long createdAt = System.currentTimeMillis();

    @Nullable
    @ColumnInfo(name = "inventory_item_id")
    private String inventoryItemId;

    @ColumnInfo(name = "sync_status")
    @NonNull
    private SyncStatus syncStatus = SyncStatus.PENDING_CREATE;

    @ColumnInfo(name = "last_modified")
    private long lastModified = System.currentTimeMillis();

    @NonNull
    @ColumnInfo(name = "sync_id")
    private String syncId = "";

    public ShoppingItem() {
        // Required by Room
        this.syncStatus = SyncStatus.PENDING_CREATE;
        this.lastModified = System.currentTimeMillis();
        this.syncId = "";
    }

    @Ignore
    public ShoppingItem(@NonNull String name, @NonNull String addedBy, int quantity) {
        this.name = name;
        this.addedBy = addedBy;
        this.quantity = Math.max(1, quantity);
        this.createdAt = System.currentTimeMillis();
        this.syncStatus = SyncStatus.PENDING_CREATE;
        this.lastModified = System.currentTimeMillis();
        this.syncId = java.util.UUID.randomUUID().toString();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    @NonNull
    public String getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(@NonNull String addedBy) {
        this.addedBy = addedBy;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = Math.max(1, quantity);
    }

    public boolean isPurchased() {
        return purchased;
    }

    public void setPurchased(boolean purchased) {
        this.purchased = purchased;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @Nullable
    public String getInventoryItemId() {
        return inventoryItemId;
    }

    public void setInventoryItemId(@Nullable String inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }

    @NonNull
    public String getSyncId() {
        return syncId;
    }

    public void setSyncId(@NonNull String syncId) {
        this.syncId = syncId;
    }

    // ==================== Syncable Implementation ====================

    @Override
    public SyncStatus getSyncStatus() {
        return syncStatus;
    }

    @Override
    public void setSyncStatus(SyncStatus syncStatus) {
        this.syncStatus = syncStatus;
    }

    @Override
    public long getLastModified() {
        return lastModified;
    }

    @Override
    public void setLastModified(long timestamp) {
        this.lastModified = timestamp;
    }

    @Override
    public String getFirestoreCollection() {
        return "shopping_items";
    }

    @Override
    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("syncId", syncId);
        map.put("id", id);
        map.put("name", name);
        map.put("addedBy", addedBy);
        map.put("quantity", quantity);
        map.put("purchased", purchased);
        map.put("createdAt", createdAt);
        map.put("inventoryItemId", inventoryItemId);
        map.put("syncStatus", syncStatus != null ? syncStatus.name() : SyncStatus.PENDING_CREATE.name());
        map.put("lastModified", lastModified);
        return map;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShoppingItem that = (ShoppingItem) o;
        return id == that.id
                && quantity == that.quantity
                && purchased == that.purchased
                && createdAt == that.createdAt
                && name.equals(that.name)
                && addedBy.equals(that.addedBy)
                && Objects.equals(inventoryItemId, that.inventoryItemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, addedBy, quantity, purchased, createdAt, inventoryItemId);
    }
}
