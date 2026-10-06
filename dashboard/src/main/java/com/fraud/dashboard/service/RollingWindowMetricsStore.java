package com.fraud.dashboard.service;

import com.fraud.dashboard.model.DashboardMetricsResponse;
import com.fraud.dashboard.model.DashboardMetricsResponse.LatencyDataPoint;
import com.fraud.dashboard.model.DashboardMetricsResponse.TimeDataPoint;
import com.fraud.dashboard.model.DecisionMetricRecord;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RollingWindowMetricsStore {

    private static final Duration WINDOW_DURATION = Duration.ofHours(1);
    private static final int MAX_HISTORY_POINTS = 60;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final List<DecisionMetricRecord> windowRecords = new ArrayList<>();
    private final Set<String> seenTxIds = ConcurrentHashMap.newKeySet();

    private final LinkedList<TimeDataPoint> tpsHistory = new LinkedList<>();
    private final LinkedList<TimeDataPoint> chanRatioHistory = new LinkedList<>();
    private final LinkedList<LatencyDataPoint> latencyHistory = new LinkedList<>();

    private final AtomicLong cumulativeTransactions = new AtomicLong(0);
    private final AtomicLong cumulativeChan = new AtomicLong(0);
    private final AtomicLong cumulativeXemXet = new AtomicLong(0);
    private final AtomicLong cumulativeChoQua = new AtomicLong(0);

    public synchronized boolean record(DecisionMetricRecord record) {
        if (record == null || record.getTransactionId() == null) {
            return false;
        }

        if (!seenTxIds.add(record.getTransactionId())) {
            // Deduplicate - transactionId already processed
            return false;
        }

        if (record.getTimestamp() == null) {
            record.setTimestamp(Instant.now());
        }

        windowRecords.add(record);

        cumulativeTransactions.incrementAndGet();
        if ("CHAN".equalsIgnoreCase(record.getDecision())) {
            cumulativeChan.incrementAndGet();
        } else if ("XEM_XET".equalsIgnoreCase(record.getDecision())) {
            cumulativeXemXet.incrementAndGet();
        } else if ("CHO_QUA".equalsIgnoreCase(record.getDecision())) {
            cumulativeChoQua.incrementAndGet();
        }

        evictOldRecords(Instant.now());
        return true;
    }

    public synchronized void recordBatch(List<DecisionMetricRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (DecisionMetricRecord r : records) {
            record(r);
        }
    }

    public synchronized void evictOldRecords(Instant now) {
        Instant cutoff = now.minus(WINDOW_DURATION);
        Iterator<DecisionMetricRecord> it = windowRecords.iterator();
        while (it.hasNext()) {
            DecisionMetricRecord r = it.next();
            if (r.getTimestamp().isBefore(cutoff)) {
                seenTxIds.remove(r.getTransactionId());
                it.remove();
            }
        }
    }

    public synchronized DashboardMetricsResponse snapshotMetrics() {
        Instant now = Instant.now();
        evictOldRecords(now);

        long total = windowRecords.size();
        long chanCount = 0;
        long xemXetCount = 0;
        long choQuaCount = 0;

        List<Long> latencies = new ArrayList<>();
        Instant oneSecondAgo = now.minusSeconds(1);
        long txLastSecond = 0;

        for (DecisionMetricRecord r : windowRecords) {
            if ("CHAN".equalsIgnoreCase(r.getDecision())) {
                chanCount++;
            } else if ("XEM_XET".equalsIgnoreCase(r.getDecision())) {
                xemXetCount++;
            } else if ("CHO_QUA".equalsIgnoreCase(r.getDecision())) {
                choQuaCount++;
            }

            if (r.getLatencyMs() != null) {
                latencies.add(r.getLatencyMs());
            }

            if (!r.getTimestamp().isBefore(oneSecondAgo)) {
                txLastSecond++;
            }
        }

        double currentTps = (double) txLastSecond;
        double chanRatio = (total > 0) ? ((double) chanCount / total) * 100.0 : 0.0;

        double p50 = 0.0;
        double p99 = 0.0;
        if (!latencies.isEmpty()) {
            Collections.sort(latencies);
            int idx50 = (int) Math.round(0.50 * (latencies.size() - 1));
            int idx99 = (int) Math.round(0.99 * (latencies.size() - 1));
            p50 = latencies.get(idx50);
            p99 = latencies.get(idx99);
        }

        List<DecisionMetricRecord> sortedRisk = new ArrayList<>(windowRecords);
        sortedRisk.sort((a, b) -> {
            int cmp = Double.compare(b.getEffectiveRiskScore(), a.getEffectiveRiskScore());
            if (cmp != 0) return cmp;
            if (a.getAmount() != null && b.getAmount() != null) {
                int amtCmp = b.getAmount().compareTo(a.getAmount());
                if (amtCmp != 0) return amtCmp;
            }
            return b.getTimestamp().compareTo(a.getTimestamp());
        });

        List<DecisionMetricRecord> top10 = sortedRisk.subList(0, Math.min(10, sortedRisk.size()));

        String timeLabel = LocalTime.now().format(TIME_FMT);
        appendHistory(tpsHistory, new TimeDataPoint(timeLabel, currentTps));
        appendHistory(chanRatioHistory, new TimeDataPoint(timeLabel, chanRatio));
        appendHistory(latencyHistory, new LatencyDataPoint(timeLabel, p50, p99));

        return new DashboardMetricsResponse(
                cumulativeTransactions.get(),
                cumulativeChan.get(),
                cumulativeXemXet.get(),
                cumulativeChoQua.get(),
                currentTps,
                chanRatio,
                p50,
                p99,
                new ArrayList<>(tpsHistory),
                new ArrayList<>(chanRatioHistory),
                new ArrayList<>(latencyHistory),
                new ArrayList<>(top10)
        );
    }

    private <T> void appendHistory(LinkedList<T> list, T item) {
        list.addLast(item);
        while (list.size() > MAX_HISTORY_POINTS) {
            list.removeFirst();
        }
    }

    public synchronized void clear() {
        windowRecords.clear();
        seenTxIds.clear();
        tpsHistory.clear();
        chanRatioHistory.clear();
        latencyHistory.clear();
        cumulativeTransactions.set(0);
        cumulativeChan.set(0);
        cumulativeXemXet.set(0);
        cumulativeChoQua.set(0);
    }
}
