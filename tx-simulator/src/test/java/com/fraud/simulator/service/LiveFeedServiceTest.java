package com.fraud.simulator.service;

import com.fraud.simulator.dto.SimulationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LiveFeedServiceTest {

    private LiveFeedService liveFeedService;

    @BeforeEach
    void setUp() {
        liveFeedService = new LiveFeedService();
    }

    @Test
    void shouldStoreAndOrderNewestFirst() {
        SimulationResult r1 = new SimulationResult("tx-1", "card-0001", BigDecimal.valueOf(100), "Shopee", "Hà Nội", null, Instant.now(), "CHO_QUA", null, null, null, 5L, "OK");
        SimulationResult r2 = new SimulationResult("tx-2", "card-0001", BigDecimal.valueOf(200), "Shopee", "Hà Nội", null, Instant.now(), "CHAN", null, null, null, 4L, "OK");

        liveFeedService.add(r1);
        liveFeedService.add(r2);

        List<SimulationResult> list = liveFeedService.getRecentTransactions();
        assertEquals(2, list.size());
        assertEquals("tx-2", list.get(0).getTransactionId());
        assertEquals("tx-1", list.get(1).getTransactionId());
    }

    @Test
    void shouldEnforceMaxEntriesLimit() {
        for (int i = 1; i <= 120; i++) {
            liveFeedService.add(new SimulationResult("tx-" + i, "card-0001", BigDecimal.valueOf(i), "Shopee", "Hà Nội", null, Instant.now(), "CHO_QUA", null, null, null, 1L, "OK"));
        }

        List<SimulationResult> list = liveFeedService.getRecentTransactions();
        assertEquals(100, list.size());
        assertEquals("tx-120", list.get(0).getTransactionId());
    }

    @Test
    void shouldReturnDecisionsSinceTimestamp() {
        Instant baseTime = Instant.ofEpochMilli(1_000_000);
        SimulationResult r1 = new SimulationResult("tx-1", "card-0001", BigDecimal.valueOf(100), "Shopee", "Hà Nội", null, baseTime, "CHO_QUA", null, null, null, 5L, "OK");
        SimulationResult r2 = new SimulationResult("tx-2", "card-0001", BigDecimal.valueOf(200), "Shopee", "Hà Nội", null, baseTime.plusMillis(500), "CHAN", null, null, null, 4L, "OK");
        SimulationResult r3 = new SimulationResult("tx-3", "card-0001", BigDecimal.valueOf(300), "Shopee", "Hà Nội", null, baseTime.plusMillis(1000), "XEM_XET", null, null, null, 3L, "OK");

        liveFeedService.add(r1);
        liveFeedService.add(r2);
        liveFeedService.add(r3);

        List<SimulationResult> sinceR2 = liveFeedService.getDecisionsSince(baseTime.plusMillis(500).toEpochMilli());
        assertEquals(2, sinceR2.size());
        assertEquals("tx-2", sinceR2.get(0).getTransactionId());
        assertEquals("tx-3", sinceR2.get(1).getTransactionId());
    }
}
