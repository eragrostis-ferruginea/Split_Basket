package com.example.split_basket;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;

import com.example.split_basket.sync.SyncStatus;
import com.example.split_basket.sync.Syncable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Entity(tableName = "bills")
@TypeConverters({BillItem.Converters.class})
public class BillItem implements Parcelable, Syncable {
    public static final Creator<BillItem> CREATOR = new Creator<BillItem>() {
        @Override
        public BillItem createFromParcel(Parcel in) {
            return new BillItem(in);
        }

        @Override
        public BillItem[] newArray(int size) {
            return new BillItem[size];
        }
    };
    @PrimaryKey
    @NonNull
    private String id;
    private String name;
    private String amount;
    private String status;
    private String method;
    private List<String> participants;
    private List<Double> customAmounts; // Used to store custom amounts
    private String creationDate; // Bill creation date

    @ColumnInfo(name = "sync_status")
    @NonNull
    private SyncStatus syncStatus = SyncStatus.PENDING_CREATE;

    @ColumnInfo(name = "last_modified")
    private long lastModified = System.currentTimeMillis();

    public BillItem(String id, String name, String amount, String status, String method, String creationDate) {
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.status = status;
        this.method = method;
        this.participants = new ArrayList<>();
        this.customAmounts = new ArrayList<>();
        this.creationDate = creationDate;
    }

    protected BillItem(Parcel in) {
        id = in.readString();
        name = in.readString();
        amount = in.readString();
        status = in.readString();
        method = in.readString();
        creationDate = in.readString();
        participants = in.createStringArrayList();
        // Read custom amounts
        int size = in.readInt();
        customAmounts = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            customAmounts.add(in.readDouble());
        }
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    public void addParticipant(String participant) {
        this.participants.add(participant);
    }

    public List<Double> getCustomAmounts() {
        return customAmounts;
    }

    public void setCustomAmounts(List<Double> customAmounts) {
        this.customAmounts = customAmounts;
    }

    public void setCustomAmount(int index, double amount) {
        if (index >= 0 && index < customAmounts.size()) {
            customAmounts.set(index, amount);
        }
    }

    public void addCustomAmount(double amount) {
        this.customAmounts.add(amount);
    }

    // ==================== Syncable Implementation ====================

    @NonNull
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
        return "bills";
    }

    @Override
    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("amount", amount);
        map.put("status", status);
        map.put("method", method);
        map.put("participants", participants);
        map.put("customAmounts", customAmounts);
        map.put("creationDate", creationDate);
        map.put("syncStatus", syncStatus != null ? syncStatus.name() : SyncStatus.PENDING_CREATE.name());
        map.put("lastModified", lastModified);
        return map;
    }

    // Calculate average amount
    public double getAverageAmount() {
        if (participants.isEmpty())
            return 0;

        try {
            // Remove currency symbols and spaces, convert to double
            String cleanAmount = amount.replace("¥", "").replace("$", "").trim();
            double total = Double.parseDouble(cleanAmount);
            return total / participants.size();
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(id);
        parcel.writeString(name);
        parcel.writeString(amount);
        parcel.writeString(status);
        parcel.writeString(method);
        parcel.writeString(creationDate);
        parcel.writeStringList(participants);
        // Write custom amounts
        parcel.writeInt(customAmounts.size());
        for (Double amount : customAmounts) {
            parcel.writeDouble(amount);
        }
    }

    // TypeConverters for Room Database
    public static class Converters {
        // Convert List<String> to String (comma-separated)
        @TypeConverter
        public static String fromStringList(List<String> value) {
            if (value == null)
                return null;
            return String.join(",", value);
        }

        // Convert String to List<String>
        @TypeConverter
        public static List<String> toStringList(String value) {
            if (value == null)
                return new ArrayList<>();
            return List.of(value.split(","));
        }

        // Convert List<Double> to String (comma-separated)
        @TypeConverter
        public static String fromDoubleList(List<Double> value) {
            if (value == null)
                return null;
            return value.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
        }

        // Convert String to List<Double>
        @TypeConverter
        public static List<Double> toDoubleList(String value) {
            if (value == null || value.isEmpty())
                return new ArrayList<>();
            return List.of(value.split(","))
                    .stream()
                    .filter(s -> !s.isEmpty()) // Skip empty strings
                    .map(Double::parseDouble)
                    .collect(Collectors.toList());
        }
    }
}