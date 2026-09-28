package com.fraud.simulator.service;

import com.fraud.common.model.Location;
import com.fraud.common.model.TransactionEvent;
import com.fraud.simulator.kafka.TransactionProducer;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionProducer transactionProducer;
    private final DemoCardService demoCardService;

    public TransactionService(TransactionProducer transactionProducer, DemoCardService demoCardService) {
        this.transactionProducer = transactionProducer;
        this.demoCardService = demoCardService;
    }

    public TransactionEvent createAndSendTransaction(String cardId,
                                                    BigDecimal amount,
                                                    String merchant,
                                                    Location location,
                                                    Instant timestamp) {
        String effectiveCardId = (cardId != null && !cardId.isBlank()) ? cardId.trim() : "card-0001";
        BigDecimal effectiveAmount = (amount != null) ? amount : BigDecimal.valueOf(100_000);
        String effectiveMerchant = (merchant != null && !merchant.isBlank()) ? merchant.trim() : "Shopee";
        Location effectiveLocation = (location != null) ? location : new Location(21.0285, 105.8542);
        Instant effectiveTimestamp = (timestamp != null) ? timestamp : Instant.now();
        String transactionId = "tx-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        TransactionEvent event = new TransactionEvent(
                transactionId,
                effectiveCardId,
                effectiveAmount,
                effectiveMerchant,
                effectiveLocation,
                effectiveTimestamp
        );

        transactionProducer.sendTransaction(event);
        return event;
    }
}
