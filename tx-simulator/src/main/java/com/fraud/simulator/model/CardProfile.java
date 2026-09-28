package com.fraud.simulator.model;

import java.math.BigDecimal;

public class CardProfile {
    private final String cardId;
    private final BigDecimal historicalAverageAmount;
    private final int historicalTxCount;
    private final BigDecimal historicalTotalAmount;

    public CardProfile(String cardId, BigDecimal historicalAverageAmount, int historicalTxCount, BigDecimal historicalTotalAmount) {
        this.cardId = cardId;
        this.historicalAverageAmount = historicalAverageAmount;
        this.historicalTxCount = historicalTxCount;
        this.historicalTotalAmount = historicalTotalAmount;
    }

    public String getCardId() {
        return cardId;
    }

    public BigDecimal getHistoricalAverageAmount() {
        return historicalAverageAmount;
    }

    public int getHistoricalTxCount() {
        return historicalTxCount;
    }

    public BigDecimal getHistoricalTotalAmount() {
        return historicalTotalAmount;
    }

    @Override
    public String toString() {
        return "CardProfile{" +
                "cardId='" + cardId + '\'' +
                ", historicalAverageAmount=" + historicalAverageAmount +
                ", historicalTxCount=" + historicalTxCount +
                '}';
    }
}
