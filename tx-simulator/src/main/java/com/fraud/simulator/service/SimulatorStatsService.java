package com.fraud.simulator.service;

import com.fraud.simulator.dto.SimulatorStats;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class SimulatorStatsService {

    private final AtomicLong totalTransactions = new AtomicLong(0);
    private final AtomicLong totalChan = new AtomicLong(0);
    private final AtomicLong totalXemXet = new AtomicLong(0);
    private final AtomicLong totalChoQua = new AtomicLong(0);

    public void record(String decision) {
        totalTransactions.incrementAndGet();
        if (decision != null) {
            switch (decision.trim().toUpperCase()) {
                case "CHAN" -> totalChan.incrementAndGet();
                case "XEM_XET" -> totalXemXet.incrementAndGet();
                case "CHO_QUA" -> totalChoQua.incrementAndGet();
                default -> {}
            }
        }
    }

    public SimulatorStats getStats() {
        return new SimulatorStats(
                totalTransactions.get(),
                totalChan.get(),
                totalXemXet.get(),
                totalChoQua.get()
        );
    }

    public void reset() {
        totalTransactions.set(0);
        totalChan.set(0);
        totalXemXet.set(0);
        totalChoQua.set(0);
    }
}
