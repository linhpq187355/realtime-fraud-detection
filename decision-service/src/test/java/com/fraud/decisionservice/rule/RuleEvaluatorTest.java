package com.fraud.decisionservice.rule;

import com.fraud.decisionservice.config.RuleDefinition;
import com.fraud.decisionservice.config.RulesConfig;
import com.fraud.decisionservice.config.RulesLoader;
import com.fraud.decisionservice.dto.FeatureDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RuleEvaluatorTest {

    private RulesLoader mockRulesLoader;
    private RuleEvaluator ruleEvaluator;

    @BeforeEach
    void setUp() {
        mockRulesLoader = mock(RulesLoader.class);
        RulesConfig config = new RulesConfig();
        config.setRules(List.of(
                new RuleDefinition("qua_nhieu_giao_dich", "so_giao_dich_5_phut > 5", "CHAN"),
                new RuleDefinition("di_chuyen_bat_kha_thi", "khoang_cach_bat_thuong == true", "CHAN"),
                new RuleDefinition("chi_tieu_qua_cao_tuyet_doi", "amount > 50000000", "XEM_XET")
        ));
        when(mockRulesLoader.getRulesConfig()).thenReturn(config);
        ruleEvaluator = new RuleEvaluator(mockRulesLoader);
    }

    @Test
    void testRapidFireRuleMatches() {
        FeatureDetails features = new FeatureDetails(6, BigDecimal.valueOf(1_000_000), BigDecimal.valueOf(500_000), BigDecimal.ONE, false);
        RuleEvaluationResult result = ruleEvaluator.evaluate(features, BigDecimal.valueOf(200_000));

        assertTrue(result.isMatched());
        assertEquals("qua_nhieu_giao_dich", result.getRuleName());
        assertEquals("CHAN", result.getAction());
    }

    @Test
    void testImpossibleTravelRuleMatches() {
        FeatureDetails features = new FeatureDetails(2, BigDecimal.valueOf(1_000_000), BigDecimal.valueOf(500_000), BigDecimal.ONE, true);
        RuleEvaluationResult result = ruleEvaluator.evaluate(features, BigDecimal.valueOf(200_000));

        assertTrue(result.isMatched());
        assertEquals("di_chuyen_bat_kha_thi", result.getRuleName());
        assertEquals("CHAN", result.getAction());
    }

    @Test
    void testHighAmountRuleMatches() {
        FeatureDetails features = new FeatureDetails(1, BigDecimal.valueOf(60_000_000), BigDecimal.valueOf(500_000), BigDecimal.valueOf(100), false);
        RuleEvaluationResult result = ruleEvaluator.evaluate(features, BigDecimal.valueOf(60_000_000));

        assertTrue(result.isMatched());
        assertEquals("chi_tieu_qua_cao_tuyet_doi", result.getRuleName());
        assertEquals("XEM_XET", result.getAction());
    }

    @Test
    void testFirstMatchingRuleWinsPrecedence() {
        // Both so_giao_dich_5_phut > 5 AND amount > 50M
        FeatureDetails features = new FeatureDetails(7, BigDecimal.valueOf(100_000_000), BigDecimal.valueOf(500_000), BigDecimal.valueOf(100), false);
        RuleEvaluationResult result = ruleEvaluator.evaluate(features, BigDecimal.valueOf(60_000_000));

        assertTrue(result.isMatched());
        assertEquals("qua_nhieu_giao_dich", result.getRuleName(), "Rule 1 must win over Rule 3");
        assertEquals("CHAN", result.getAction());
    }

    @Test
    void testNoRuleMatchesFallthrough() {
        FeatureDetails features = new FeatureDetails(2, BigDecimal.valueOf(1_000_000), BigDecimal.valueOf(500_000), BigDecimal.ZERO, false);
        RuleEvaluationResult result = ruleEvaluator.evaluate(features, BigDecimal.valueOf(300_000));

        assertFalse(result.isMatched());
        assertNull(result.getRuleName());
        assertNull(result.getAction());
    }
}
