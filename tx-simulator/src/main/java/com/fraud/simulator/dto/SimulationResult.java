package com.fraud.simulator.dto;

import com.fraud.common.model.Location;

import java.math.BigDecimal;
import java.time.Instant;

public class SimulationResult {
    private String transactionId;
    private String cardId;
    private BigDecimal amount;
    private String merchant;
    private String city;
    private Location location;
    private Instant timestamp;
    private String decision;
    private Double riskScore;
    private String triggeredRule;
    private FeatureDetailsDto features;
    private Long latencyMs;
    private String statusMessage;

    public SimulationResult() {
    }

    public SimulationResult(String transactionId, String cardId, BigDecimal amount, String merchant,
                            String city, Location location, Instant timestamp, String decision,
                            Double riskScore, String triggeredRule, FeatureDetailsDto features,
                            Long latencyMs, String statusMessage) {
        this.transactionId = transactionId;
        this.cardId = cardId;
        this.amount = amount;
        this.merchant = merchant;
        this.city = city;
        this.location = location;
        this.timestamp = timestamp;
        this.decision = decision;
        this.riskScore = riskScore;
        this.triggeredRule = triggeredRule;
        this.features = features;
        this.latencyMs = latencyMs;
        this.statusMessage = statusMessage;
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

    public FeatureDetailsDto getFeatures() {
        return features;
    }

    public void setFeatures(FeatureDetailsDto features) {
        this.features = features;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
}
