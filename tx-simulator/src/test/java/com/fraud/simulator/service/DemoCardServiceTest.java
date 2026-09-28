package com.fraud.simulator.service;

import com.fraud.simulator.model.CardProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DemoCardServiceTest {

    private DemoCardService demoCardService;

    @BeforeEach
    void setUp() {
        demoCardService = new DemoCardService();
        demoCardService.init();
    }

    @Test
    void testInitializes20Cards() {
        List<CardProfile> cards = demoCardService.getAllCards();
        assertEquals(20, cards.size(), "Must initialize exactly 20 demo cards");

        for (int i = 1; i <= 20; i++) {
            String expectedCardId = String.format("card-%04d", i);
            Optional<CardProfile> card = demoCardService.getCard(expectedCardId);
            assertTrue(card.isPresent(), "Card should exist: " + expectedCardId);
            assertEquals(expectedCardId, card.get().getCardId());
            assertEquals(30, card.get().getHistoricalTxCount());
            assertTrue(card.get().getHistoricalAverageAmount().compareTo(BigDecimal.ZERO) > 0,
                    "Historical average must be positive");
            assertTrue(card.get().getHistoricalTotalAmount().compareTo(BigDecimal.ZERO) > 0,
                    "Historical total must be positive");
        }
    }

    @Test
    void testDeterministicCardAverages() {
        DemoCardService secondService = new DemoCardService();
        secondService.init();

        for (int i = 1; i <= 20; i++) {
            String cardId = String.format("card-%04d", i);
            CardProfile card1 = demoCardService.getCard(cardId).orElseThrow();
            CardProfile card2 = secondService.getCard(cardId).orElseThrow();
            assertEquals(card1.getHistoricalAverageAmount(), card2.getHistoricalAverageAmount(),
                    "Averages should be deterministic for " + cardId);
        }
    }
}
