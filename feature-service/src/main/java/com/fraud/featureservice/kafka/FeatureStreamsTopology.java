package com.fraud.featureservice.kafka;

import com.fraud.common.constant.KafkaConstants;
import com.fraud.common.model.CardFeatures;
import com.fraud.common.model.TransactionEvent;
import com.fraud.common.util.HaversineUtil;
import com.fraud.featureservice.model.CardTxHistory;
import com.fraud.featureservice.model.WindowTxRecord;
import com.fraud.featureservice.storage.RedisFeatureStore;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.processor.api.FixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorContext;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorSupplier;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class FeatureStreamsTopology {

    private static final Logger log = LoggerFactory.getLogger(FeatureStreamsTopology.class);

    public static final String STORE_NAME = "card-tx-window-store";
    public static final long FIVE_MINUTES_MS = 5 * 60 * 1000L;
    public static final long ONE_HOUR_MS = 60 * 60 * 1000L;

    private final RedisFeatureStore redisFeatureStore;

    @Autowired
    public FeatureStreamsTopology(RedisFeatureStore redisFeatureStore) {
        this.redisFeatureStore = redisFeatureStore;
    }

    @Autowired
    public void configure(StreamsBuilder streamsBuilder) {
        buildPipeline(streamsBuilder, redisFeatureStore);
    }

    public static Topology buildPipeline(StreamsBuilder streamsBuilder, RedisFeatureStore redisFeatureStore) {
        StoreBuilder<KeyValueStore<String, CardTxHistory>> storeBuilder = Stores.keyValueStoreBuilder(
                Stores.inMemoryKeyValueStore(STORE_NAME),
                Serdes.String(),
                new JsonSerde<>(CardTxHistory.class)
        ).withLoggingDisabled();
        streamsBuilder.addStateStore(storeBuilder);

        KStream<String, TransactionEvent> transactionStream = streamsBuilder.stream(
                KafkaConstants.TOPIC_TRANSACTIONS,
                Consumed.with(Serdes.String(), new JsonSerde<>(TransactionEvent.class))
                        .withTimestampExtractor(new TransactionTimestampExtractor())
        );

        KStream<String, CardFeatures> featuresStream = transactionStream.processValues(
                new FixedKeyProcessorSupplier<String, TransactionEvent, CardFeatures>() {
                    @Override
                    public FixedKeyProcessor<String, TransactionEvent, CardFeatures> get() {
                        return new WindowFeatureProcessor(redisFeatureStore);
                    }
                },
                STORE_NAME
        );

        featuresStream.foreach((cardId, features) -> {
            log.info("Calculated features for cardId {}: so_giao_dich_5_phut={}, tong_tien_1_gio={}",
                    cardId, features.getSoGiaoDich5Phut(), features.getTongTien1Gio());
            if (redisFeatureStore != null) {
                redisFeatureStore.saveFeatures(features);
            }
        });

        return streamsBuilder.build();
    }

    private static class WindowFeatureProcessor implements FixedKeyProcessor<String, TransactionEvent, CardFeatures> {

        private final RedisFeatureStore redisFeatureStore;
        private KeyValueStore<String, CardTxHistory> stateStore;
        private FixedKeyProcessorContext<String, CardFeatures> context;

        public WindowFeatureProcessor(RedisFeatureStore redisFeatureStore) {
            this.redisFeatureStore = redisFeatureStore;
        }

        @Override
        public void init(FixedKeyProcessorContext<String, CardFeatures> context) {
            this.context = context;
            this.stateStore = context.getStateStore(STORE_NAME);
        }

        @Override
        public void process(FixedKeyRecord<String, TransactionEvent> record) {
            TransactionEvent event = record.value();
            if (event == null || event.getCardId() == null) {
                return;
            }

            String cardId = record.key() != null ? record.key() : event.getCardId();
            long eventTimeMs = record.timestamp();

            CardTxHistory history = stateStore.get(cardId);
            if (history == null) {
                history = new CardTxHistory();
            }

            // 5. khoang_cach_bat_thuong: compare with immediately previous transaction in event-time
            WindowTxRecord prevTx = history.getLastTransaction();
            boolean khoangCachBatThuong = false;
            if (prevTx != null && prevTx.getLocation() != null && event.getLocation() != null) {
                double distanceKm = HaversineUtil.calculateDistanceKm(prevTx.getLocation(), event.getLocation());
                long elapsedMs = eventTimeMs - prevTx.getTimestampEpochMs();
                if (elapsedMs <= 0) {
                    khoangCachBatThuong = distanceKm > 0;
                } else {
                    double elapsedHours = elapsedMs / 3_600_000.0;
                    double impliedSpeedKmH = distanceKm / elapsedHours;
                    khoangCachBatThuong = impliedSpeedKmH > 900.0;
                }
            }

            List<WindowTxRecord> records = history.getTransactions();
            if (records == null) {
                records = new ArrayList<>();
                history.setTransactions(records);
            }

            // Create and append current transaction
            WindowTxRecord currentRecord = new WindowTxRecord(
                    event.getTransactionId(),
                    eventTimeMs,
                    event.getAmount() != null ? event.getAmount() : BigDecimal.ZERO,
                    event.getLocation()
            );
            records.add(currentRecord);

            // Update last transaction on history
            history.setLastTransaction(currentRecord);

            // Sort by event-time to maintain strict ordering
            records.sort(Comparator.comparingLong(WindowTxRecord::getTimestampEpochMs));

            // 1-hour horizon: prune records older than (eventTimeMs - 1 hour)
            long oneHourCutoff = eventTimeMs - ONE_HOUR_MS;
            records.removeIf(r -> r.getTimestampEpochMs() < oneHourCutoff);

            // 1. so_giao_dich_5_phut: 5-minute window count (including current event)
            long fiveMinutesCutoff = eventTimeMs - FIVE_MINUTES_MS;
            long count5Min = records.stream()
                    .filter(r -> r.getTimestampEpochMs() >= fiveMinutesCutoff)
                    .count();

            // 2. tong_tien_1_gio: 1-hour window sum (including current event)
            BigDecimal sum1Hour = records.stream()
                    .filter(r -> r.getTimestampEpochMs() >= oneHourCutoff)
                    .map(WindowTxRecord::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Save updated history to internal in-memory state store
            stateStore.put(cardId, history);

            // Atomically sync to Redis ZSET/HASH without overwriting decision-service state
            if (redisFeatureStore != null) {
                redisFeatureStore.addAndPruneTransaction(cardId, currentRecord);
            }

            // 3. trung_binh_lich_su: read from pre-seeded Redis snapshot
            BigDecimal trungBinhLichSu = BigDecimal.ZERO;
            if (redisFeatureStore != null) {
                trungBinhLichSu = redisFeatureStore.getFeatures(cardId)
                        .map(CardFeatures::getTrungBinhLichSu)
                        .orElse(BigDecimal.ZERO);
            }

            // 4. lech_so_voi_trung_binh: (amount - trung_binh_lich_su) / trung_binh_lich_su
            BigDecimal amount = event.getAmount() != null ? event.getAmount() : BigDecimal.ZERO;
            BigDecimal lechSoVoiTrungBinh = BigDecimal.ZERO;
            if (trungBinhLichSu != null && trungBinhLichSu.compareTo(BigDecimal.ZERO) > 0) {
                lechSoVoiTrungBinh = amount.subtract(trungBinhLichSu)
                        .divide(trungBinhLichSu, 4, RoundingMode.HALF_UP);
            }

            CardFeatures features = new CardFeatures(
                    cardId,
                    count5Min,
                    sum1Hour,
                    trungBinhLichSu,
                    lechSoVoiTrungBinh,
                    khoangCachBatThuong,
                    event.getTransactionId(),
                    event.getTimestamp(),
                    event.getLocation(),
                    Instant.now()
            );

            context.forward(record.withValue(features));
        }
    }
}
