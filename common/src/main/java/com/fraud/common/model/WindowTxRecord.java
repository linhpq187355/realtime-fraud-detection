package com.fraud.common.model;

import java.math.BigDecimal;
import java.util.Objects;

public class WindowTxRecord {
    private String transactionId;
    private long timestampEpochMs;
    private BigDecimal amount;
    private Location location;

    public WindowTxRecord() {
    }

    public WindowTxRecord(String transactionId, long timestampEpochMs, BigDecimal amount, Location location) {
        this.transactionId = transactionId;
        this.timestampEpochMs = timestampEpochMs;
        this.amount = amount != null ? amount : BigDecimal.ZERO;
        this.location = location;
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
        this.amount = amount != null ? amount : BigDecimal.ZERO;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WindowTxRecord that = (WindowTxRecord) o;
        return Objects.equals(transactionId, that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    @Override
    public String toString() {
        return "WindowTxRecord{" +
                "transactionId='" + transactionId + '\'' +
                ", timestampEpochMs=" + timestampEpochMs +
                ", amount=" + amount +
                ", location=" + location +
                '}';
    }
}
