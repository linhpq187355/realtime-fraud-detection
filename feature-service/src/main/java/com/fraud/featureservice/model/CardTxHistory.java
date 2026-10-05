package com.fraud.featureservice.model;

import java.util.ArrayList;
import java.util.List;

public class CardTxHistory {
    private List<WindowTxRecord> transactions = new ArrayList<>();
    private WindowTxRecord lastTransaction;

    public CardTxHistory() {
    }

    public CardTxHistory(List<WindowTxRecord> transactions) {
        this.transactions = transactions != null ? transactions : new ArrayList<>();
    }

    public List<WindowTxRecord> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<WindowTxRecord> transactions) {
        this.transactions = transactions != null ? transactions : new ArrayList<>();
    }

    public WindowTxRecord getLastTransaction() {
        return lastTransaction;
    }

    public void setLastTransaction(WindowTxRecord lastTransaction) {
        this.lastTransaction = lastTransaction;
    }
}
