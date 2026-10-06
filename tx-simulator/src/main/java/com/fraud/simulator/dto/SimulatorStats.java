package com.fraud.simulator.dto;

public class SimulatorStats {
    private long totalTransactions;
    private long totalChan;
    private long totalXemXet;
    private long totalChoQua;

    public SimulatorStats() {
    }

    public SimulatorStats(long totalTransactions, long totalChan, long totalXemXet, long totalChoQua) {
        this.totalTransactions = totalTransactions;
        this.totalChan = totalChan;
        this.totalXemXet = totalXemXet;
        this.totalChoQua = totalChoQua;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(long totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public long getTotalChan() {
        return totalChan;
    }

    public void setTotalChan(long totalChan) {
        this.totalChan = totalChan;
    }

    public long getTotalXemXet() {
        return totalXemXet;
    }

    public void setTotalXemXet(long totalXemXet) {
        this.totalXemXet = totalXemXet;
    }

    public long getTotalChoQua() {
        return totalChoQua;
    }

    public void setTotalChoQua(long totalChoQua) {
        this.totalChoQua = totalChoQua;
    }
}
