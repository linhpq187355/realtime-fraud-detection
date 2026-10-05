package com.fraud.decisionservice.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MlThresholds {

    @JsonProperty("chan_neu_diem_tren")
    private double chanNeuDiemTren = 0.8;

    @JsonProperty("xem_xet_neu_diem_tren")
    private double xemXetNeuDiemTren = 0.4;

    public MlThresholds() {
    }

    public MlThresholds(double chanNeuDiemTren, double xemXetNeuDiemTren) {
        this.chanNeuDiemTren = chanNeuDiemTren;
        this.xemXetNeuDiemTren = xemXetNeuDiemTren;
    }

    public double getChanNeuDiemTren() {
        return chanNeuDiemTren;
    }

    public void setChanNeuDiemTren(double chanNeuDiemTren) {
        this.chanNeuDiemTren = chanNeuDiemTren;
    }

    public double getXemXetNeuDiemTren() {
        return xemXetNeuDiemTren;
    }

    public void setXemXetNeuDiemTren(double xemXetNeuDiemTren) {
        this.xemXetNeuDiemTren = xemXetNeuDiemTren;
    }
}
