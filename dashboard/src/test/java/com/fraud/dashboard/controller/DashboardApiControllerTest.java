package com.fraud.dashboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraud.dashboard.model.DashboardMetricsResponse;
import com.fraud.dashboard.model.DecisionMetricRecord;
import com.fraud.dashboard.service.RollingWindowMetricsStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardApiController.class)
class DashboardApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private RollingWindowMetricsStore metricsStore;

    @Test
    void shouldReturnDashboardMetrics() throws Exception {
        DashboardMetricsResponse response = new DashboardMetricsResponse(
                150, 15, 10, 125,
                12.5, 10.0, 5.0, 18.0,
                List.of(new DashboardMetricsResponse.TimeDataPoint("10:00:00", 12.5)),
                List.of(new DashboardMetricsResponse.TimeDataPoint("10:00:00", 10.0)),
                List.of(new DashboardMetricsResponse.LatencyDataPoint("10:00:00", 5.0, 18.0)),
                List.of(new DecisionMetricRecord("tx-001", "card-0001", BigDecimal.valueOf(100), "Shopee", "Hà Nội", Instant.now(), "CHAN", 0.95, null, 12L))
        );

        when(metricsStore.snapshotMetrics()).thenReturn(response);

        mockMvc.perform(get("/api/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTransactions").value(150))
                .andExpect(jsonPath("$.currentTps").value(12.5))
                .andExpect(jsonPath("$.chanRatio").value(10.0))
                .andExpect(jsonPath("$.p50LatencyMs").value(5.0))
                .andExpect(jsonPath("$.p99LatencyMs").value(18.0))
                .andExpect(jsonPath("$.top10RiskTransactions[0].transactionId").value("tx-001"));
    }

    @Test
    void shouldRecordMetricViaPost() throws Exception {
        DecisionMetricRecord rec = new DecisionMetricRecord(
                "tx-999", "card-0005", BigDecimal.valueOf(500_000), "Grab", "Hà Nội",
                Instant.now(), "CHO_QUA", 0.05, null, 8L
        );

        mockMvc.perform(post("/api/metrics/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rec)))
                .andExpect(status().isOk());

        verify(metricsStore).record(any(DecisionMetricRecord.class));
    }
}
