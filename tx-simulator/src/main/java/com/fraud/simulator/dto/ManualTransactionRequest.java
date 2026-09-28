package com.fraud.simulator.dto;

import com.fraud.common.model.Location;

import java.math.BigDecimal;
import java.time.Instant;

public class ManualTransactionRequest {
    private String cardId;
    private BigDecimal amount;
    private String merchant;
    private Location location;
    private Instant timestamp;

    public ManualTransactionRequest() {
    }

    public ManualTransactionRequest(String cardId, BigDecimal amount, String merchant, Location location, Instant timestamp) {
        this.cardId = cardId;
        this.amount = amount;
        this.merchant = merchant;
        this.location = location;
        this.timestamp = timestamp;
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
}
