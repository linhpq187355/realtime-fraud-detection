package com.fraud.dashboard.controller;

import com.fraud.dashboard.model.DashboardMetricsResponse;
import com.fraud.dashboard.model.DecisionMetricRecord;
import com.fraud.dashboard.service.RollingWindowMetricsStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/metrics")
public class DashboardApiController {

    private final RollingWindowMetricsStore metricsStore;

    public DashboardApiController(RollingWindowMetricsStore metricsStore) {
        this.metricsStore = metricsStore;
    }

    @GetMapping
    public ResponseEntity<DashboardMetricsResponse> getMetrics() {
        return ResponseEntity.ok(metricsStore.snapshotMetrics());
    }

    @PostMapping("/record")
    public ResponseEntity<Void> recordMetric(@RequestBody DecisionMetricRecord record) {
        if (record != null && record.getTransactionId() != null) {
            metricsStore.record(record);
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> resetMetrics() {
        metricsStore.clear();
        return ResponseEntity.ok().build();
    }
}
