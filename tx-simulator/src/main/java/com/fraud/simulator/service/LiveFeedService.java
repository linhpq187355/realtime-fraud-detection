package com.fraud.simulator.service;

import com.fraud.simulator.dto.SimulationResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Service
public class LiveFeedService {

    private static final int MAX_ENTRIES = 100;
    private static final int MAX_HISTORY = 20_000;
    private final LinkedList<SimulationResult> buffer = new LinkedList<>();
    private final LinkedList<SimulationResult> historyBuffer = new LinkedList<>();

    public synchronized void add(SimulationResult result) {
        if (result == null) {
            return;
        }
        buffer.addFirst(result);
        while (buffer.size() > MAX_ENTRIES) {
            buffer.removeLast();
        }

        historyBuffer.addLast(result);
        while (historyBuffer.size() > MAX_HISTORY) {
            historyBuffer.removeFirst();
        }
    }

    public synchronized List<SimulationResult> getRecentTransactions() {
        return new ArrayList<>(buffer);
    }

    public synchronized List<SimulationResult> getDecisionsSince(long sinceEpochMs) {
        List<SimulationResult> matched = new ArrayList<>();
        for (SimulationResult res : historyBuffer) {
            if (res.getTimestamp() != null && res.getTimestamp().toEpochMilli() >= sinceEpochMs) {
                matched.add(res);
            }
        }
        return matched;
    }

    public synchronized void clear() {
        buffer.clear();
        historyBuffer.clear();
    }
}
