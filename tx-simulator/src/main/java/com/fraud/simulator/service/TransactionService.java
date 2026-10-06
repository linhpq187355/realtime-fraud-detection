package com.fraud.simulator.service;

import com.fraud.common.model.Location;
import com.fraud.common.model.TransactionEvent;
import com.fraud.simulator.client.DecisionServiceClient;
import com.fraud.simulator.dto.DecisionCheckRequest;
import com.fraud.simulator.dto.DecisionCheckResponse;
import com.fraud.simulator.dto.SimulationResult;
import com.fraud.simulator.kafka.TransactionProducer;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionProducer transactionProducer;
    private final DemoCardService demoCardService;
    private final DecisionServiceClient decisionServiceClient;
    private final SimulatorStatsService statsService;
    private final LiveFeedService liveFeedService;
    private final CityLocationService cityLocationService;

    public TransactionService(TransactionProducer transactionProducer,
                              DemoCardService demoCardService,
                              DecisionServiceClient decisionServiceClient,
                              SimulatorStatsService statsService,
                              LiveFeedService liveFeedService,
                              CityLocationService cityLocationService) {
        this.transactionProducer = transactionProducer;
        this.demoCardService = demoCardService;
        this.decisionServiceClient = decisionServiceClient;
        this.statsService = statsService;
        this.liveFeedService = liveFeedService;
        this.cityLocationService = cityLocationService;
    }

    public TransactionEvent createAndSendTransaction(String cardId,
                                                    BigDecimal amount,
                                                    String merchant,
                                                    Location location,
                                                    Instant timestamp) {
        String effectiveCardId = (cardId != null && !cardId.isBlank()) ? cardId.trim() : "card-0001";
        BigDecimal effectiveAmount = (amount != null) ? amount : BigDecimal.valueOf(100_000);
        String effectiveMerchant = (merchant != null && !merchant.isBlank()) ? merchant.trim() : "Shopee";
        Location effectiveLocation = (location != null) ? location : new Location(21.0285, 105.8542);
        Instant effectiveTimestamp = (timestamp != null) ? timestamp : Instant.now();
        String transactionId = "tx-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        TransactionEvent event = new TransactionEvent(
                transactionId,
                effectiveCardId,
                effectiveAmount,
                effectiveMerchant,
                effectiveLocation,
                effectiveTimestamp
        );

        transactionProducer.sendTransaction(event);
        return event;
    }

    public SimulationResult processTransaction(String cardId,
                                              BigDecimal amount,
                                              String merchant,
                                              String city,
                                              Location location,
                                              Instant timestamp) {
        Location effectiveLocation = (location != null)
                ? location
                : cityLocationService.resolveLocation(city, null);
        String effectiveCity = (city != null && !city.isBlank())
                ? city.trim()
                : cityLocationService.resolveCityName(effectiveLocation);

        TransactionEvent event = createAndSendTransaction(cardId, amount, merchant, effectiveLocation, timestamp);

        DecisionCheckRequest checkRequest = new DecisionCheckRequest(
                event.getTransactionId(),
                event.getCardId(),
                event.getAmount(),
                event.getMerchant(),
                event.getLocation(),
                event.getTimestamp()
        );

        DecisionCheckResponse checkResponse = decisionServiceClient.checkTransaction(checkRequest);

        String decision = checkResponse != null ? checkResponse.getDecision() : "CHUA_XAC_DINH";
        Double riskScore = checkResponse != null ? checkResponse.getRiskScore() : null;
        String triggeredRule = checkResponse != null ? checkResponse.getTriggeredRule() : null;
        Long latencyMs = checkResponse != null ? checkResponse.getLatencyMs() : 0L;

        statsService.record(decision);

        SimulationResult result = new SimulationResult(
                event.getTransactionId(),
                event.getCardId(),
                event.getAmount(),
                event.getMerchant(),
                effectiveCity,
                event.getLocation(),
                event.getTimestamp(),
                decision,
                riskScore,
                triggeredRule,
                checkResponse != null ? checkResponse.getFeatures() : null,
                latencyMs,
                "OK"
        );

        liveFeedService.add(result);
        return result;
    }
}
