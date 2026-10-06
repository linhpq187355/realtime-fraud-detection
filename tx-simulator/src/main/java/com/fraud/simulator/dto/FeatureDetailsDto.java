package com.fraud.simulator.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FeatureDetailsDto {

    @JsonProperty("soGiaoDich5Phut")
    @JsonAlias({"so_giao_dich_5_phut", "soGiaoDich5Phut"})
    private long soGiaoDich5Phut;

    @JsonProperty("tongTien1Gio")
    @JsonAlias({"tong_tien_1_gio", "tongTien1Gio"})
    private BigDecimal tongTien1Gio;

    @JsonProperty("trungBinhLichSu")
    @JsonAlias({"trung_binh_lich_su", "trungBinhLichSu"})
    private BigDecimal trungBinhLichSu;

    @JsonProperty("lechSoVoiTrungBinh")
    @JsonAlias({"lech_so_voi_trung_binh", "lechSoVoiTrungBinh"})
    private BigDecimal lechSoVoiTrungBinh;

    @JsonProperty("khoangCachBatThuong")
    @JsonAlias({"khoang_cach_bat_thuong", "khoangCachBatThuong"})
    private boolean khoangCachBatThuong;

    public FeatureDetailsDto() {
    }

    public FeatureDetailsDto(long soGiaoDich5Phut, BigDecimal tongTien1Gio, BigDecimal trungBinhLichSu, BigDecimal lechSoVoiTrungBinh, boolean khoangCachBatThuong) {
        this.soGiaoDich5Phut = soGiaoDich5Phut;
        this.tongTien1Gio = tongTien1Gio;
        this.trungBinhLichSu = trungBinhLichSu;
        this.lechSoVoiTrungBinh = lechSoVoiTrungBinh;
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
