package com.fraud.decisionservice.storage;

import com.fraud.common.model.Location;
import com.fraud.decisionservice.dto.FeatureDetails;
import com.fraud.decisionservice.dto.TransactionCheckRequest;
import com.fraud.decisionservice.exception.FeatureStateUnavailableException;
import com.fraud.decisionservice.exception.InvalidCardException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisWindowStoreTest {

    private StringRedisTemplate mockRedisTemplate;
    private ValueOperations<String, String> mockValueOps;
    private ZSetOperations<String, String> mockZSetOps;
    private HashOperations<String, Object, Object> mockHashOps;
    private RedisWindowStore windowStore;

    @BeforeEach
    void setUp() {
        mockRedisTemplate = mock(StringRedisTemplate.class);
        mockValueOps = mock(ValueOperations.class);
        mockZSetOps = mock(ZSetOperations.class);
        mockHashOps = mock(HashOperations.class);

        when(mockRedisTemplate.opsForValue()).thenReturn(mockValueOps);
        when(mockRedisTemplate.opsForZSet()).thenReturn(mockZSetOps);
        when(mockRedisTemplate.opsForHash()).thenReturn(mockHashOps);

        windowStore = new RedisWindowStore(mockRedisTemplate);
    }

    @Test
    void testUnknownCardThrowsInvalidCardException() {
        TransactionCheckRequest req = new TransactionCheckRequest(
                "tx-1", "card-9999", BigDecimal.valueOf(100_000), "Shopee",
                new Location(21.0285, 105.8542), Instant.now()
        );

        assertThrows(InvalidCardException.class, () -> windowStore.resolveFeaturesAndRegister(req));
    }

    @Test
    void testUnseededCardThrowsFeatureStateUnavailableException() {
        TransactionCheckRequest req = new TransactionCheckRequest(
                "tx-1", "card-0001", BigDecimal.valueOf(100_000), "Shopee",
                new Location(21.0285, 105.8542), Instant.now()
        );

        when(mockValueOps.get("features:card-0001")).thenReturn(null);

        assertThrows(FeatureStateUnavailableException.class, () -> windowStore.resolveFeaturesAndRegister(req));
    }

    @Test
    void testExactSlidingWindowAndImpossibleTravel() {
        Instant t0 = Instant.parse("2026-09-29T10:00:00Z");
        Instant t1 = t0.plusSeconds(300); // 5 minutes later

        // Seeded baseline snapshot
        String baselineJson = "{\"cardId\":\"card-0001\",\"trungBinhLichSu\":350000}";
        when(mockValueOps.get("features:card-0001")).thenReturn(baselineJson);

        // Previous tx in Hanoi
        String prevTxJson = "{\"transactionId\":\"tx-prev\",\"timestampEpochMs\":" + t0.toEpochMilli() +
                ",\"amount\":200000,\"location\":{\"lat\":21.0285,\"lon\":105.8542}}";
        // Current tx in HCMC 5 minutes later (1140 km in 5 mins -> >900 km/h)
        String currTxJson = "{\"transactionId\":\"tx-curr\",\"timestampEpochMs\":" + t1.toEpochMilli() +
                ",\"amount\":400000,\"location\":{\"lat\":10.8231,\"lon\":106.6297}}";

        when(mockZSetOps.rangeByScore(eq("tx:window:card-0001"), anyDouble(), anyDouble()))
                .thenReturn(Set.of("tx-prev", "tx-curr"));
        when(mockHashOps.multiGet(eq("tx:data:card-0001"), anyCollection()))
                .thenReturn(List.of(prevTxJson, currTxJson));

        TransactionCheckRequest req = new TransactionCheckRequest(
                "tx-curr", "card-0001", BigDecimal.valueOf(400_000), "Grab",
                new Location(10.8231, 106.6297), t1
        );

        FeatureDetails features = windowStore.resolveFeaturesAndRegister(req);

        assertNotNull(features);
        assertEquals(2, features.getSoGiaoDich5Phut(), "Both transactions are within 5 minutes");
        assertEquals(0, BigDecimal.valueOf(600_000).compareTo(features.getTongTien1Gio()), "Sum should be 200k + 400k = 600k");
        assertTrue(features.isKhoangCachBatThuong(), "Hanoi to HCMC in 5 minutes must trigger impossible travel");
        assertEquals(0, BigDecimal.valueOf(350_000).compareTo(features.getTrungBinhLichSu()));
    }
}
