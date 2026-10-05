package com.fraud.common.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public class CardFeatures {
    private String cardId;
    private long soGiaoDich5Phut;
    private BigDecimal tongTien1Gio;
    private BigDecimal trungBinhLichSu;
    private BigDecimal lechSoVoiTrungBinh;
    private boolean khoangCachBatThuong;

    private String lastTransactionId;
    private Instant lastTimestamp;
    private Location lastLocation;
    private Instant updatedAt;

    public CardFeatures() {
        this.tongTien1Gio = BigDecimal.ZERO;
        this.trungBinhLichSu = BigDecimal.ZERO;
        this.lechSoVoiTrungBinh = BigDecimal.ZERO;
    }

    public CardFeatures(String cardId,
                        long soGiaoDich5Phut,
                        BigDecimal tongTien1Gio,
                        String lastTransactionId,
                        Instant lastTimestamp) {
        this(cardId, soGiaoDich5Phut, tongTien1Gio, BigDecimal.ZERO, BigDecimal.ZERO, false,
                lastTransactionId, lastTimestamp, null, Instant.now());
    }

    public CardFeatures(String cardId,
                        long soGiaoDich5Phut,
                        BigDecimal tongTien1Gio,
                        BigDecimal trungBinhLichSu,
                        BigDecimal lechSoVoiTrungBinh,
                        boolean khoangCachBatThuong,
                        String lastTransactionId,
                        Instant lastTimestamp,
                        Location lastLocation,
                        Instant updatedAt) {
        this.cardId = cardId;
        this.soGiaoDich5Phut = soGiaoDich5Phut;
        this.tongTien1Gio = tongTien1Gio != null ? tongTien1Gio : BigDecimal.ZERO;
        this.trungBinhLichSu = trungBinhLichSu != null ? trungBinhLichSu : BigDecimal.ZERO;
        this.lechSoVoiTrungBinh = lechSoVoiTrungBinh != null ? lechSoVoiTrungBinh : BigDecimal.ZERO;
        this.khoangCachBatThuong = khoangCachBatThuong;
        this.lastTransactionId = lastTransactionId;
        this.lastTimestamp = lastTimestamp;
        this.lastLocation = lastLocation;
        this.updatedAt = updatedAt;
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
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

    public String getLastTransactionId() {
        return lastTransactionId;
    }

    public void setLastTransactionId(String lastTransactionId) {
        this.lastTransactionId = lastTransactionId;
    }

    public Instant getLastTimestamp() {
        return lastTimestamp;
    }

    public void setLastTimestamp(Instant lastTimestamp) {
        this.lastTimestamp = lastTimestamp;
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(Location lastLocation) {
        this.lastLocation = lastLocation;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CardFeatures that = (CardFeatures) o;
        return soGiaoDich5Phut == that.soGiaoDich5Phut &&
                khoangCachBatThuong == that.khoangCachBatThuong &&
                Objects.equals(cardId, that.cardId) &&
                Objects.equals(tongTien1Gio, that.tongTien1Gio) &&
                Objects.equals(trungBinhLichSu, that.trungBinhLichSu) &&
                Objects.equals(lechSoVoiTrungBinh, that.lechSoVoiTrungBinh) &&
                Objects.equals(lastTransactionId, that.lastTransactionId) &&
                Objects.equals(lastTimestamp, that.lastTimestamp) &&
                Objects.equals(lastLocation, that.lastLocation) &&
                Objects.equals(updatedAt, that.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardId, soGiaoDich5Phut, tongTien1Gio, trungBinhLichSu, lechSoVoiTrungBinh,
                khoangCachBatThuong, lastTransactionId, lastTimestamp, lastLocation, updatedAt);
    }

    @Override
    public String toString() {
        return "CardFeatures{" +
                "cardId='" + cardId + '\'' +
                ", soGiaoDich5Phut=" + soGiaoDich5Phut +
                ", tongTien1Gio=" + tongTien1Gio +
                ", trungBinhLichSu=" + trungBinhLichSu +
                ", lechSoVoiTrungBinh=" + lechSoVoiTrungBinh +
                ", khoangCachBatThuong=" + khoangCachBatThuong +
                ", lastTransactionId='" + lastTransactionId + '\'' +
                ", lastTimestamp=" + lastTimestamp +
                ", lastLocation=" + lastLocation +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
