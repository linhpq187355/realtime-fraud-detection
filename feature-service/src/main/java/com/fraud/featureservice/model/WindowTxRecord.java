package com.fraud.featureservice.model;

import java.math.BigDecimal;

public class WindowTxRecord {
    private String transactionId;
    private long timestampEpochMs;
    private BigDecimal amount;

    public WindowTxRecord() {
    }

    public WindowTxRecord(String transactionId, long timestampEpochMs, BigDecimal amount) {
        this.transactionId = transactionId;
        this.timestampEpochMs = timestampEpochMs;
        this.amount = amount != null ? amount : BigDecimal.ZERO;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public long getTimestampEpochMs() {
        return timestampEpochMs;
    }

    public void setTimestampEpochMs(long timestampEpochMs) {
        this.timestampEpochMs = timestampEpochMs;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
