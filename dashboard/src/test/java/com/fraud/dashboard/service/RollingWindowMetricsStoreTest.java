package com.fraud.dashboard.service;

import com.fraud.dashboard.model.DashboardMetricsResponse;
import com.fraud.dashboard.model.DecisionMetricRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RollingWindowMetricsStoreTest {

    private RollingWindowMetricsStore store;

    @BeforeEach
    void setUp() {
        store = new RollingWindowMetricsStore();
    }

    @Test
    void shouldDeduplicateByTransactionId() {
        DecisionMetricRecord r1 = new DecisionMetricRecord(
                "tx-1", "card-0001", BigDecimal.valueOf(100_000), "Shopee", "Hà Nội",
                Instant.now(), "CHO_QUA", 0.1, null, 10L
        );
        DecisionMetricRecord duplicate = new DecisionMetricRecord(
                "tx-1", "card-0001", BigDecimal.valueOf(100_000), "Shopee", "Hà Nội",
                Instant.now(), "CHO_QUA", 0.1, null, 10L
        );

        assertTrue(store.record(r1));
        assertFalse(store.record(duplicate)); // duplicate should be ignored

        DashboardMetricsResponse metrics = store.snapshotMetrics();
        assertEquals(1, metrics.getTotalTransactions());
        assertEquals(1, metrics.getTotalChoQua());
    }

    @Test
    void shouldCalculateChanRatioAccurately() {
        store.record(new DecisionMetricRecord("tx-1", "card-0001", BigDecimal.valueOf(100), "M1", "Hà Nội", Instant.now(), "CHO_QUA", 0.1, null, 5L));
        store.record(new DecisionMetricRecord("tx-2", "card-0001", BigDecimal.valueOf(200), "M1", "Hà Nội", Instant.now(), "CHO_QUA", 0.2, null, 6L));
        store.record(new DecisionMetricRecord("tx-3", "card-0001", BigDecimal.valueOf(300), "M1", "Hà Nội", Instant.now(), "XEM_XET", 0.5, null, 7L));
        store.record(new DecisionMetricRecord("tx-4", "card-0001", BigDecimal.valueOf(400), "M1", "Hà Nội", Instant.now(), "CHAN", 1.0, "qua_nhieu_giao_dich", 8L));

        DashboardMetricsResponse metrics = store.snapshotMetrics();
        assertEquals(4, metrics.getTotalTransactions());
        assertEquals(1, metrics.getTotalChan());
        assertEquals(1, metrics.getTotalXemXet());
        assertEquals(2, metrics.getTotalChoQua());
        assertEquals(25.0, metrics.getChanRatio(), 0.01);
    }

    @Test
    void shouldCalculateLatencyPercentilesP50AndP99() {
        // Record 100 transactions with latency from 1 to 100 ms
        for (int i = 1; i <= 100; i++) {
            store.record(new DecisionMetricRecord(
                    "tx-" + i, "card-0001", BigDecimal.valueOf(1000), "M", "Hà Nội",
                    Instant.now(), "CHO_QUA", 0.1, null, (long) i
            ));
        }

        DashboardMetricsResponse metrics = store.snapshotMetrics();
        // For sorted list of 1..100:
        // index 50% = round(0.50 * 99) = 50 -> value 51
        // index 99% = round(0.99 * 99) = 98 -> value 99
        assertEquals(51.0, metrics.getP50LatencyMs(), 1.0);
        assertEquals(99.0, metrics.getP99LatencyMs(), 1.0);
    }

    @Test
    void shouldRankTop10HighestRiskTransactionsWithChanAtTop() {
        // Record 15 transactions with various risk scores and rules
        for (int i = 1; i <= 10; i++) {
            store.record(new DecisionMetricRecord(
                    "tx-low-" + i, "card-0001", BigDecimal.valueOf(100 * i), "M", "Hà Nội",
                    Instant.now(), "CHO_QUA", 0.05 * i, null, 5L
            ));
        }

        // Add 2 rule-blocked CHAN transactions (riskScore is null in rule blocks)
        store.record(new DecisionMetricRecord(
                "tx-chan-1", "card-0002", BigDecimal.valueOf(50_000_000), "ATM", "TP.HCM",
                Instant.now(), "CHAN", null, "qua_nhieu_giao_dich", 12L
        ));
        store.record(new DecisionMetricRecord(
                "tx-chan-2", "card-0003", BigDecimal.valueOf(90_000_000), "ATM", "Hà Nội",
                Instant.now(), "CHAN", null, "di_chuyen_bat_kha_thi", 15L
        ));

        DashboardMetricsResponse metrics = store.snapshotMetrics();
        List<DecisionMetricRecord> top10 = metrics.getTop10RiskTransactions();

        assertEquals(10, top10.size());
        // First two must be the CHAN transactions (effective risk score = 1.0)
        assertEquals("CHAN", top10.get(0).getDecision());
        assertEquals("CHAN", top10.get(1).getDecision());
        // tx-chan-2 has higher amount (90M vs 50M), so it should rank first among the two
        assertEquals("tx-chan-2", top10.get(0).getTransactionId());
        assertEquals("tx-chan-1", top10.get(1).getTransactionId());
    }

    @Test
    void shouldEvictRecordsOlderThanOneHour() {
        Instant now = Instant.now();
        Instant twoHoursAgo = now.minusSeconds(7200);

        store.record(new DecisionMetricRecord(
                "tx-old", "card-0001", BigDecimal.valueOf(1000), "M", "Hà Nội",
                twoHoursAgo, "CHO_QUA", 0.1, null, 5L
        ));
        store.record(new DecisionMetricRecord(
                "tx-recent", "card-0001", BigDecimal.valueOf(2000), "M", "Hà Nội",
                now, "CHAN", 0.9, null, 10L
        ));

        DashboardMetricsResponse metrics = store.snapshotMetrics();
        // The old transaction should have been evicted
        List<DecisionMetricRecord> top = metrics.getTop10RiskTransactions();
        assertEquals(1, top.size());
        assertEquals("tx-recent", top.get(0).getTransactionId());
    }
}
