package com.fraud.decisionservice.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraud.common.model.CardFeatures;
import com.fraud.common.model.Location;
import com.fraud.common.model.WindowTxRecord;
import com.fraud.common.util.HaversineUtil;
import com.fraud.decisionservice.dto.FeatureDetails;
import com.fraud.decisionservice.dto.TransactionCheckRequest;
import com.fraud.decisionservice.exception.FeatureStateUnavailableException;
import com.fraud.decisionservice.exception.InvalidCardException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Repository
public class RedisWindowStore {

    private static final Logger log = LoggerFactory.getLogger(RedisWindowStore.class);

    private static final String FEATURE_KEY_PREFIX = "features:";
    private static final String WINDOW_KEY_PREFIX = "tx:window:";
    private static final String DATA_KEY_PREFIX = "tx:data:";

    private static final long FIVE_MINUTES_MS = 5 * 60 * 1000L;
    private static final long ONE_HOUR_MS = 60 * 60 * 1000L;

    private static final Set<String> VALID_DEMO_CARDS = new HashSet<>();
    static {
        for (int i = 1; i <= 20; i++) {
            VALID_DEMO_CARDS.add(String.format("card-%04d", i));
        }
    }

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

    public RedisWindowStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        this.addAndPruneScript = new DefaultRedisScript<>(LUA_ADD_AND_PRUNE, Long.class);
    }

    public FeatureDetails resolveFeaturesAndRegister(TransactionCheckRequest request) {
        String cardId = request.getCardId();
        if (cardId == null || !VALID_DEMO_CARDS.contains(cardId.trim())) {
            throw new InvalidCardException("Unknown card: " + cardId + ". Valid demo cards are card-0001 to card-0020.");
        }
        cardId = cardId.trim();

        // 1. Read baseline from features:<cardId>
        BigDecimal baseline = readBaselineOrThrow(cardId);

        long eventTimeMs = request.getTimestamp() != null ? request.getTimestamp().toEpochMilli() : System.currentTimeMillis();
        BigDecimal amount = request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO;
        String txId = request.getTransactionId();
        Location location = request.getLocation();

        WindowTxRecord currentRecord = new WindowTxRecord(txId, eventTimeMs, amount, location);

        // 2. Concurrency-safe atomic registration in Redis ZSET + HASH and pruning > 1h
        String zsetKey = WINDOW_KEY_PREFIX + cardId;
        String hashKey = DATA_KEY_PREFIX + cardId;
        long cutoff1Hour = eventTimeMs - ONE_HOUR_MS;

        try {
            String payloadJson = objectMapper.writeValueAsString(currentRecord);
            redisTemplate.execute(
                    addAndPruneScript,
                    List.of(zsetKey, hashKey),
                    String.valueOf(eventTimeMs),
                    txId,
                    payloadJson,
                    String.valueOf(cutoff1Hour)
            );
        } catch (Exception e) {
            log.error("Failed to register transaction in Redis for card {}: {}", cardId, e.getMessage());
            throw new FeatureStateUnavailableException("Failed to update feature window state in Redis: " + e.getMessage(), e);
        }

        // 3. Query 1-hour window IDs from ZSET
        Set<String> txIds;
        try {
            txIds = redisTemplate.opsForZSet().rangeByScore(zsetKey, cutoff1Hour, Double.POSITIVE_INFINITY);
        } catch (Exception e) {
            throw new FeatureStateUnavailableException("Failed to read window from Redis for card " + cardId, e);
        }

        if (txIds == null || txIds.isEmpty()) {
            txIds = Collections.singleton(txId);
        }

        // 4. Query transaction payloads from HASH
        List<Object> hashValues;
        try {
            hashValues = redisTemplate.opsForHash().multiGet(hashKey, new ArrayList<>(txIds));
        } catch (Exception e) {
            throw new FeatureStateUnavailableException("Failed to read transaction data from Redis for card " + cardId, e);
        }

        List<WindowTxRecord> records = new ArrayList<>();
        if (hashValues != null) {
            for (Object obj : hashValues) {
                if (obj instanceof String jsonStr) {
                    try {
                        records.add(objectMapper.readValue(jsonStr, WindowTxRecord.class));
                    } catch (JsonProcessingException e) {
                        log.warn("Failed to deserialize WindowTxRecord JSON: {}", jsonStr);
                    }
                }
            }
        }

        // Ensure current record is in the list
        if (records.stream().noneMatch(r -> r.getTransactionId().equals(txId))) {
            records.add(currentRecord);
        }

        // Sort strictly by event-time order
        records.sort(Comparator.comparingLong(WindowTxRecord::getTimestampEpochMs));

        // 5. Calculate so_giao_dich_5_phut (exact count in [eventTimeMs - 5m, eventTimeMs])
        long cutoff5Min = eventTimeMs - FIVE_MINUTES_MS;
        long soGiaoDich5Phut = records.stream()
                .filter(r -> r.getTimestampEpochMs() >= cutoff5Min && r.getTimestampEpochMs() <= eventTimeMs)
                .count();

        // 6. Calculate tong_tien_1_gio (exact sum in [eventTimeMs - 1h, eventTimeMs])
        BigDecimal tongTien1Gio = records.stream()
                .filter(r -> r.getTimestampEpochMs() >= cutoff1Hour && r.getTimestampEpochMs() <= eventTimeMs)
                .map(WindowTxRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 7. Calculate khoang_cach_bat_thuong (against immediately preceding transaction in event-time order)
        WindowTxRecord prevTx = null;
        for (int i = records.size() - 1; i >= 0; i--) {
            WindowTxRecord r = records.get(i);
            if (!r.getTransactionId().equals(txId) && r.getTimestampEpochMs() <= eventTimeMs) {
                prevTx = r;
                break;
            }
        }

        boolean khoangCachBatThuong = false;
        if (prevTx != null && prevTx.getLocation() != null && location != null) {
            double distanceKm = HaversineUtil.calculateDistanceKm(prevTx.getLocation(), location);
            long elapsedMs = eventTimeMs - prevTx.getTimestampEpochMs();
            if (elapsedMs <= 0) {
                khoangCachBatThuong = distanceKm > 0.0;
            } else {
                double elapsedHours = elapsedMs / 3_600_000.0;
                double speedKmH = distanceKm / elapsedHours;
                khoangCachBatThuong = speedKmH > 900.0;
            }
        }

        // 8. Calculate lech_so_voi_trung_binh: (amount - baseline) / baseline
        BigDecimal lechSoVoiTrungBinh = BigDecimal.ZERO;
        if (baseline.compareTo(BigDecimal.ZERO) > 0 && amount != null) {
            lechSoVoiTrungBinh = amount.subtract(baseline)
                    .divide(baseline, 4, RoundingMode.HALF_UP);
        }

        return new FeatureDetails(
                soGiaoDich5Phut,
                tongTien1Gio,
                baseline,
                lechSoVoiTrungBinh,
                khoangCachBatThuong
        );
    }

    private BigDecimal readBaselineOrThrow(String cardId) {
        String key = FEATURE_KEY_PREFIX + cardId;
        String json;
        try {
            json = redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            throw new FeatureStateUnavailableException("Redis unavailable while fetching baseline for card " + cardId, e);
        }

        if (json == null || json.isBlank()) {
            throw new FeatureStateUnavailableException("Feature state unavailable for card: " + cardId + ". Demo baselines not seeded.");
        }

        try {
            CardFeatures features = objectMapper.readValue(json, CardFeatures.class);
            if (features.getTrungBinhLichSu() == null || features.getTrungBinhLichSu().compareTo(BigDecimal.ZERO) <= 0) {
                throw new FeatureStateUnavailableException("Historical average not seeded for card: " + cardId);
            }
            return features.getTrungBinhLichSu();
        } catch (FeatureStateUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new FeatureStateUnavailableException("Corrupted feature snapshot in Redis for card " + cardId, e);
        }
    }
}
