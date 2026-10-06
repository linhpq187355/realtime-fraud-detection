package com.fraud.simulator.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DecisionCheckResponse {
    private String transactionId;
    private String decision;
    private Double riskScore;
    private String triggeredRule;
    private FeatureDetailsDto features;
    private long latencyMs;

    public DecisionCheckResponse() {
    }

    public DecisionCheckResponse(String transactionId, String decision, Double riskScore, String triggeredRule, FeatureDetailsDto features, long latencyMs) {
        this.transactionId = transactionId;
        this.decision = decision;
        this.riskScore = riskScore;
        this.triggeredRule = triggeredRule;
        this.features = features;
        this.latencyMs = latencyMs;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
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

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }
}
