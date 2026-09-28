package com.fraud.featureservice.kafka;

import com.fraud.common.model.TransactionEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.TimestampExtractor;

public class TransactionTimestampExtractor implements TimestampExtractor {

    @Override
    public long extract(ConsumerRecord<Object, Object> record, long partitionTime) {
        Object value = record.value();
        if (value instanceof TransactionEvent event && event.getTimestamp() != null) {
            return event.getTimestamp().toEpochMilli();
        }
        if (record.timestamp() > 0) {
            return record.timestamp();
        }
        throw new IllegalStateException("Cannot extract event-time timestamp from record: " + record);
    }
}
