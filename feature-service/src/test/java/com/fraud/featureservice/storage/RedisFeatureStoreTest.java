package com.fraud.featureservice.storage;

import com.fraud.common.model.CardFeatures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RedisFeatureStoreTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RedisFeatureStore featureStore;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        featureStore = new RedisFeatureStore(redisTemplate);
    }

    @Test
    void testSaveFeaturesWritesToRedisKey() {
        CardFeatures features = new CardFeatures(
                "card-0001",
                3,
                BigDecimal.valueOf(750_000),
                "tx-999",
                Instant.parse("2026-09-23T12:00:00Z")
        );

        featureStore.saveFeatures(features);

        verify(valueOperations, times(1)).set(eq("features:card-0001"), contains("\"cardId\":\"card-0001\""));
    }

    @Test
    void testGetFeaturesParsesCorrectly() {
        String json = "{\"cardId\":\"card-0002\",\"soGiaoDich5Phut\":2,\"tongTien1Gio\":500000,\"lastTransactionId\":\"tx-123\",\"updatedAt\":\"2026-09-23T12:00:00Z\"}";
        when(valueOperations.get("features:card-0002")).thenReturn(json);

        Optional<CardFeatures> result = featureStore.getFeatures("card-0002");
        assertTrue(result.isPresent());
        assertEquals("card-0002", result.get().getCardId());
        assertEquals(2, result.get().getSoGiaoDich5Phut());
        assertEquals(0, BigDecimal.valueOf(500000).compareTo(result.get().getTongTien1Gio()));
    }

    @Test
    void testRedisErrorFailsVisiblyWithoutFallback() {
        CardFeatures features = new CardFeatures(
                "card-0003",
                1,
                BigDecimal.valueOf(100_000),
                "tx-001",
                Instant.now()
        );

        doThrow(new RedisConnectionFailureException("Connection refused"))
                .when(valueOperations).set(anyString(), anyString());

        assertThrows(RedisConnectionFailureException.class, () -> featureStore.saveFeatures(features),
                "Must propagate RedisConnectionFailureException rather than fallback silently");
    }
}
