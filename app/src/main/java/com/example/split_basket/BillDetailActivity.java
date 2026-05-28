package com.example.split_basket;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.split_basket.data.BillRepository;
import com.example.split_basket.data.DebtSimplifier;
import com.example.split_basket.data.SettlementPlan;
import com.google.android.material.button.MaterialButton;

import java.text.DecimalFormat;
import java.util.List;

public class BillDetailActivity extends AppCompatActivity {

    public static final String EXTRA_BILL_NAME = "bill_name";
    public static final String EXTRA_BILL_AMOUNT = "bill_amount";
    public static final String EXTRA_BILL_STATUS = "bill_status";
    public static final String EXTRA_BILL_METHOD = "bill_method";
    public static final String EXTRA_BILL_ID = "bill_id";
    public static final int RESULT_BILL_PAID = 1001;
    // For formatting amount display
    private final DecimalFormat df = new DecimalFormat("0.00");
    private String billId;
    private boolean isPaid = false;
    private BillRepository billStorage;
    private BillItem currentBill;
    private boolean isCustomAmountMode = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_detail);

        try {
            // Initialize bill storage
            billStorage = BillRepository.getInstance(this);

            // Get incoming bill information
            Intent intent = getIntent();
            if (intent == null) {
                Toast.makeText(this, "Cannot retrieve bill information", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Safely get bill information, providing default values
            String billName = intent.getStringExtra(EXTRA_BILL_NAME);
            if (billName == null)
                billName = "Unnamed Bill";

            String billAmount = intent.getStringExtra(EXTRA_BILL_AMOUNT);
            if (billAmount == null)
                billAmount = "$ 0.00";

            String billStatus = intent.getStringExtra(EXTRA_BILL_STATUS);
            if (billStatus == null)
                billStatus = "Unpaid";

            String billMethod = intent.getStringExtra(EXTRA_BILL_METHOD);
            if (billMethod == null)
                billMethod = "Equal Split";

            billId = intent.getStringExtra(EXTRA_BILL_ID);
            if (billId == null)
                billId = "unknown_bill_" + System.currentTimeMillis();

            isPaid = "Paid".equals(billStatus);

            // Get complete bill information from storage
            currentBill = billStorage.getBillById(billId);
            if (currentBill == null) {
                // If not in storage, create a new one
                currentBill = new BillItem(billId, billName, billAmount, billStatus, billMethod,
                        new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()));
            }

            // Check if it is a custom amount mode
            isCustomAmountMode = "Custom".equals(currentBill.getMethod());

            // Display bill information
            TextView tvBillName = findViewById(R.id.tvBillName);
            TextView tvBillAmount = findViewById(R.id.tvBillAmount);
            TextView tvPaymentStatus = findViewById(R.id.tvPaymentStatus);
            TextView tvSplitMethod = findViewById(R.id.tvSplitMethod);

            // Safely set text
            if (tvBillName != null)
                tvBillName.setText(currentBill.getName());
            if (tvBillAmount != null) {
                String amount = currentBill.getAmount();
                // Ensure RMB symbol is displayed
                if (!amount.startsWith("¥")) {
                    // Replace if contains $ symbol
                    amount = amount.replace("$", "¥");
                    if (!amount.startsWith("¥")) {
                        amount = "¥ " + amount;
                    }
                }
                tvBillAmount.setText(amount);
            }
            if (tvPaymentStatus != null) {
                // Display corresponding English text based on status
                String status = currentBill.getStatus();
                if ("Paid".equals(status)) {
                    tvPaymentStatus.setText("Paid");
                    tvPaymentStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                } else if ("Unpaid".equals(status)) {
                    tvPaymentStatus.setText("Unpaid");
                    tvPaymentStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                } else {
                    tvPaymentStatus.setText(status);
                }
            }
            if (tvSplitMethod != null) {
                // Display corresponding English text based on split method
                String method = currentBill.getMethod();
                if ("Equal".equals(method)) {
                    tvSplitMethod.setText("Equal Split");
                } else if ("Custom".equals(method)) {
                    tvSplitMethod.setText("Custom");
                } else if ("By quantity".equals(method)) {
                    tvSplitMethod.setText("By Quantity");
                } else if ("By item".equals(method)) {
                    tvSplitMethod.setText("By Item");
                } else {
                    tvSplitMethod.setText(method);
                }
            }

            // Display creation date
            TextView tvCreationDateValue = findViewById(R.id.tvCreationDateValue);
            if (tvCreationDateValue != null && currentBill != null && currentBill.getCreationDate() != null) {
                tvCreationDateValue.setText(currentBill.getCreationDate());
            }

            // Set back button
            MaterialButton btnBack = findViewById(R.id.btnBack);
            if (btnBack != null) {
                btnBack.setOnClickListener(v -> finish());
            }

            // Set payment button
            MaterialButton btnPay = findViewById(R.id.btnPayBill);
            if (btnPay != null) {
                if (isPaid) {
                    btnPay.setVisibility(View.GONE);
                }

                btnPay.setOnClickListener(v -> {
                    try {
                        // Save custom amounts (if any)
                        if (isCustomAmountMode) {
                            saveCustomAmounts();
                        }

                        // Update bill status
                        if (currentBill != null) {
                            currentBill.setStatus("Paid");
                            billStorage.updateBill(currentBill);
                        }

                        // Mark bill as paid
                        isPaid = true;
                        // Ensure current bill status is set to English Paid
                        if (currentBill != null) {
                            currentBill.setStatus("Paid");
                        }
                        if (tvPaymentStatus != null) {
                            tvPaymentStatus.setText("Paid");
                            tvPaymentStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                        }
                        if (btnPay != null) {
                            btnPay.setVisibility(View.GONE);
                        }

                        Toast.makeText(this, "Bill paid successfully", Toast.LENGTH_SHORT).show();

                        // Set result and return
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra(EXTRA_BILL_ID, billId);
                        setResult(RESULT_BILL_PAID, resultIntent);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Payment failed", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            // Set delete bill button
            MaterialButton btnDelete = findViewById(R.id.btnDeleteBill);
            if (btnDelete != null) {
                btnDelete.setOnClickListener(v -> {
                    try {
                        // Delete bill from database
                        if (billStorage != null && billId != null) {
                            billStorage.deleteBill(billId);
                            Toast.makeText(this, "Bill deleted successfully", Toast.LENGTH_SHORT).show();

                            // Set result and return
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra(EXTRA_BILL_ID, billId);
                            setResult(RESULT_OK, resultIntent);

                            // Close current activity
                            finish();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            // Add participants information
            addParticipants();

            // Show optimized settlement using graph algorithm
            showOptimizedSettlement();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading bill details", Toast.LENGTH_SHORT).show();
            finish(); // Close page on error
        }
    }

    private void showOptimizedSettlement() {
        try {
            LinearLayout settlementContainer = findViewById(R.id.settlementContainer);
            TextView tvSummary = findViewById(R.id.tvSettlementSummary);
            if (settlementContainer == null || tvSummary == null) return;

            settlementContainer.removeAllViews();

            // Compute optimized settlement using graph theory algorithm
            SettlementPlan plan = DebtSimplifier.optimizeFromBill(currentBill);

            if (plan.getTransactions() == null || plan.getTransactions().isEmpty()) {
                tvSummary.setText("All settled — no transactions needed.");
                return;
            }

            tvSummary.setText(plan.getSummary());

            for (SettlementPlan.Transaction tx : plan.getTransactions()) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(8, 8, 8, 8);

                TextView tvFrom = new TextView(this);
                tvFrom.setLayoutParams(new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                tvFrom.setText(tx.getFrom());
                tvFrom.setTextSize(14);

                TextView tvArrow = new TextView(this);
                tvArrow.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                tvArrow.setText(" → ");
                tvArrow.setTextSize(14);

                TextView tvTo = new TextView(this);
                tvTo.setLayoutParams(new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                tvTo.setText(tx.getTo());
                tvTo.setTextSize(14);

                TextView tvAmount = new TextView(this);
                tvAmount.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                tvAmount.setText(String.format("¥ %.2f", tx.getAmount()));
                tvAmount.setTextSize(14);
                tvAmount.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                tvAmount.setTypeface(null, android.graphics.Typeface.BOLD);

                row.addView(tvFrom);
                row.addView(tvArrow);
                row.addView(tvTo);
                row.addView(tvAmount);

                settlementContainer.addView(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addParticipants() {
        try {
            LinearLayout container = findViewById(R.id.participantsContainer);
            if (container == null) {
                return; // Return directly if container not found
            }

            // Clear the container to avoid duplicate additions
            container.removeAllViews();

            // Get the list of participants
            List<String> participants = currentBill.getParticipants();
            List<Double> customAmounts = currentBill.getCustomAmounts();

            if (participants.isEmpty()) {
                // If no participants, use default 4 participants
                String[] defaultParticipants = {"User1", "User2", "User3", "User4"};
                for (String participant : defaultParticipants) {
                    currentBill.addParticipant(participant);
                }
                participants = currentBill.getParticipants();
            }

            // Calculate the average amount
            double averageAmount = currentBill.getAverageAmount();

            // Add participants
            for (int i = 0; i < participants.size(); i++) {
                String participant = participants.get(i);

                LinearLayout participantRow = new LinearLayout(this);
                participantRow.setOrientation(LinearLayout.HORIZONTAL);
                participantRow.setPadding(12, 12, 12, 12);

                TextView nameTv = new TextView(this);
                LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                nameTv.setLayoutParams(nameParams);
                nameTv.setText(participant);
                nameTv.setTextSize(14);

                if (isCustomAmountMode && !isPaid) {
                    // Show input fields when in custom amount mode and unpaid
                    EditText amountEdit = new EditText(this);
                    LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(0,
                            LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                    amountEdit.setLayoutParams(editParams);
                    amountEdit.setInputType(
                            android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);

                    // Set default values
                    if (i < customAmounts.size() && customAmounts.get(i) > 0) {
                        amountEdit.setText(df.format(customAmounts.get(i)));
                    } else {
                        amountEdit.setText(df.format(averageAmount));
                    }

                    // Set hint text
                    amountEdit.setHint("Enter amount");

                    // Set tag for subsequent value retrieval
                    amountEdit.setTag("amount_edit_" + i);

                    participantRow.addView(nameTv);
                    participantRow.addView(amountEdit);
                } else {
                    // Show text in other cases
                    TextView amountTv = new TextView(this);
                    LinearLayout.LayoutParams amountParams = new LinearLayout.LayoutParams(0,
                            LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                    amountTv.setLayoutParams(amountParams);

                    // Display amount
                    if (i < customAmounts.size() && customAmounts.get(i) > 0) {
                        amountTv.setText("¥ " + df.format(customAmounts.get(i)));
                    } else {
                        amountTv.setText("¥ " + df.format(averageAmount));
                    }
                    amountTv.setTextSize(14);

                    participantRow.addView(nameTv);
                    participantRow.addView(amountTv);
                }

                container.addView(participantRow);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Failure to load participant information does not affect main functionality
        }
    }

    // Save custom amount
    private void saveCustomAmounts() {
        try {
            if (currentBill == null)
                return;

            LinearLayout container = findViewById(R.id.participantsContainer);
            if (container == null)
                return;

            List<String> participants = currentBill.getParticipants();
            // Clear the custom amounts list
            currentBill.getCustomAmounts().clear();

            for (int i = 0; i < participants.size(); i++) {
                EditText editText = container.findViewWithTag("amount_edit_" + i);
                if (editText != null) {
                    String text = editText.getText().toString();
                    if (!text.isEmpty()) {
                        try {
                            double amount = Double.parseDouble(text);
                            currentBill.addCustomAmount(amount);
                        } catch (NumberFormatException e) {
                            // Use 0 if format is wrong
                            currentBill.addCustomAmount(0.0);
                        }
                    } else {
                        currentBill.addCustomAmount(0.0);
                    }
                }
            }

            // Update storage
            billStorage.updateBill(currentBill);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving custom amounts", Toast.LENGTH_SHORT).show();
        }
    }
}