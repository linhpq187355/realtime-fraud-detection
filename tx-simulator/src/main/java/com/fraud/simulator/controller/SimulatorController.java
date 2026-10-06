package com.fraud.simulator.controller;

import com.fraud.simulator.dto.*;
import com.fraud.simulator.model.CardProfile;
import com.fraud.simulator.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/simulator")
public class SimulatorController {

    private final TransactionService transactionService;
    private final DemoCardService demoCardService;
    private final ScenarioService scenarioService;
    private final AutoModeService autoModeService;
    private final SimulatorStatsService statsService;
    private final LiveFeedService liveFeedService;
    private final CityLocationService cityLocationService;

    public SimulatorController(TransactionService transactionService,
                               DemoCardService demoCardService,
                               ScenarioService scenarioService,
                               AutoModeService autoModeService,
                               SimulatorStatsService statsService,
                               LiveFeedService liveFeedService,
                               CityLocationService cityLocationService) {
        this.transactionService = transactionService;
        this.demoCardService = demoCardService;
        this.scenarioService = scenarioService;
        this.autoModeService = autoModeService;
        this.statsService = statsService;
        this.liveFeedService = liveFeedService;
        this.cityLocationService = cityLocationService;
    }

    @PostMapping("/manual-transaction")
    public ResponseEntity<SimulationResult> createManualTransaction(@RequestBody(required = false) ManualTransactionRequest request) {
        ManualTransactionRequest req = (request != null) ? request : new ManualTransactionRequest();
        SimulationResult result = transactionService.processTransaction(
                req.getCardId(),
                req.getAmount(),
                req.getMerchant(),
                req.getCity(),
                req.getLocation(),
                req.getTimestamp()
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/scenario/{name}")
    public ResponseEntity<ScenarioResponse> runScenario(@PathVariable("name") String name,
                                                       @RequestBody(required = false) Map<String, String> body,
                                                       @RequestParam(value = "cardId", required = false) String cardIdParam) {
        String cardId = cardIdParam;
        if (cardId == null && body != null) {
            cardId = body.get("cardId");
        }
        ScenarioResponse response = scenarioService.executeScenario(name, cardId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/auto-mode")
    public ResponseEntity<AutoModeRequest> updateAutoMode(@RequestBody(required = false) AutoModeRequest request) {
        AutoModeRequest req = (request != null) ? request : new AutoModeRequest(false, 5);
        autoModeService.setAutoMode(req.isEnabled(), req.getRatePerSecond());
        return ResponseEntity.ok(autoModeService.getStatus());
    }

    @GetMapping("/auto-mode")
    public ResponseEntity<AutoModeRequest> getAutoMode() {
        return ResponseEntity.ok(autoModeService.getStatus());
    }

    @GetMapping("/stats")
    public ResponseEntity<SimulatorStats> getStats() {
        return ResponseEntity.ok(statsService.getStats());
    }

    @GetMapping("/live-feed")
    public ResponseEntity<List<SimulationResult>> getLiveFeed() {
        return ResponseEntity.ok(liveFeedService.getRecentTransactions());
    }

    @GetMapping("/cards")
    public ResponseEntity<List<CardProfile>> getDemoCards() {
        return ResponseEntity.ok(demoCardService.getAllCards());
    }

    @GetMapping("/cities")
    public ResponseEntity<List<String>> getCities() {
        return ResponseEntity.ok(cityLocationService.getSupportedCities());
    }

    @GetMapping("/merchants")
    public ResponseEntity<List<String>> getMerchants() {
        return ResponseEntity.ok(List.of("Shopee", "Grab", "Circle K", "ATM Rút tiền", "Tiki", "Lazada", "Starbucks"));
    }
}
