package com.example.split_basket.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Implements a graph-theory-based minimum transaction algorithm.
 * <p>
 * Given a list of participants and how much each person paid (or owes),
 * this algorithm computes the minimum number of transactions needed
 * to settle all debts.
 * <p>
 * Algorithm: Net balance → Two Priority Queues (max-heap for creditors,
 * max-heap for debtors) → Greedy matching to minimize transaction count.
 * This is the classic "Minimum Cash Flow" / "Debt Simplification" algorithm.
 */
public class DebtSimplifier {

    /**
     * Computes the optimal settlement plan given participants and their payments.
     *
     * @param participants List of participant names
     * @param amountsPaid  Amount each participant paid (total bill contribution)
     * @param splitAmount  The fair share each person should pay
     * @return A SettlementPlan with minimum number of transactions
     */
    public static SettlementPlan optimize(
            List<String> participants,
            List<Double> amountsPaid,
            double splitAmount
    ) {
        if (participants == null || participants.isEmpty()) {
            return SettlementPlan.empty();
        }

        // Step 1: Calculate net balance for each person
        // Positive = should receive money (paid more than share)
        // Negative = should pay money (paid less than share)
        Map<String, Double> netBalance = new HashMap<>();
        for (int i = 0; i < participants.size(); i++) {
            String person = participants.get(i);
            double paid = (i < amountsPaid.size()) ? amountsPaid.get(i) : 0.0;
            double balance = paid - splitAmount;
            // Round to 2 decimal places to avoid floating point issues
            balance = Math.round(balance * 100.0) / 100.0;
            if (Math.abs(balance) > 0.01) {
                netBalance.put(person, balance);
            }
        }

        if (netBalance.isEmpty()) {
            return SettlementPlan.empty();
        }

        // Step 2: Split into creditors (positive balance) and debtors (negative balance)
        // Use max-heaps: largest absolute balance first
        PriorityQueue<Map.Entry<String, Double>> creditors = new PriorityQueue<>(
                (a, b) -> Double.compare(b.getValue(), a.getValue())
        );
        PriorityQueue<Map.Entry<String, Double>> debtors = new PriorityQueue<>(
                (a, b) -> Double.compare(Math.abs(b.getValue()), Math.abs(a.getValue()))
        );

        for (Map.Entry<String, Double> entry : netBalance.entrySet()) {
            if (entry.getValue() > 0) {
                creditors.offer(entry);
            } else {
                debtors.offer(entry);
            }
        }

        // Step 3: Greedily match largest debtor with largest creditor
        List<SettlementPlan.Transaction> transactions = new ArrayList<>();

        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            Map.Entry<String, Double> creditor = creditors.poll();
            Map.Entry<String, Double> debtor = debtors.poll();

            double creditAmount = creditor.getValue();
            double debitAmount = Math.abs(debtor.getValue());
            double settledAmount = Math.min(creditAmount, debitAmount);
            settledAmount = Math.round(settledAmount * 100.0) / 100.0;

            if (settledAmount > 0.01) {
                transactions.add(new SettlementPlan.Transaction(
                        debtor.getKey(),
                        creditor.getKey(),
                        settledAmount
                ));
            }

            // Push remaining balance back
            double remainingCredit = Math.round((creditAmount - settledAmount) * 100.0) / 100.0;
            double remainingDebit = Math.round((debitAmount - settledAmount) * 100.0) / 100.0;

            if (remainingCredit > 0.01) {
                final double rem = remainingCredit;
                creditors.offer(new Map.Entry<String, Double>() {
                    @Override
                    public String getKey() { return creditor.getKey(); }
                    @Override
                    public Double getValue() { return rem; }
                    @Override
                    public Double setValue(Double value) { return null; }
                });
            }
            if (remainingDebit > 0.01) {
                final double rem = remainingDebit;
                debtors.offer(new Map.Entry<String, Double>() {
                    @Override
                    public String getKey() { return debtor.getKey(); }
                    @Override
                    public Double getValue() { return -rem; }
                    @Override
                    public Double setValue(Double value) { return null; }
                });
            }
        }

        String summary = String.format(
                "Optimized from %d to %d transaction%s",
                participants.size(),
                transactions.size(),
                transactions.size() == 1 ? "" : "s"
        );

        return new SettlementPlan(transactions, summary);
    }

    /**
     * Computes optimal settlement from a BillItem.
     * Uses custom amounts if available (Custom mode), otherwise splits equally.
     */
    public static SettlementPlan optimizeFromBill(com.example.split_basket.BillItem bill) {
        List<String> participants = bill.getParticipants();
        if (participants == null || participants.isEmpty()) {
            return SettlementPlan.empty();
        }

        double totalAmount;
        try {
            String cleanAmount = bill.getAmount()
                    .replace("¥", "")
                    .replace("$", "")
                    .replace(",", "")
                    .trim();
            totalAmount = Double.parseDouble(cleanAmount);
        } catch (NumberFormatException e) {
            return SettlementPlan.empty();
        }

        double splitAmount = totalAmount / participants.size();
        List<Double> amountsPaid;

        if ("Custom".equals(bill.getMethod()) && bill.getCustomAmounts() != null
                && bill.getCustomAmounts().size() == participants.size()) {
            // Custom mode: use the custom amounts as "what they owe"
            // In custom mode, amountsPaid = what each person actually should pay
            // But for settlement, we need what each person has already paid
            // Default assumption: in Custom mode, amounts represent contributions
            amountsPaid = bill.getCustomAmounts();
        } else {
            // Equal split: assume one person paid everything
            // For demo purposes, distribute total across participants equally as "paid"
            amountsPaid = new ArrayList<>();
            for (int i = 0; i < participants.size(); i++) {
                if (i == 0) {
                    amountsPaid.add(totalAmount); // First person paid everything
                } else {
                    amountsPaid.add(0.0);
                }
            }
        }

        return optimize(participants, amountsPaid, splitAmount);
    }
}
