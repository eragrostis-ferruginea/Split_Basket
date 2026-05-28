package com.example.split_basket;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.split_basket.sync.SyncStatus;
import com.example.split_basket.sync.Syncable;

import java.util.HashMap;
import java.util.Map;

@Entity(tableName = "inventory_items")
public class InventoryItem implements Syncable {
    @PrimaryKey
    @NonNull
    public String id;
    public String name;
    public int quantity;
    public String category; // Vegetable/Meat/Fruit/Other
    public Long expireDateMillis; // nullable
    public long createdAtMillis;
    public String photoUri; // Added: nullable photo URI

    @ColumnInfo(name = "sync_status")
    @NonNull
    private SyncStatus syncStatus = SyncStatus.PENDING_CREATE;

    @ColumnInfo(name = "last_modified")
    private long lastModified = System.currentTimeMillis();

    // Default constructor for Room
    public InventoryItem() {
        this.syncStatus = SyncStatus.PENDING_CREATE;
        this.lastModified = System.currentTimeMillis();
    }

    // Constructor without photoUri (compatible with existing code)
    @androidx.room.Ignore
    public InventoryItem(String id, String name, int quantity, String category, Long expireDateMillis,
                         long createdAtMillis) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.category = category;
        this.expireDateMillis = expireDateMillis;
        this.createdAtMillis = createdAtMillis;
        this.photoUri = null;
    }

    // Full constructor with photoUri
    @androidx.room.Ignore
    public InventoryItem(String id, String name, int quantity, String category, Long expireDateMillis,
                         long createdAtMillis, String photoUri) {
        this(id, name, quantity, category, expireDateMillis, createdAtMillis);
        this.photoUri = photoUri;
    }

    // Builder pattern for easier creation
    public static class Builder {
        private String id;
        private String name;
        private int quantity;
        private String category;
        private Long expireDateMillis;
        private long createdAtMillis;
        private String photoUri;

        public Builder() {
            this.createdAtMillis = System.currentTimeMillis();
            this.photoUri = null;
        }

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setQuantity(int quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder setCategory(String category) {
            this.category = category;
            return this;
        }

        public Builder setExpireDateMillis(Long expireDateMillis) {
            this.expireDateMillis = expireDateMillis;
            return this;
        }

        public Builder setCreatedAtMillis(long createdAtMillis) {
            this.createdAtMillis = createdAtMillis;
            return this;
        }

        public Builder setPhotoUri(String photoUri) {
            this.photoUri = photoUri;
            return this;
        }

        public InventoryItem build() {
            return new InventoryItem(id, name, quantity, category, expireDateMillis, createdAtMillis, photoUri);
        }
    }

    // ==================== Syncable Implementation ====================

    @Override
    public String getSyncId() {
        return id;
    }

    @NonNull
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
        return "inventory_items";
    }

    @Override
    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("quantity", quantity);
        map.put("category", category);
        map.put("expireDateMillis", expireDateMillis);
        map.put("createdAtMillis", createdAtMillis);
        map.put("photoUri", photoUri);
        map.put("syncStatus", syncStatus != null ? syncStatus.name() : SyncStatus.PENDING_CREATE.name());
        map.put("lastModified", lastModified);
        return map;
    }
}