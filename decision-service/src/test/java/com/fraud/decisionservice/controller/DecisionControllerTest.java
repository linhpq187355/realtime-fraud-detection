package com.fraud.decisionservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraud.common.model.Location;
import com.fraud.decisionservice.dto.FeatureDetails;
import com.fraud.decisionservice.dto.TransactionCheckRequest;
import com.fraud.decisionservice.dto.TransactionCheckResponse;
import com.fraud.decisionservice.exception.FeatureStateUnavailableException;
import com.fraud.decisionservice.exception.InvalidCardException;
import com.fraud.decisionservice.service.DecisionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DecisionController.class)
class DecisionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private DecisionService decisionService;

    @Test
    void testCheckTransactionSuccess() throws Exception {
        TransactionCheckRequest req = new TransactionCheckRequest(
                "tx-123", "card-0001", BigDecimal.valueOf(25_000_000), "ATM",
                new Location(21.0285, 105.8542), Instant.parse("2026-08-19T10:15:32Z")
        );

        FeatureDetails features = new FeatureDetails(2, BigDecimal.valueOf(27_000_000), BigDecimal.valueOf(6_000_000), BigDecimal.valueOf(3.1), false);
        TransactionCheckResponse resp = new TransactionCheckResponse("tx-123", "XEM_XET", 0.63, null, features, 18);

        when(decisionService.checkTransaction(any(TransactionCheckRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/check-transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("tx-123"))
                .andExpect(jsonPath("$.decision").value("XEM_XET"))
                .andExpect(jsonPath("$.riskScore").value(0.63))
                .andExpect(jsonPath("$.triggeredRule").doesNotExist())
                .andExpect(jsonPath("$.features.so_giao_dich_5_phut").value(2))
                .andExpect(jsonPath("$.features.tong_tien_1_gio").value(27000000))
                .andExpect(jsonPath("$.features.lech_so_voi_trung_binh").value(3.1))
                .andExpect(jsonPath("$.features.khoang_cach_bat_thuong").value(false))
                .andExpect(jsonPath("$.latencyMs").value(18));
    }

    @Test
    void testUnknownCardReturnsBadRequest400() throws Exception {
        TransactionCheckRequest req = new TransactionCheckRequest(
                "tx-unknown", "card-9999", BigDecimal.valueOf(100_000), "Shopee",
                new Location(21.0285, 105.8542), Instant.now()
        );

        when(decisionService.checkTransaction(any()))
                .thenThrow(new InvalidCardException("Unknown card: card-9999. Valid demo cards are card-0001 to card-0020."));

        mockMvc.perform(post("/check-transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Unknown card: card-9999. Valid demo cards are card-0001 to card-0020."));
    }

    @Test
    void testUnseededBaselineReturnsServiceUnavailable503() throws Exception {
        TransactionCheckRequest req = new TransactionCheckRequest(
                "tx-unseeded", "card-0001", BigDecimal.valueOf(100_000), "Shopee",
                new Location(21.0285, 105.8542), Instant.now()
        );

        when(decisionService.checkTransaction(any()))
                .thenThrow(new FeatureStateUnavailableException("Feature state unavailable for card: card-0001. Demo baselines not seeded."));

        mockMvc.perform(post("/check-transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Service Unavailable"))
                .andExpect(jsonPath("$.message").value("Feature state unavailable for card: card-0001. Demo baselines not seeded."));
    }
}
