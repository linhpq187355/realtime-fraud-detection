package com.fraud.simulator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraud.common.model.CardFeatures;
import com.fraud.simulator.model.CardProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class DemoCardRedisSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoCardRedisSeeder.class);
    private static final String FEATURE_KEY_PREFIX = "features:";

    private final DemoCardService demoCardService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public DemoCardRedisSeeder(DemoCardService demoCardService, StringRedisTemplate redisTemplate) {
        this.demoCardService = demoCardService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    }

    @Override
    public void run(ApplicationArguments args) {
        seedDemoCards();
    }

    public void seedDemoCards() {
        try {
            List<CardProfile> cards = demoCardService.getAllCards();
            log.info("Seeding {} demo cards into Redis...", cards.size());
            for (CardProfile card : cards) {
                String key = FEATURE_KEY_PREFIX + card.getCardId();
                Boolean exists = redisTemplate.hasKey(key);
                if (Boolean.FALSE.equals(exists)) {
                    CardFeatures features = new CardFeatures();
                    features.setCardId(card.getCardId());
                    features.setTrungBinhLichSu(card.getHistoricalAverageAmount());
                    features.setUpdatedAt(Instant.now());
                    String json = objectMapper.writeValueAsString(features);
                    redisTemplate.opsForValue().set(key, json);
                }
            }
            log.info("Successfully completed demo cards seeding in Redis.");
        } catch (Exception e) {
            log.error("Failed to seed demo cards into Redis: {}", e.getMessage(), e);
        }
    }
}
