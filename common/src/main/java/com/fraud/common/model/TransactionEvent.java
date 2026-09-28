package com.fraud.common.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public class TransactionEvent {
    private String transactionId;
    private String cardId;
    private BigDecimal amount;
    private String merchant;
    private Location location;
    private Instant timestamp;

    public TransactionEvent() {
    }

    public TransactionEvent(String transactionId, String cardId, BigDecimal amount, String merchant, Location location, Instant timestamp) {
        this.transactionId = transactionId;
        this.cardId = cardId;
        this.amount = amount;
        this.merchant = merchant;
        this.location = location;
        this.timestamp = timestamp;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getMerchant() {
        return merchant;
    }

    public void setMerchant(String merchant) {
        this.merchant = merchant;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionEvent that = (TransactionEvent) o;
        return Objects.equals(transactionId, that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    @Override
    public String toString() {
        return "TransactionEvent{" +
                "transactionId='" + transactionId + '\'' +
                ", cardId='" + cardId + '\'' +
                ", amount=" + amount +
                ", merchant='" + merchant + '\'' +
                ", location=" + location +
                ", timestamp=" + timestamp +
                '}';
    }
}
