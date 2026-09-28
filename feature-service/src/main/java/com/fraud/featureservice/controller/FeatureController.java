package com.fraud.featureservice.controller;

import com.fraud.common.model.CardFeatures;
import com.fraud.featureservice.storage.RedisFeatureStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/features")
public class FeatureController {

    private final RedisFeatureStore redisFeatureStore;

    public FeatureController(RedisFeatureStore redisFeatureStore) {
        this.redisFeatureStore = redisFeatureStore;
    }

    @GetMapping("/{cardId}")
    public ResponseEntity<CardFeatures> getFeatures(@PathVariable String cardId) {
        return redisFeatureStore.getFeatures(cardId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
