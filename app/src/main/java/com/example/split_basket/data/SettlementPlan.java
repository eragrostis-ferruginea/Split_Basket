package com.example.split_basket.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an optimized settlement plan that minimizes the number of transactions
 * needed to settle debts among a group of people.
 */
public class SettlementPlan {

    private final List<Transaction> transactions;
    private final String summary;

    public SettlementPlan(List<Transaction> transactions, String summary) {
        this.transactions = transactions;
        this.summary = summary;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public String getSummary() {
        return summary;
    }

    public int getTransactionCount() {
        return transactions != null ? transactions.size() : 0;
    }

    /**
     * A single recommended payment transaction between two people.
     */
    public static class Transaction {
        private final String from;
        private final String to;
        private final double amount;

        public Transaction(String from, String to, double amount) {
            this.from = from;
            this.to = to;
            this.amount = amount;
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }

        public double getAmount() {
            return amount;
        }
    }

    /**
     * Returns an empty settlement plan (no transactions needed).
     */
    public static SettlementPlan empty() {
        return new SettlementPlan(new ArrayList<>(), "All settled — no transactions needed.");
    }
}
