package com.fraud.simulator.dto;

import java.util.List;

public class ScenarioResponse {
    private String scenario;
    private String description;
    private String triggeredOn;
    private String finalDecision;
    private List<SimulationResult> results;

    public ScenarioResponse() {
    }

    public ScenarioResponse(String scenario, String description, String triggeredOn, String finalDecision, List<SimulationResult> results) {
        this.scenario = scenario;
        this.description = description;
        this.triggeredOn = triggeredOn;
        this.finalDecision = finalDecision;
        this.results = results;
    }

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTriggeredOn() {
        return triggeredOn;
    }

    public void setTriggeredOn(String triggeredOn) {
        this.triggeredOn = triggeredOn;
    }

    public String getFinalDecision() {
        return finalDecision;
    }

    public void setFinalDecision(String finalDecision) {
        this.finalDecision = finalDecision;
    }

    public List<SimulationResult> getResults() {
        return results;
    }

    public void setResults(List<SimulationResult> results) {
        this.results = results;
    }
}
