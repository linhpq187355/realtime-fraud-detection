package com.fraud.simulator.kafka;

import com.fraud.common.constant.KafkaConstants;
import com.fraud.common.model.TransactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class TransactionProducer {

    private static final Logger log = LoggerFactory.getLogger(TransactionProducer.class);

    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    public TransactionProducer(KafkaTemplate<String, TransactionEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<SendResult<String, TransactionEvent>> sendTransaction(TransactionEvent event) {
        String key = event.getCardId();
        log.info("Publishing transaction {} for cardId {} to topic {}",
                event.getTransactionId(), key, KafkaConstants.TOPIC_TRANSACTIONS);

        return kafkaTemplate.send(KafkaConstants.TOPIC_TRANSACTIONS, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish transaction {}: {}", event.getTransactionId(), ex.getMessage(), ex);
                    } else {
                        log.debug("Successfully published transaction {} to partition {}",
                                event.getTransactionId(), result.getRecordMetadata().partition());
                    }
                });
    }
}
