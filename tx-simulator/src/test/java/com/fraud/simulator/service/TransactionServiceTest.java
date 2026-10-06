package com.fraud.simulator.service;

import com.fraud.common.model.Location;
import com.fraud.common.model.TransactionEvent;
import com.fraud.simulator.client.DecisionServiceClient;
import com.fraud.simulator.dto.DecisionCheckResponse;
import com.fraud.simulator.dto.FeatureDetailsDto;
import com.fraud.simulator.dto.SimulationResult;
import com.fraud.simulator.kafka.TransactionProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionProducer transactionProducer;

    @Mock
    private DemoCardService demoCardService;

    @Mock
    private DecisionServiceClient decisionServiceClient;

    private SimulatorStatsService statsService;
    private LiveFeedService liveFeedService;
    private CityLocationService cityLocationService;
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        statsService = new SimulatorStatsService();
        liveFeedService = new LiveFeedService();
        cityLocationService = new CityLocationService();
        transactionService = new TransactionService(
                transactionProducer, demoCardService, decisionServiceClient,
                statsService, liveFeedService, cityLocationService
        );
    }

    @Test
    void shouldCreateAndSendTransactionDirectly() {
        TransactionEvent event = transactionService.createAndSendTransaction(
                "card-0001", BigDecimal.valueOf(100_000), "Shopee", new Location(21.0285, 105.8542), Instant.now()
        );

        assertNotNull(event);
        assertEquals("card-0001", event.getCardId());
        assertEquals(BigDecimal.valueOf(100_000), event.getAmount());

        verify(transactionProducer, times(1)).sendTransaction(any(TransactionEvent.class));
    }

    @Test
    void shouldProcessSimulationTransactionAndRecordStatsAndFeed() {
        FeatureDetailsDto features = new FeatureDetailsDto(1, BigDecimal.valueOf(250_000), BigDecimal.valueOf(200_000), BigDecimal.valueOf(0.25), false);
        DecisionCheckResponse checkResp = new DecisionCheckResponse(
                "tx-test", "CHO_QUA", 0.05, null, features, 6L
        );
        when(decisionServiceClient.checkTransaction(any())).thenReturn(checkResp);

        SimulationResult result = transactionService.processTransaction(
                "card-0001", BigDecimal.valueOf(250_000), "Shopee", "Hà Nội", null, Instant.now()
        );

        assertNotNull(result);
        assertEquals("card-0001", result.getCardId());
        assertEquals("CHO_QUA", result.getDecision());
        assertEquals(0.05, result.getRiskScore());
        assertEquals("Hà Nội", result.getCity());

        // Verify Kafka producer sent
        verify(transactionProducer, times(1)).sendTransaction(any(TransactionEvent.class));

        // Verify stats recorded
        assertEquals(1, statsService.getStats().getTotalTransactions());
        assertEquals(1, statsService.getStats().getTotalChoQua());

        // Verify live feed has entry
        assertEquals(1, liveFeedService.getRecentTransactions().size());
    }
}
