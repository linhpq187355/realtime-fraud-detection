package com.fraud.dashboard.service;

import com.fraud.dashboard.model.DecisionMetricRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class SimulatorMetricsCollector {

    private static final Logger log = LoggerFactory.getLogger(SimulatorMetricsCollector.class);

    private final RestClient restClient;
    private final RollingWindowMetricsStore metricsStore;
    private final String simulatorBaseUrl;

    private final AtomicLong lastPolledTimestamp = new AtomicLong(0);

    public SimulatorMetricsCollector(RestClient restClient,
                                     RollingWindowMetricsStore metricsStore,
                                     @Value("${simulator.service.url:http://localhost:8080}") String simulatorBaseUrl) {
        this.restClient = restClient;
        this.metricsStore = metricsStore;
        this.simulatorBaseUrl = simulatorBaseUrl;
    }

    @Scheduled(fixedDelay = 1000)
    public void pollDecisions() {
        try {
            long since = lastPolledTimestamp.get();
            String url = simulatorBaseUrl + "/simulator/decisions?sinceTimestamp=" + since;

            List<DecisionMetricRecord> records = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<DecisionMetricRecord>>() {});

            if (records != null && !records.isEmpty()) {
                long maxTimestamp = since;
                for (DecisionMetricRecord rec : records) {
                    metricsStore.record(rec);
                    if (rec.getTimestamp() != null) {
                        maxTimestamp = Math.max(maxTimestamp, rec.getTimestamp().toEpochMilli());
                    }
                }
                lastPolledTimestamp.set(maxTimestamp);
                log.debug("Polled {} decisions from simulator, updated cursor to {}", records.size(), maxTimestamp);
            }
        } catch (Exception e) {
            log.debug("Failed to poll decisions from simulator at {}: {}", simulatorBaseUrl, e.getMessage());
        }
    }

    public long getLastPolledTimestamp() {
        return lastPolledTimestamp.get();
    }

    public void setLastPolledTimestamp(long timestamp) {
        lastPolledTimestamp.set(timestamp);
    }
}
