package com.fraud.simulator.dto;

public class AutoModeRequest {
    private boolean enabled;
    private int ratePerSecond = 5;

    public AutoModeRequest() {
    }

    public AutoModeRequest(boolean enabled, int ratePerSecond) {
        this.enabled = enabled;
        this.ratePerSecond = ratePerSecond;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getRatePerSecond() {
        return ratePerSecond;
    }

    public void setRatePerSecond(int ratePerSecond) {
        this.ratePerSecond = ratePerSecond;
    }
}
