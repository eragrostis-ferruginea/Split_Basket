package com.example.split_basket.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.split_basket.BillItem;
import com.example.split_basket.InventoryItem;
import com.example.split_basket.ShoppingItem;

@Database(entities = {ShoppingItem.class, InventoryItem.class, BillItem.class, LogEntry.class}, version = 5, exportSchema = false)
public abstract class SplitBasketDatabase extends RoomDatabase {

    private static final String DB_NAME = "split_basket.db";
    private static volatile SplitBasketDatabase INSTANCE;

    public static SplitBasketDatabase getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (SplitBasketDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    SplitBasketDatabase.class, DB_NAME)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public abstract ShoppingListDao shoppingListDao();

    public abstract InventoryDao inventoryDao();

    public abstract BillDao billDao();

    public abstract LogDao logDao();
}
