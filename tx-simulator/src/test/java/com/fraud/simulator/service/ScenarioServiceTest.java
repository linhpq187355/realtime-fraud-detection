package com.fraud.simulator.service;

import com.fraud.common.model.Location;
import com.fraud.simulator.dto.ScenarioResponse;
import com.fraud.simulator.dto.SimulationResult;
import com.fraud.simulator.model.CardProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScenarioServiceTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private DemoCardService demoCardService;

    private CityLocationService cityLocationService;
    private ScenarioService scenarioService;

    @BeforeEach
    void setUp() {
        cityLocationService = new CityLocationService();
        scenarioService = new ScenarioService(transactionService, demoCardService, cityLocationService);
    }

    @Test
    void shouldExecuteRapidFireScenarioWith10Transactions() {
        when(transactionService.processTransaction(eq("card-0001"), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> {
                    return new SimulationResult("tx-1", "card-0001", BigDecimal.valueOf(100_000), "Shopee", "Hà Nội",
                            null, Instant.now(), "CHO_QUA", null, null, null, 5L, "OK");
                });

        ScenarioResponse response = scenarioService.executeScenario("rapid-fire", "card-0001");
        assertNotNull(response);
        assertEquals("rapid-fire", response.getScenario());
        assertEquals(10, response.getResults().size());
        verify(transactionService, times(10)).processTransaction(eq("card-0001"), any(), any(), any(), any(), any());
    }

    @Test
    void shouldExecuteImpossibleTravelScenarioWith2Transactions() {
        when(transactionService.processTransaction(eq("card-0002"), any(), any(), eq("Hà Nội"), any(), any()))
                .thenReturn(new SimulationResult("tx-1", "card-0002", BigDecimal.valueOf(120_000), "Circle K", "Hà Nội",
                        new Location(21.0285, 105.8542), Instant.now(), "CHO_QUA", null, null, null, 5L, "OK"));

        when(transactionService.processTransaction(eq("card-0002"), any(), any(), eq("TP.HCM"), any(), any()))
                .thenReturn(new SimulationResult("tx-2", "card-0002", BigDecimal.valueOf(180_000), "Grab", "TP.HCM",
                        new Location(10.8231, 106.6297), Instant.now().plusSeconds(120), "CHAN", null, "di_chuyen_bat_kha_thi", null, 5L, "OK"));

        ScenarioResponse response = scenarioService.executeScenario("impossible-travel", "card-0002");
        assertNotNull(response);
        assertEquals("impossible-travel", response.getScenario());
        assertEquals(2, response.getResults().size());
        assertEquals("CHAN", response.getFinalDecision());
        assertTrue(response.getTriggeredOn().contains("di_chuyen_bat_kha_thi"));
    }

    @Test
    void shouldExecuteUnusualAmountScenario() {
        CardProfile profile = new CardProfile("card-0003", BigDecimal.valueOf(500_000), 30, BigDecimal.valueOf(15_000_000));
        when(demoCardService.getCard("card-0003")).thenReturn(Optional.of(profile));

        when(transactionService.processTransaction(eq("card-0003"), eq(BigDecimal.valueOf(10_000_000)), any(), any(), any(), any()))
                .thenReturn(new SimulationResult("tx-1", "card-0003", BigDecimal.valueOf(10_000_000), "ATM Rút tiền", "Đà Nẵng",
                        null, Instant.now(), "XEM_XET", 0.65, null, null, 10L, "OK"));

        ScenarioResponse response = scenarioService.executeScenario("unusual-amount", "card-0003");
        assertNotNull(response);
        assertEquals("unusual-amount", response.getScenario());
        assertEquals(1, response.getResults().size());
        assertEquals("XEM_XET", response.getFinalDecision());
    }

    @Test
    void shouldThrowOnUnknownScenario() {
        assertThrows(IllegalArgumentException.class, () -> {
            scenarioService.executeScenario("unknown-scenario", "card-0001");
        });
    }
}
