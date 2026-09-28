package com.fraud.featureservice.model;

import java.util.ArrayList;
import java.util.List;

public class CardTxHistory {
    private List<WindowTxRecord> transactions = new ArrayList<>();

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
}
