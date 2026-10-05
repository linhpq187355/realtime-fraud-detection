package com.fraud.decisionservice.service;

import com.fraud.decisionservice.dto.FeatureDetails;
import com.fraud.decisionservice.dto.TransactionCheckRequest;
import com.fraud.decisionservice.dto.TransactionCheckResponse;
import com.fraud.decisionservice.ml.OnnxScoringService;
import com.fraud.decisionservice.rule.RuleEvaluationResult;
import com.fraud.decisionservice.rule.RuleEvaluator;
import com.fraud.decisionservice.storage.RedisWindowStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DecisionService {

    private static final Logger log = LoggerFactory.getLogger(DecisionService.class);

    private final RedisWindowStore redisWindowStore;
    private final RuleEvaluator ruleEvaluator;
    private final OnnxScoringService onnxScoringService;

    public DecisionService(RedisWindowStore redisWindowStore,
                           RuleEvaluator ruleEvaluator,
                           OnnxScoringService onnxScoringService) {
        this.redisWindowStore = redisWindowStore;
        this.ruleEvaluator = ruleEvaluator;
        this.onnxScoringService = onnxScoringService;
    }

    public TransactionCheckResponse checkTransaction(TransactionCheckRequest request) {
        long startTime = System.currentTimeMillis();

        // 1. Resolve current feature state using Redis ZSET + HASH
        FeatureDetails features = redisWindowStore.resolveFeaturesAndRegister(request);

        // 2. Evaluate YAML rules top-to-bottom
        RuleEvaluationResult ruleResult = ruleEvaluator.evaluate(features, request.getAmount());

        String decision;
        Double riskScore = null;
        String triggeredRule = null;

        if (ruleResult.isMatched()) {
            // First matching rule wins: ONNX must NOT run
            decision = ruleResult.getAction();
            triggeredRule = ruleResult.getRuleName();
            log.info("Transaction {} decided by rule {}: {}", request.getTransactionId(), triggeredRule, decision);
        } else {
            // No rule matched: run ONNX Runtime scoring model
            riskScore = onnxScoringService.scoreTransaction(features, request.getAmount());
            decision = onnxScoringService.mapScoreToDecision(riskScore);
            log.info("Transaction {} decided by ML model: riskScore={}, decision={}",
                    request.getTransactionId(), riskScore, decision);
        }

        long latencyMs = System.currentTimeMillis() - startTime;

        return new TransactionCheckResponse(
                request.getTransactionId(),
                decision,
                riskScore,
                triggeredRule,
                features,
                latencyMs
        );
    }
}
