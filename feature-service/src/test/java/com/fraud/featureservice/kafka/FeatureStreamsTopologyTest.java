package com.fraud.featureservice.kafka;

import com.fraud.common.constant.KafkaConstants;
import com.fraud.common.model.CardFeatures;
import com.fraud.common.model.Location;
import com.fraud.common.model.TransactionEvent;
import com.fraud.featureservice.storage.RedisFeatureStore;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class FeatureStreamsTopologyTest {

    private TopologyTestDriver testDriver;
    private TestInputTopic<String, TransactionEvent> inputTopic;
    private RedisFeatureStore mockRedisStore;

    @BeforeEach
    void setUp() {
        mockRedisStore = mock(RedisFeatureStore.class);

        StreamsBuilder builder = new StreamsBuilder();
        Topology topology = FeatureStreamsTopology.buildPipeline(builder, mockRedisStore);

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test-feature-service");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class.getName());

        testDriver = new TopologyTestDriver(topology, props);

        inputTopic = testDriver.createInputTopic(
                KafkaConstants.TOPIC_TRANSACTIONS,
                new StringSerializer(),
                new JsonSerde<>(TransactionEvent.class).serializer()
        );
    }

    @AfterEach
    void tearDown() {
        if (testDriver != null) {
            testDriver.close();
        }
    }

    @Test
    void testEventTimeWindowingSemantics() {
        String cardId = "card-0001";
        Instant t0 = Instant.parse("2026-09-23T10:00:00Z");

        // T1: at t0, amount 100,000 -> count = 1, sum = 100,000
        TransactionEvent tx1 = new TransactionEvent(
                "tx-1", cardId, BigDecimal.valueOf(100_000), "Shopee",
                new Location(21.0285, 105.8542), t0
        );
        inputTopic.pipeInput(cardId, tx1, t0);

        // T2: at t0 + 2 min, amount 200,000 -> count = 2, sum = 300,000
        Instant t1 = t0.plusSeconds(120);
        TransactionEvent tx2 = new TransactionEvent(
                "tx-2", cardId, BigDecimal.valueOf(200_000), "Grab",
                new Location(21.0285, 105.8542), t1
        );
        inputTopic.pipeInput(cardId, tx2, t1);

        // T3: at t0 + 6 min, amount 150,000 -> count = 2 (tx1 expired from 5m), sum = 450,000 (all 3 in 1hr)
        Instant t2 = t0.plusSeconds(360);
        TransactionEvent tx3 = new TransactionEvent(
                "tx-3", cardId, BigDecimal.valueOf(150_000), "Circle K",
                new Location(21.0285, 105.8542), t2
        );
        inputTopic.pipeInput(cardId, tx3, t2);

        // T4: at t0 + 65 min, amount 50,000 -> count = 1, sum = 200,000 (tx1 and tx2 expired from 1hr; tx3 and tx4 remain)
        Instant t3 = t0.plusSeconds(3900);
        TransactionEvent tx4 = new TransactionEvent(
                "tx-4", cardId, BigDecimal.valueOf(50_000), "Shopee",
                new Location(21.0285, 105.8542), t3
        );
        inputTopic.pipeInput(cardId, tx4, t3);

        ArgumentCaptor<CardFeatures> captor = ArgumentCaptor.forClass(CardFeatures.class);
        verify(mockRedisStore, times(4)).saveFeatures(captor.capture());

        List<CardFeatures> captured = captor.getAllValues();
        assertEquals(4, captured.size());

        // Verification for T1
        CardFeatures f1 = captured.get(0);
        assertEquals(cardId, f1.getCardId());
        assertEquals(1, f1.getSoGiaoDich5Phut(), "T1: count should be 1");
        assertEquals(0, BigDecimal.valueOf(100_000).compareTo(f1.getTongTien1Gio()), "T1: sum should be 100,000");
        org.junit.jupiter.api.Assertions.assertFalse(f1.isKhoangCachBatThuong(), "T1: no previous transaction, impossible travel is false");
        org.junit.jupiter.api.Assertions.assertNotNull(f1.getTrungBinhLichSu());
        org.junit.jupiter.api.Assertions.assertNotNull(f1.getLechSoVoiTrungBinh());

        // Verification for T2 (+2 min)
        CardFeatures f2 = captured.get(1);
        assertEquals(cardId, f2.getCardId());
        assertEquals(2, f2.getSoGiaoDich5Phut(), "T2 (+2min): count should be 2");
        assertEquals(0, BigDecimal.valueOf(300_000).compareTo(f2.getTongTien1Gio()), "T2 (+2min): sum should be 300,000");
        org.junit.jupiter.api.Assertions.assertFalse(f2.isKhoangCachBatThuong(), "T2: same location, not impossible travel");

        // Verification for T3 (+6 min)
        CardFeatures f3 = captured.get(2);
        assertEquals(cardId, f3.getCardId());
        assertEquals(2, f3.getSoGiaoDich5Phut(), "T3 (+6min): count should be 2 (tx1 expired from 5m window)");
        assertEquals(0, BigDecimal.valueOf(450_000).compareTo(f3.getTongTien1Gio()), "T3 (+6min): sum should be 450,000");

        // Verification for T4 (+65 min)
        CardFeatures f4 = captured.get(3);
        assertEquals(cardId, f4.getCardId());
        assertEquals(1, f4.getSoGiaoDich5Phut(), "T4 (+65min): count should be 1");
        assertEquals(0, BigDecimal.valueOf(200_000).compareTo(f4.getTongTien1Gio()), "T4 (+65min): sum should be 200,000 (150,000 + 50,000)");
    }

    @Test
    void testImpossibleTravelDetection() {
        String cardId = "card-0002";
        Instant t0 = Instant.parse("2026-09-23T10:00:00Z");

        // T1: Hanoi (21.0285, 105.8542)
        TransactionEvent tx1 = new TransactionEvent(
                "tx-hanoi", cardId, BigDecimal.valueOf(100_000), "Shopee",
                new Location(21.0285, 105.8542), t0
        );
        inputTopic.pipeInput(cardId, tx1, t0);

        // T2: HCMC (10.8231, 106.6297) after 10 minutes (approx 1,140 km -> ~6840 km/h > 900 km/h)
        Instant t1 = t0.plusSeconds(600);
        TransactionEvent tx2 = new TransactionEvent(
                "tx-hcmc", cardId, BigDecimal.valueOf(200_000), "Grab",
                new Location(10.8231, 106.6297), t1
        );
        inputTopic.pipeInput(cardId, tx2, t1);

        // T3: HCMC (same city) after 5 minutes -> distance ~ 0 km -> speed 0 <= 900 km/h
        Instant t2 = t1.plusSeconds(300);
        TransactionEvent tx3 = new TransactionEvent(
                "tx-hcmc-2", cardId, BigDecimal.valueOf(150_000), "Circle K",
                new Location(10.8231, 106.6297), t2
        );
        inputTopic.pipeInput(cardId, tx3, t2);

        ArgumentCaptor<CardFeatures> captor = ArgumentCaptor.forClass(CardFeatures.class);
        verify(mockRedisStore, times(3)).saveFeatures(captor.capture());

        List<CardFeatures> captured = captor.getAllValues();
        assertEquals(3, captured.size());

        // T1: first tx, no previous tx -> false
        org.junit.jupiter.api.Assertions.assertFalse(captured.get(0).isKhoangCachBatThuong());

        // T2: Hanoi -> HCMC in 10 mins (>900 km/h) -> true
        org.junit.jupiter.api.Assertions.assertTrue(captured.get(1).isKhoangCachBatThuong());

        // T3: same location in HCMC -> false
        org.junit.jupiter.api.Assertions.assertFalse(captured.get(2).isKhoangCachBatThuong());
    }
}
