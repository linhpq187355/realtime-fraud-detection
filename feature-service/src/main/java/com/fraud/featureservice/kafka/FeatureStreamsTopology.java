package com.fraud.featureservice.kafka;

import com.fraud.common.constant.KafkaConstants;
import com.fraud.common.model.CardFeatures;
import com.fraud.common.model.TransactionEvent;
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
                        return new WindowFeatureProcessor();
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

        private KeyValueStore<String, CardTxHistory> stateStore;
        private FixedKeyProcessorContext<String, CardFeatures> context;

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

            List<WindowTxRecord> records = history.getTransactions();
            if (records == null) {
                records = new ArrayList<>();
                history.setTransactions(records);
            }

            // Append current transaction
            records.add(new WindowTxRecord(
                    event.getTransactionId(),
                    eventTimeMs,
                    event.getAmount() != null ? event.getAmount() : BigDecimal.ZERO
            ));

            // Sort by event-time to maintain strict ordering
            records.sort(Comparator.comparingLong(WindowTxRecord::getTimestampEpochMs));

            // 1-hour horizon: prune records older than (eventTimeMs - 1 hour)
            long oneHourCutoff = eventTimeMs - ONE_HOUR_MS;
            records.removeIf(r -> r.getTimestampEpochMs() < oneHourCutoff);

            // 5-minute window count (including current event)
            long fiveMinutesCutoff = eventTimeMs - FIVE_MINUTES_MS;
            long count5Min = records.stream()
                    .filter(r -> r.getTimestampEpochMs() >= fiveMinutesCutoff)
                    .count();

            // 1-hour window sum (including current event)
            BigDecimal sum1Hour = records.stream()
                    .filter(r -> r.getTimestampEpochMs() >= oneHourCutoff)
                    .map(WindowTxRecord::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Save updated history to state store
            stateStore.put(cardId, history);

            CardFeatures features = new CardFeatures(
                    cardId,
                    count5Min,
                    sum1Hour,
                    event.getTransactionId(),
                    event.getTimestamp()
            );

            context.forward(record.withValue(features));
        }
    }
}
