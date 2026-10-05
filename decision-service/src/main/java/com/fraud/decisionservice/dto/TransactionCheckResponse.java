package com.fraud.decisionservice.dto;

public class TransactionCheckResponse {
    private String transactionId;
    private String decision;
    private Double riskScore;
    private String triggeredRule;
    private FeatureDetails features;
    private long latencyMs;

    public TransactionCheckResponse() {
    }

    public TransactionCheckResponse(String transactionId, String decision, Double riskScore, String triggeredRule, FeatureDetails features, long latencyMs) {
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

    public FeatureDetails getFeatures() {
        return features;
    }

    public void setFeatures(FeatureDetails features) {
        this.features = features;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }
}
