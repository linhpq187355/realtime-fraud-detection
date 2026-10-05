package com.fraud.decisionservice.rule;

public class RuleEvaluationResult {
    private final boolean matched;
    private final String ruleName;
    private final String action;

    private RuleEvaluationResult(boolean matched, String ruleName, String action) {
        this.matched = matched;
        this.ruleName = ruleName;
        this.action = action;
    }

    public static RuleEvaluationResult match(String ruleName, String action) {
        return new RuleEvaluationResult(true, ruleName, action);
    }

    public static RuleEvaluationResult noMatch() {
        return new RuleEvaluationResult(false, null, null);
    }

    public boolean isMatched() {
        return matched;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getAction() {
        return action;
    }
}
