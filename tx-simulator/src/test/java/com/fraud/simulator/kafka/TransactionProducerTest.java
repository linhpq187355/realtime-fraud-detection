package com.fraud.simulator.kafka;

import com.fraud.common.constant.KafkaConstants;
import com.fraud.common.model.Location;
import com.fraud.common.model.TransactionEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TransactionProducerTest {

    @Test
    void testSendTransactionUsesCardIdAsKey() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, TransactionEvent> kafkaTemplate = mock(KafkaTemplate.class);
        TransactionProducer producer = new TransactionProducer(kafkaTemplate);

        TransactionEvent event = new TransactionEvent(
                "tx-123",
                "card-0005",
                BigDecimal.valueOf(500_000),
                "Shopee",
                new Location(21.0285, 105.8542),
                Instant.now()
        );

        ProducerRecord<String, TransactionEvent> record = new ProducerRecord<>(
                KafkaConstants.TOPIC_TRANSACTIONS, "card-0005", event
        );
        RecordMetadata metadata = new RecordMetadata(new TopicPartition(KafkaConstants.TOPIC_TRANSACTIONS, 0), 0, 0, 0, 0, 0);
        SendResult<String, TransactionEvent> sendResult = new SendResult<>(record, metadata);
        CompletableFuture<SendResult<String, TransactionEvent>> future = CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.send(eq(KafkaConstants.TOPIC_TRANSACTIONS), eq("card-0005"), any(TransactionEvent.class)))
                .thenReturn(future);

        producer.sendTransaction(event);

        verify(kafkaTemplate, times(1)).send(
                KafkaConstants.TOPIC_TRANSACTIONS,
                "card-0005",
                event
        );
    }
}
