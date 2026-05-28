package com.example.split_basket.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.split_basket.BillItem;
import com.example.split_basket.EventLogManager;
import com.example.split_basket.sync.SyncRepository;
import com.example.split_basket.sync.SyncStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BillRepository {

    private static final String PREF_NAME = "bill_storage";
    private static final String KEY_FIRST_LAUNCH = "first_launch";

    private static volatile BillRepository INSTANCE;
    private final BillDao billDao;
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private final Context appContext;
    private final EventLogManager eventLogManager;
    private final SharedPreferences preferences;
    private volatile boolean seeded = false;

    private BillRepository(@NonNull Context context) {
        appContext = context.getApplicationContext();
        SplitBasketDatabase database = SplitBasketDatabase.getInstance(appContext);
        billDao = database.billDao();
        eventLogManager = EventLogManager.getInstance(appContext);
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    public static BillRepository getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (BillRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new BillRepository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    public void ensureSeedData() {
        if (seeded)
            return;
        executorService.execute(() -> {
            // Initialize default bill data on first launch
            if (preferences.getBoolean(KEY_FIRST_LAUNCH, true)) {
                initializeDefaultBills();
                preferences.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
            } else {
                // If it's not the first launch but there are no bills, initialize default bills
                if (billDao.countBills() == 0) {
                    initializeDefaultBills();
                }
            }
            seeded = true;
        });
    }

    // Initialize default bill data
    private void initializeDefaultBills() {
        try {
            List<BillItem> defaultBills = new ArrayList<>();

            // Add default unpaid bill
            BillItem unpaidBill = new BillItem("unpaid_bill_1", "Weekend Party", "$ 389.50", "Unpaid", "Equal Split",
                    "2024-01-01");
            unpaidBill.addParticipant("User1");
            unpaidBill.addParticipant("User2");
            unpaidBill.addParticipant("User3");
            unpaidBill.addParticipant("User4");
            defaultBills.add(unpaidBill);

            // Add default paid bill
            BillItem paidBill1 = new BillItem("paid_bill_1", "Daily Shopping", "$ 128.30", "Paid", "By Quantity",
                    "2024-01-02");
            paidBill1.addParticipant("User1");
            paidBill1.addParticipant("User2");
            paidBill1.addParticipant("User3");
            paidBill1.addParticipant("User4");
            defaultBills.add(paidBill1);

            BillItem paidBill2 = new BillItem("paid_bill_2", "Dinner", "$ 456.80", "Paid", "Custom", "2024-01-03");
            paidBill2.addParticipant("User1");
            paidBill2.addParticipant("User2");
            paidBill2.addParticipant("User3");
            defaultBills.add(paidBill2);

            billDao.insertAll(defaultBills);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public LiveData<List<BillItem>> observeBills() {
        return billDao.observeBills();
    }

    public List<BillItem> getAllBills() {
        List<BillItem> result = null;
        try {
            result = executorService.submit(billDao::getAllBills).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result != null ? result : java.util.Collections.emptyList();
    }

    public List<BillItem> getUnpaidBills() {
        List<BillItem> result = null;
        try {
            result = executorService.submit(billDao::getUnpaidBills).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result != null ? result : java.util.Collections.emptyList();
    }

    public List<BillItem> getPaidBills() {
        List<BillItem> result = null;
        try {
            result = executorService.submit(billDao::getPaidBills).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result != null ? result : java.util.Collections.emptyList();
    }

    public BillItem getBillById(String billId) {
        BillItem result = null;
        try {
            result = executorService.submit(() -> billDao.getBillById(billId)).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public void addBill(@NonNull BillItem bill) {
        executorService.execute(() -> {
            bill.setSyncStatus(SyncStatus.PENDING_CREATE);
            bill.setLastModified(System.currentTimeMillis());
            billDao.insert(bill);
            // Add log record
            eventLogManager.addLog(EventLogManager.EVENT_TYPE_BILL_ADD, bill.getName() + " - " + bill.getAmount(), "");
            triggerSync();
        });
    }

    public void updateBill(@NonNull BillItem updatedBill) {
        executorService.execute(() -> {
            updatedBill.setSyncStatus(SyncStatus.PENDING_UPLOAD);
            updatedBill.setLastModified(System.currentTimeMillis());
            billDao.update(updatedBill);
            // Add log record
            eventLogManager.addLog(EventLogManager.EVENT_TYPE_BILL_UPDATE,
                    updatedBill.getName() + " - " + updatedBill.getAmount(), "");
            triggerSync();
        });
    }

    public void deleteBill(String billId) {
        executorService.execute(() -> {
            // First record the information of the bill to be deleted
            BillItem deletedBill = billDao.getBillById(billId);
            if (deletedBill != null) {
                deletedBill.setSyncStatus(SyncStatus.PENDING_DELETE);
                deletedBill.setLastModified(System.currentTimeMillis());
                billDao.deleteById(billId);
                // Record deletion log
                eventLogManager.addLog(EventLogManager.EVENT_TYPE_BILL_REMOVE,
                        deletedBill.getName() + " - " + deletedBill.getAmount(), "");
                triggerSync();
            }
        });
    }

    private void triggerSync() {
        try {
            SyncRepository.getInstance(appContext).triggerImmediateSync();
        } catch (Exception e) {
            // Sync is best-effort
            e.printStackTrace();
        }
    }

    public void clearAllBills() {
        executorService.execute(() -> {
            billDao.clearAll();
        });
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
