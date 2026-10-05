package com.fraud.decisionservice.controller;

import com.fraud.decisionservice.dto.TransactionCheckRequest;
import com.fraud.decisionservice.dto.TransactionCheckResponse;
import com.fraud.decisionservice.service.DecisionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DecisionController {

    private final DecisionService decisionService;

    public DecisionController(DecisionService decisionService) {
        this.decisionService = decisionService;
    }

    @PostMapping("/check-transaction")
    public ResponseEntity<TransactionCheckResponse> checkTransaction(@RequestBody TransactionCheckRequest request) {
        if (request == null || request.getTransactionId() == null || request.getCardId() == null) {
            return ResponseEntity.badRequest().build();
        }
        TransactionCheckResponse response = decisionService.checkTransaction(request);
        return ResponseEntity.ok(response);
    }
}
