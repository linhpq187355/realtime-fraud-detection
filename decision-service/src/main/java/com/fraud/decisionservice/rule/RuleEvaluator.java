package com.fraud.decisionservice.rule;

import com.fraud.decisionservice.config.RuleDefinition;
import com.fraud.decisionservice.config.RulesConfig;
import com.fraud.decisionservice.config.RulesLoader;
import com.fraud.decisionservice.dto.FeatureDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class RuleEvaluator {

    private static final Logger log = LoggerFactory.getLogger(RuleEvaluator.class);

    private final RulesLoader rulesLoader;

    public RuleEvaluator(RulesLoader rulesLoader) {
        this.rulesLoader = rulesLoader;
    }

    public RuleEvaluationResult evaluate(FeatureDetails features, BigDecimal amount) {
        RulesConfig config = rulesLoader.getRulesConfig();
        if (config == null || config.getRules() == null || config.getRules().isEmpty()) {
            return RuleEvaluationResult.noMatch();
        }

        List<RuleDefinition> rules = config.getRules();
        for (RuleDefinition rule : rules) {
            if (matchesCondition(rule.getCondition(), features, amount)) {
                log.info("Rule matched: name={}, condition={}, action={}",
                        rule.getName(), rule.getCondition(), rule.getAction());
                return RuleEvaluationResult.match(rule.getName(), rule.getAction());
            }
        }

        return RuleEvaluationResult.noMatch();
    }

    boolean matchesCondition(String condition, FeatureDetails features, BigDecimal amount) {
        if (condition == null || condition.isBlank()) {
            return false;
        }

        String normalized = condition.replaceAll("\\s+", "");

        // 1. so_giao_dich_5_phut > X
        if (normalized.startsWith("so_giao_dich_5_phut>")) {
            try {
                long threshold = Long.parseLong(normalized.substring("so_giao_dich_5_phut>".length()));
                return features != null && features.getSoGiaoDich5Phut() > threshold;
            } catch (NumberFormatException e) {
                log.warn("Failed to parse rule threshold from: {}", condition);
                return false;
            }
        }

        // 2. khoang_cach_bat_thuong == true
        if (normalized.equalsIgnoreCase("khoang_cach_bat_thuong==true")) {
            return features != null && features.isKhoangCachBatThuong();
        }

        // 3. amount > X
        if (normalized.startsWith("amount>")) {
            try {
                BigDecimal threshold = new BigDecimal(normalized.substring("amount>".length()));
                return amount != null && amount.compareTo(threshold) > 0;
            } catch (Exception e) {
                log.warn("Failed to parse rule amount threshold from: {}", condition);
                return false;
            }
        }

        log.warn("Unsupported rule condition: {}", condition);
        return false;
    }
}
