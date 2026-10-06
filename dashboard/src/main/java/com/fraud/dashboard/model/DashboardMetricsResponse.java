package com.fraud.dashboard.model;

import java.util.List;

public class DashboardMetricsResponse {

    private long totalTransactions;
    private long totalChan;
    private long totalXemXet;
    private long totalChoQua;

    private double currentTps;
    private double chanRatio;
    private double p50LatencyMs;
    private double p99LatencyMs;

    private List<TimeDataPoint> tpsHistory;
    private List<TimeDataPoint> chanRatioHistory;
    private List<LatencyDataPoint> latencyHistory;

    private List<DecisionMetricRecord> top10RiskTransactions;

    public DashboardMetricsResponse() {
    }

    public DashboardMetricsResponse(long totalTransactions, long totalChan, long totalXemXet, long totalChoQua,
                                    double currentTps, double chanRatio, double p50LatencyMs, double p99LatencyMs,
                                    List<TimeDataPoint> tpsHistory, List<TimeDataPoint> chanRatioHistory,
                                    List<LatencyDataPoint> latencyHistory,
                                    List<DecisionMetricRecord> top10RiskTransactions) {
        this.totalTransactions = totalTransactions;
        this.totalChan = totalChan;
        this.totalXemXet = totalXemXet;
        this.totalChoQua = totalChoQua;
        this.currentTps = currentTps;
        this.chanRatio = chanRatio;
        this.p50LatencyMs = p50LatencyMs;
        this.p99LatencyMs = p99LatencyMs;
        this.tpsHistory = tpsHistory;
        this.chanRatioHistory = chanRatioHistory;
        this.latencyHistory = latencyHistory;
        this.top10RiskTransactions = top10RiskTransactions;
    }

    public static class TimeDataPoint {
        private String time;
        private double value;

        public TimeDataPoint() {}
        public TimeDataPoint(String time, double value) {
            this.time = time;
            this.value = value;
        }
        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
        public double getValue() { return value; }
        public void setValue(double value) { this.value = value; }
    }

    public static class LatencyDataPoint {
        private String time;
        private double p50;
        private double p99;

        public LatencyDataPoint() {}
        public LatencyDataPoint(String time, double p50, double p99) {
            this.time = time;
            this.p50 = p50;
            this.p99 = p99;
        }
        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
        public double getP50() { return p50; }
        public void setP50(double p50) { this.p50 = p50; }
        public double getP99() { return p99; }
        public void setP99(double p99) { this.p99 = p99; }
    }

    public long getTotalTransactions() { return totalTransactions; }
    public void setTotalTransactions(long totalTransactions) { this.totalTransactions = totalTransactions; }
    public long getTotalChan() { return totalChan; }
    public void setTotalChan(long totalChan) { this.totalChan = totalChan; }
    public long getTotalXemXet() { return totalXemXet; }
    public void setTotalXemXet(long totalXemXet) { this.totalXemXet = totalXemXet; }
    public long getTotalChoQua() { return totalChoQua; }
    public void setTotalChoQua(long totalChoQua) { this.totalChoQua = totalChoQua; }
    public double getCurrentTps() { return currentTps; }
    public void setCurrentTps(double currentTps) { this.currentTps = currentTps; }
    public double getChanRatio() { return chanRatio; }
    public void setChanRatio(double chanRatio) { this.chanRatio = chanRatio; }
    public double getP50LatencyMs() { return p50LatencyMs; }
    public void setP50LatencyMs(double p50LatencyMs) { this.p50LatencyMs = p50LatencyMs; }
    public double getP99LatencyMs() { return p99LatencyMs; }
    public void setP99LatencyMs(double p99LatencyMs) { this.p99LatencyMs = p99LatencyMs; }
    public List<TimeDataPoint> getTpsHistory() { return tpsHistory; }
    public void setTpsHistory(List<TimeDataPoint> tpsHistory) { this.tpsHistory = tpsHistory; }
    public List<TimeDataPoint> getChanRatioHistory() { return chanRatioHistory; }
    public void setChanRatioHistory(List<TimeDataPoint> chanRatioHistory) { this.chanRatioHistory = chanRatioHistory; }
    public List<LatencyDataPoint> getLatencyHistory() { return latencyHistory; }
    public void setLatencyHistory(List<LatencyDataPoint> latencyHistory) { this.latencyHistory = latencyHistory; }
    public List<DecisionMetricRecord> getTop10RiskTransactions() { return top10RiskTransactions; }
    public void setTop10RiskTransactions(List<DecisionMetricRecord> top10RiskTransactions) { this.top10RiskTransactions = top10RiskTransactions; }
}
