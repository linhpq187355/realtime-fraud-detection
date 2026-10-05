package com.fraud.decisionservice.ml;

import com.fraud.decisionservice.config.MlThresholds;
import com.fraud.decisionservice.config.RulesConfig;
import com.fraud.decisionservice.config.RulesLoader;
import com.fraud.decisionservice.dto.FeatureDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OnnxScoringServiceTest {

    private OnnxScoringService scoringService;
    private RulesLoader mockRulesLoader;

    @BeforeEach
    void setUp() {
        mockRulesLoader = mock(RulesLoader.class);
        RulesConfig config = new RulesConfig();
        config.setMlThresholds(new MlThresholds(0.8, 0.4));
        when(mockRulesLoader.getRulesConfig()).thenReturn(config);

        scoringService = new OnnxScoringService(
                new DefaultResourceLoader(),
                "classpath:model.onnx",
                mockRulesLoader
        );
        scoringService.init();
    }

    @AfterEach
    void tearDown() {
        if (scoringService != null) {
            scoringService.tearDown();
        }
    }

    @Test
    void testModelInferenceProducesNumericRiskScoreBetween0And1() {
        FeatureDetails features = new FeatureDetails(
                1,
                BigDecimal.valueOf(350_000),
                BigDecimal.valueOf(350_000),
                BigDecimal.ZERO,
                false
        );

        double score = scoringService.scoreTransaction(features, BigDecimal.valueOf(350_000));
        assertTrue(score >= 0.0 && score <= 1.0, "Risk score must be in range [0.0, 1.0]");
        assertFalse(Double.isNaN(score), "Risk score must not be NaN");
        assertFalse(Double.isInfinite(score), "Risk score must not be Infinite");
    }

    @Test
    void testThresholdMapping() {
        assertEquals("CHAN", scoringService.mapScoreToDecision(0.85));
        assertEquals("CHAN", scoringService.mapScoreToDecision(0.81));
        assertEquals("XEM_XET", scoringService.mapScoreToDecision(0.80));
        assertEquals("XEM_XET", scoringService.mapScoreToDecision(0.55));
        assertEquals("XEM_XET", scoringService.mapScoreToDecision(0.41));
        assertEquals("CHO_QUA", scoringService.mapScoreToDecision(0.40));
        assertEquals("CHO_QUA", scoringService.mapScoreToDecision(0.15));
        assertEquals("CHO_QUA", scoringService.mapScoreToDecision(0.00));
    }
}
