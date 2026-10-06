package com.fraud.simulator.service;

import com.fraud.simulator.dto.SimulatorStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimulatorStatsServiceTest {

    private SimulatorStatsService statsService;

    @BeforeEach
    void setUp() {
        statsService = new SimulatorStatsService();
    }

    @Test
    void shouldRecordDecisionsCorrectly() {
        statsService.record("CHO_QUA");
        statsService.record("CHO_QUA");
        statsService.record("XEM_XET");
        statsService.record("CHAN");

        SimulatorStats stats = statsService.getStats();
        assertEquals(4, stats.getTotalTransactions());
        assertEquals(2, stats.getTotalChoQua());
        assertEquals(1, stats.getTotalXemXet());
        assertEquals(1, stats.getTotalChan());
    }

    @Test
    void shouldResetStats() {
        statsService.record("CHAN");
        statsService.reset();

        SimulatorStats stats = statsService.getStats();
        assertEquals(0, stats.getTotalTransactions());
        assertEquals(0, stats.getTotalChan());
    }
}
