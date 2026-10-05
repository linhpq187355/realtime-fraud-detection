package com.fraud.featureservice.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraud.common.model.CardFeatures;
import com.fraud.common.model.WindowTxRecord;
import com.fraud.featureservice.kafka.JsonSerde;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class RedisFeatureStore {

    private static final Logger log = LoggerFactory.getLogger(RedisFeatureStore.class);
    private static final String KEY_PREFIX = "features:";
    private static final String WINDOW_KEY_PREFIX = "tx:window:";
    private static final String DATA_KEY_PREFIX = "tx:data:";
    private static final long ONE_HOUR_MS = 60 * 60 * 1000L;

    private static final String LUA_ADD_AND_PRUNE =
            "redis.call('ZADD', KEYS[1], ARGV[1], ARGV[2])\n" +
            "redis.call('HSET', KEYS[2], ARGV[2], ARGV[3])\n" +
            "local expired = redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', '(' .. ARGV[4])\n" +
            "if #expired > 0 then\n" +
            "    redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', '(' .. ARGV[4])\n" +
            "    for i = 1, #expired do\n" +
            "        redis.call('HDEL', KEYS[2], expired[i])\n" +
            "    end\n" +
            "end\n" +
            "return 1";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final DefaultRedisScript<Long> addAndPruneScript;

    public RedisFeatureStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = JsonSerde.getObjectMapper();
        this.addAndPruneScript = new DefaultRedisScript<>(LUA_ADD_AND_PRUNE, Long.class);
    }

    public void addAndPruneTransaction(String cardId, WindowTxRecord record) {
        if (cardId == null || record == null || record.getTransactionId() == null) {
            return;
        }
        String zsetKey = WINDOW_KEY_PREFIX + cardId.trim();
        String hashKey = DATA_KEY_PREFIX + cardId.trim();
        try {
            String payloadJson = objectMapper.writeValueAsString(record);
            long score = record.getTimestampEpochMs();
            long cutoff1Hour = score - ONE_HOUR_MS;
            redisTemplate.execute(
                    addAndPruneScript,
                    List.of(zsetKey, hashKey),
                    String.valueOf(score),
                    record.getTransactionId(),
                    payloadJson,
                    String.valueOf(cutoff1Hour)
            );
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize WindowTxRecord for Redis: {}", record, e);
        } catch (Exception e) {
            log.error("Failed to execute atomic addAndPrune script for card {}: {}", cardId, e.getMessage());
        }
    }

    public void saveFeatures(CardFeatures features) {
        if (features == null || features.getCardId() == null) {
            return;
        }
        String key = KEY_PREFIX + features.getCardId();
        try {
            if (features.getTrungBinhLichSu() == null || BigDecimal.ZERO.compareTo(features.getTrungBinhLichSu()) == 0) {
                getFeatures(features.getCardId()).ifPresent(prev -> {
                    if (prev.getTrungBinhLichSu() != null && BigDecimal.ZERO.compareTo(prev.getTrungBinhLichSu()) < 0) {
                        features.setTrungBinhLichSu(prev.getTrungBinhLichSu());
                    }
                });
            }
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
