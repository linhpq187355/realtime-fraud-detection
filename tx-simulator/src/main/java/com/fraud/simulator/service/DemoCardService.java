package com.fraud.simulator.service;

import com.fraud.simulator.model.CardProfile;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class DemoCardService {

    public static final int DEMO_CARD_COUNT = 20;
    public static final int HISTORICAL_TX_COUNT = 30;

    private final Map<String, CardProfile> cardMap = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        initializeCards();
    }

    void initializeCards() {
        cardMap.clear();
        for (int i = 1; i <= DEMO_CARD_COUNT; i++) {
            String cardId = String.format("card-%04d", i);
            CardProfile profile = generateDeterministicProfile(cardId, i);
            cardMap.put(cardId, profile);
        }
    }

    private CardProfile generateDeterministicProfile(String cardId, int cardIndex) {
        Random random = new Random(1000L + cardIndex);
        long baseAmount = 200_000L + (cardIndex * 150_000L);
        BigDecimal total = BigDecimal.ZERO;

        for (int j = 0; j < HISTORICAL_TX_COUNT; j++) {
            // Variation between 70% and 130% of base amount
            double factor = 0.70 + (random.nextDouble() * 0.60);
            long txAmount = Math.round(baseAmount * factor);
            total = total.add(BigDecimal.valueOf(txAmount));
        }

        BigDecimal average = total.divide(BigDecimal.valueOf(HISTORICAL_TX_COUNT), 2, RoundingMode.HALF_UP);
        return new CardProfile(cardId, average, HISTORICAL_TX_COUNT, total);
    }

    public List<CardProfile> getAllCards() {
        return new ArrayList<>(cardMap.values());
    }

    public Optional<CardProfile> getCard(String cardId) {
        return Optional.ofNullable(cardMap.get(cardId));
    }

    public boolean cardExists(String cardId) {
        return cardMap.containsKey(cardId);
    }
}
