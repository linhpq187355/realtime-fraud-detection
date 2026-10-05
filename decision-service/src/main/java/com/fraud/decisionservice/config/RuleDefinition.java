package com.fraud.decisionservice.config;

public class RuleDefinition {
    private String name;
    private String condition;
    private String action;

    public RuleDefinition() {
    }

    public RuleDefinition(String name, String condition, String action) {
        this.name = name;
        this.condition = condition;
        this.action = action;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
