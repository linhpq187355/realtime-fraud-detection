package com.fraud.decisionservice.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class RulesConfig {

    private List<RuleDefinition> rules = new ArrayList<>();

    @JsonProperty("ml_thresholds")
    private MlThresholds mlThresholds = new MlThresholds();

    public RulesConfig() {
    }

    public RulesConfig(List<RuleDefinition> rules, MlThresholds mlThresholds) {
        this.rules = rules != null ? rules : new ArrayList<>();
        this.mlThresholds = mlThresholds != null ? mlThresholds : new MlThresholds();
    }

    public List<RuleDefinition> getRules() {
        return rules;
    }

    public void setRules(List<RuleDefinition> rules) {
        this.rules = rules != null ? rules : new ArrayList<>();
    }

    public MlThresholds getMlThresholds() {
        return mlThresholds;
    }

    public void setMlThresholds(MlThresholds mlThresholds) {
        this.mlThresholds = mlThresholds != null ? mlThresholds : new MlThresholds();
    }
}
