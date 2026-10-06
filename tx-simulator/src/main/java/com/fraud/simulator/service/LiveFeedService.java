package com.fraud.simulator.service;

import com.fraud.simulator.dto.SimulationResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Service
public class LiveFeedService {

    private static final int MAX_ENTRIES = 100;
    private final LinkedList<SimulationResult> buffer = new LinkedList<>();

    public synchronized void add(SimulationResult result) {
        if (result == null) {
            return;
        }
        buffer.addFirst(result);
        while (buffer.size() > MAX_ENTRIES) {
            buffer.removeLast();
        }
    }

    public synchronized List<SimulationResult> getRecentTransactions() {
        return new ArrayList<>(buffer);
    }

    public synchronized void clear() {
        buffer.clear();
    }
}
