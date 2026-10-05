package com.fraud.decisionservice.service;

import com.fraud.common.model.Location;
import com.fraud.decisionservice.dto.FeatureDetails;
import com.fraud.decisionservice.dto.TransactionCheckRequest;
import com.fraud.decisionservice.dto.TransactionCheckResponse;
import com.fraud.decisionservice.ml.OnnxScoringService;
import com.fraud.decisionservice.rule.RuleEvaluationResult;
import com.fraud.decisionservice.rule.RuleEvaluator;
import com.fraud.decisionservice.storage.RedisWindowStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DecisionServiceTest {

    private RedisWindowStore mockWindowStore;
    private RuleEvaluator mockRuleEvaluator;
    private OnnxScoringService mockOnnxScoringService;
    private DecisionService decisionService;

    @BeforeEach
    void setUp() {
        mockWindowStore = mock(RedisWindowStore.class);
        mockRuleEvaluator = mock(RuleEvaluator.class);
        mockOnnxScoringService = mock(OnnxScoringService.class);
        decisionService = new DecisionService(mockWindowStore, mockRuleEvaluator, mockOnnxScoringService);
    }

    @Test
    void testRuleMatchDoesNotInvokeOnnx() {
        TransactionCheckRequest req = new TransactionCheckRequest(
                "tx-burst-6", "card-0001", BigDecimal.valueOf(200_000), "Shopee",
                new Location(21.0285, 105.8542), Instant.now()
        );

        FeatureDetails features = new FeatureDetails(6, BigDecimal.valueOf(1_200_000), BigDecimal.valueOf(350_000), BigDecimal.valueOf(-0.4286), false);
        when(mockWindowStore.resolveFeaturesAndRegister(req)).thenReturn(features);
        when(mockRuleEvaluator.evaluate(features, req.getAmount()))
                .thenReturn(RuleEvaluationResult.match("qua_nhieu_giao_dich", "CHAN"));

        TransactionCheckResponse response = decisionService.checkTransaction(req);

        assertEquals("tx-burst-6", response.getTransactionId());
        assertEquals("CHAN", response.getDecision());
        assertEquals("qua_nhieu_giao_dich", response.getTriggeredRule());
        assertNull(response.getRiskScore(), "riskScore must be null when rule matches");
        assertNotNull(response.getFeatures());
        assertEquals(6, response.getFeatures().getSoGiaoDich5Phut());
        assertTrue(response.getLatencyMs() >= 0);

        // Verify ONNX was NOT invoked
        verify(mockOnnxScoringService, never()).scoreTransaction(any(), any());
    }

    @Test
    void testNoRuleMatchInvokesOnnxAndMapsDecision() {
        TransactionCheckRequest req = new TransactionCheckRequest(
                "tx-normal-1", "card-0001", BigDecimal.valueOf(350_000), "Circle K",
                new Location(21.0285, 105.8542), Instant.now()
        );

        FeatureDetails features = new FeatureDetails(1, BigDecimal.valueOf(350_000), BigDecimal.valueOf(350_000), BigDecimal.ZERO, false);
        when(mockWindowStore.resolveFeaturesAndRegister(req)).thenReturn(features);
        when(mockRuleEvaluator.evaluate(features, req.getAmount())).thenReturn(RuleEvaluationResult.noMatch());
        when(mockOnnxScoringService.scoreTransaction(features, req.getAmount())).thenReturn(0.1250);
        when(mockOnnxScoringService.mapScoreToDecision(0.1250)).thenReturn("CHO_QUA");

        TransactionCheckResponse response = decisionService.checkTransaction(req);

        assertEquals("tx-normal-1", response.getTransactionId());
        assertEquals("CHO_QUA", response.getDecision());
        assertNull(response.getTriggeredRule(), "triggeredRule must be null when no rule matched");
        assertEquals(0.1250, response.getRiskScore());
        assertNotNull(response.getFeatures());
        assertTrue(response.getLatencyMs() >= 0);

        verify(mockOnnxScoringService, times(1)).scoreTransaction(features, req.getAmount());
    }
}
