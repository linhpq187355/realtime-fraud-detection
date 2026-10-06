package com.fraud.dashboard.model;

import java.math.BigDecimal;
import java.time.Instant;

public class DecisionMetricRecord {
    private String transactionId;
    private String cardId;
    private BigDecimal amount;
    private String merchant;
    private String city;
    private Instant timestamp;
    private String decision;
    private Double riskScore;
    private String triggeredRule;
    private Long latencyMs;

    public DecisionMetricRecord() {
    }

    public DecisionMetricRecord(String transactionId, String cardId, BigDecimal amount,
                                String merchant, String city, Instant timestamp,
                                String decision, Double riskScore, String triggeredRule,
                                Long latencyMs) {
        this.transactionId = transactionId;
        this.cardId = cardId;
        this.amount = amount;
        this.merchant = merchant;
        this.city = city;
        this.timestamp = timestamp;
        this.decision = decision;
        this.riskScore = riskScore;
        this.triggeredRule = triggeredRule;
        this.latencyMs = latencyMs;
    }

    public double getEffectiveRiskScore() {
        if ("CHAN".equalsIgnoreCase(decision)) {
            return 1.0;
        }
        if (riskScore != null) {
            return riskScore;
        }
        if ("XEM_XET".equalsIgnoreCase(decision)) {
            return 0.5;
        }
        return 0.0;
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

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public Double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Double riskScore) {
        this.riskScore = riskScore;
    }

    public String getTriggeredRule() {
        return triggeredRule;
    }

    public void setTriggeredRule(String triggeredRule) {
        this.triggeredRule = triggeredRule;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }
}
