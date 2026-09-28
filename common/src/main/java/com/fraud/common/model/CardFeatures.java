package com.fraud.common.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public class CardFeatures {
    private String cardId;
    private long soGiaoDich5Phut;
    private BigDecimal tongTien1Gio;
    private String lastTransactionId;
    private Instant updatedAt;

    public CardFeatures() {
        this.tongTien1Gio = BigDecimal.ZERO;
    }

    public CardFeatures(String cardId, long soGiaoDich5Phut, BigDecimal tongTien1Gio, String lastTransactionId, Instant updatedAt) {
        this.cardId = cardId;
        this.soGiaoDich5Phut = soGiaoDich5Phut;
        this.tongTien1Gio = tongTien1Gio != null ? tongTien1Gio : BigDecimal.ZERO;
        this.lastTransactionId = lastTransactionId;
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

    public String getLastTransactionId() {
        return lastTransactionId;
    }

    public void setLastTransactionId(String lastTransactionId) {
        this.lastTransactionId = lastTransactionId;
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
                Objects.equals(cardId, that.cardId) &&
                Objects.equals(tongTien1Gio, that.tongTien1Gio) &&
                Objects.equals(lastTransactionId, that.lastTransactionId) &&
                Objects.equals(updatedAt, that.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardId, soGiaoDich5Phut, tongTien1Gio, lastTransactionId, updatedAt);
    }

    @Override
    public String toString() {
        return "CardFeatures{" +
                "cardId='" + cardId + '\'' +
                ", soGiaoDich5Phut=" + soGiaoDich5Phut +
                ", tongTien1Gio=" + tongTien1Gio +
                ", lastTransactionId='" + lastTransactionId + '\'' +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
