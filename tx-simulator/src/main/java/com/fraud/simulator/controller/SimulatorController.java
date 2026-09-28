package com.fraud.simulator.controller;

import com.fraud.common.model.TransactionEvent;
import com.fraud.simulator.dto.ManualTransactionRequest;
import com.fraud.simulator.model.CardProfile;
import com.fraud.simulator.service.DemoCardService;
import com.fraud.simulator.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/simulator")
public class SimulatorController {

    private final TransactionService transactionService;
    private final DemoCardService demoCardService;

    public SimulatorController(TransactionService transactionService, DemoCardService demoCardService) {
        this.transactionService = transactionService;
        this.demoCardService = demoCardService;
    }

    @PostMapping("/manual-transaction")
    public ResponseEntity<TransactionEvent> createManualTransaction(@RequestBody(required = false) ManualTransactionRequest request) {
        ManualTransactionRequest req = (request != null) ? request : new ManualTransactionRequest();
        TransactionEvent event = transactionService.createAndSendTransaction(
                req.getCardId(),
                req.getAmount(),
                req.getMerchant(),
                req.getLocation(),
                req.getTimestamp()
        );
        return ResponseEntity.ok(event);
    }

    @GetMapping("/cards")
    public ResponseEntity<List<CardProfile>> getDemoCards() {
        return ResponseEntity.ok(demoCardService.getAllCards());
    }
}
