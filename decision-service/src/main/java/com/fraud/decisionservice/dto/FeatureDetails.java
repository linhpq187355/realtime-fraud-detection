package com.fraud.decisionservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public class FeatureDetails {

    @JsonProperty("so_giao_dich_5_phut")
    private long soGiaoDich5Phut;

    @JsonProperty("tong_tien_1_gio")
    private BigDecimal tongTien1Gio;

    @JsonProperty("trung_binh_lich_su")
    private BigDecimal trungBinhLichSu;

    @JsonProperty("lech_so_voi_trung_binh")
    private BigDecimal lechSoVoiTrungBinh;

    @JsonProperty("khoang_cach_bat_thuong")
    private boolean khoangCachBatThuong;

    public FeatureDetails() {
    }

    public FeatureDetails(long soGiaoDich5Phut, BigDecimal tongTien1Gio, BigDecimal trungBinhLichSu, BigDecimal lechSoVoiTrungBinh, boolean khoangCachBatThuong) {
        this.soGiaoDich5Phut = soGiaoDich5Phut;
        this.tongTien1Gio = tongTien1Gio != null ? tongTien1Gio : BigDecimal.ZERO;
        this.trungBinhLichSu = trungBinhLichSu != null ? trungBinhLichSu : BigDecimal.ZERO;
        this.lechSoVoiTrungBinh = lechSoVoiTrungBinh != null ? lechSoVoiTrungBinh : BigDecimal.ZERO;
        this.khoangCachBatThuong = khoangCachBatThuong;
    }

    public long getSoGiaoDich5Phut() {
        return soGiaoDich5Phut;
    }

    public void setSoGiaoDich5Phut(long soGiaoDich5Phut) {
        this.soGiaoDich5Phut = soGiaoDich5Phut;
    }

    public BigDecimal getTongTien1Gio() {
        return tongTien1Gio;
    }

    public void setTongTien1Gio(BigDecimal tongTien1Gio) {
        this.tongTien1Gio = tongTien1Gio;
    }

    public BigDecimal getTrungBinhLichSu() {
        return trungBinhLichSu;
    }

    public void setTrungBinhLichSu(BigDecimal trungBinhLichSu) {
        this.trungBinhLichSu = trungBinhLichSu;
    }

    public BigDecimal getLechSoVoiTrungBinh() {
        return lechSoVoiTrungBinh;
    }

    public void setLechSoVoiTrungBinh(BigDecimal lechSoVoiTrungBinh) {
        this.lechSoVoiTrungBinh = lechSoVoiTrungBinh;
    }

    public boolean isKhoangCachBatThuong() {
        return khoangCachBatThuong;
    }

    public void setKhoangCachBatThuong(boolean khoangCachBatThuong) {
        this.khoangCachBatThuong = khoangCachBatThuong;
    }
}
