package com.fraud.featureservice.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraud.common.model.CardFeatures;
import com.fraud.featureservice.kafka.JsonSerde;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class RedisFeatureStore {

    private static final Logger log = LoggerFactory.getLogger(RedisFeatureStore.class);
    private static final String KEY_PREFIX = "features:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisFeatureStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = JsonSerde.getObjectMapper();
    }

    public void saveFeatures(CardFeatures features) {
        if (features == null || features.getCardId() == null) {
            return;
        }
        String key = KEY_PREFIX + features.getCardId();
        try {
            String json = objectMapper.writeValueAsString(features);
            redisTemplate.opsForValue().set(key, json);
            log.info("Saved features to Redis [key={}]: {}", key, json);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize CardFeatures to JSON: " + features, e);
        }
    }

    public Optional<CardFeatures> getFeatures(String cardId) {
        if (cardId == null || cardId.isBlank()) {
            return Optional.empty();
        }
        String key = KEY_PREFIX + cardId.trim();
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            CardFeatures features = objectMapper.readValue(json, CardFeatures.class);
            return Optional.of(features);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse CardFeatures JSON from Redis [key={}]: {}", key, json, e);
            throw new IllegalStateException("Corrupted feature data in Redis for key " + key, e);
        }
    }
}
