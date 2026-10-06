package com.fraud.simulator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraud.simulator.dto.*;
import com.fraud.simulator.model.CardProfile;
import com.fraud.simulator.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SimulatorController.class)
class SimulatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private DemoCardService demoCardService;

    @MockitoBean
    private ScenarioService scenarioService;

    @MockitoBean
    private AutoModeService autoModeService;

    @MockitoBean
    private SimulatorStatsService statsService;

    @MockitoBean
    private LiveFeedService liveFeedService;

    @MockitoBean
    private CityLocationService cityLocationService;

    @Test
    void shouldCreateManualTransaction() throws Exception {
        SimulationResult result = new SimulationResult(
                "tx-1234567890", "card-0001", BigDecimal.valueOf(250_000), "Shopee",
                "Hà Nội", null, Instant.now(), "CHO_QUA", 0.12, null, null, 8L, "OK"
        );
        when(transactionService.processTransaction(eq("card-0001"), eq(BigDecimal.valueOf(250_000)), eq("Shopee"), eq("Hà Nội"), any(), any()))
                .thenReturn(result);

        ManualTransactionRequest request = new ManualTransactionRequest();
        request.setCardId("card-0001");
        request.setAmount(BigDecimal.valueOf(250_000));
        request.setMerchant("Shopee");
        request.setCity("Hà Nội");

        mockMvc.perform(post("/simulator/manual-transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("tx-1234567890"))
                .andExpect(jsonPath("$.decision").value("CHO_QUA"))
                .andExpect(jsonPath("$.riskScore").value(0.12));
    }

    @Test
    void shouldRunScenario() throws Exception {
        ScenarioResponse response = new ScenarioResponse(
                "rapid-fire", "Quẹt dồn dập", "Giao dịch #6 bị CHAN", "CHAN", List.of()
        );
        when(scenarioService.executeScenario(eq("rapid-fire"), any())).thenReturn(response);

        mockMvc.perform(post("/simulator/scenario/rapid-fire")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenario").value("rapid-fire"))
                .andExpect(jsonPath("$.finalDecision").value("CHAN"));
    }

    @Test
    void shouldGetAndSetAutoMode() throws Exception {
        when(autoModeService.getStatus()).thenReturn(new AutoModeRequest(true, 10));

        mockMvc.perform(get("/simulator/auto-mode"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.ratePerSecond").value(10));

        AutoModeRequest req = new AutoModeRequest(false, 5);
        when(autoModeService.getStatus()).thenReturn(req);

        mockMvc.perform(post("/simulator/auto-mode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void shouldGetStats() throws Exception {
        when(statsService.getStats()).thenReturn(new SimulatorStats(100, 10, 5, 85));

        mockMvc.perform(get("/simulator/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTransactions").value(100))
                .andExpect(jsonPath("$.totalChan").value(10))
                .andExpect(jsonPath("$.totalXemXet").value(5))
                .andExpect(jsonPath("$.totalChoQua").value(85));
    }

    @Test
    void shouldGetLiveFeed() throws Exception {
        SimulationResult r = new SimulationResult("tx-1", "card-0001", BigDecimal.valueOf(500), "Shopee", "Hà Nội", null, Instant.now(), "CHO_QUA", null, null, null, 2L, "OK");
        when(liveFeedService.getRecentTransactions()).thenReturn(List.of(r));

        mockMvc.perform(get("/simulator/live-feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transactionId").value("tx-1"));
    }

    @Test
    void shouldGetCardsAndCities() throws Exception {
        when(demoCardService.getAllCards()).thenReturn(List.of(new CardProfile("card-0001", BigDecimal.valueOf(100), 30, BigDecimal.valueOf(3000))));
        when(cityLocationService.getSupportedCities()).thenReturn(List.of("Hà Nội", "TP.HCM"));

        mockMvc.perform(get("/simulator/cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cardId").value("card-0001"));

        mockMvc.perform(get("/simulator/cities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Hà Nội"));
    }

    @Test
    void shouldGetDecisionsSinceTimestamp() throws Exception {
        SimulationResult r = new SimulationResult("tx-1", "card-0001", BigDecimal.valueOf(500), "Shopee", "Hà Nội", null, Instant.now(), "CHO_QUA", null, null, null, 2L, "OK");
        when(liveFeedService.getDecisionsSince(1000L)).thenReturn(List.of(r));

        mockMvc.perform(get("/simulator/decisions").param("sinceTimestamp", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transactionId").value("tx-1"));
    }
}
