package com.fraud.decisionservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class RulesLoader {

    private static final Logger log = LoggerFactory.getLogger(RulesLoader.class);

    private final ResourceLoader resourceLoader;
    private final String rulesPath;
    private final ObjectMapper yamlMapper;
    private RulesConfig rulesConfig;

    public RulesLoader(ResourceLoader resourceLoader, @Value("${fraud.rules.path:classpath:rules.yaml}") String rulesPath) {
        this.resourceLoader = resourceLoader;
        this.rulesPath = rulesPath;
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    @PostConstruct
    public void init() {
        loadRules();
    }

    public void loadRules() {
        try {
            log.info("Loading rules configuration from: {}", rulesPath);
            Resource resource = resourceLoader.getResource(rulesPath);
            if (!resource.exists()) {
                throw new IllegalStateException("Rules file not found at " + rulesPath);
            }
            try (InputStream is = resource.getInputStream()) {
                this.rulesConfig = yamlMapper.readValue(is, RulesConfig.class);
            }
            log.info("Successfully loaded {} rules and ML thresholds: chan > {}, xem_xet > {}",
                    rulesConfig.getRules().size(),
                    rulesConfig.getMlThresholds().getChanNeuDiemTren(),
                    rulesConfig.getMlThresholds().getXemXetNeuDiemTren());
        } catch (Exception e) {
            log.error("Failed to load rules configuration from {}: {}", rulesPath, e.getMessage(), e);
            throw new IllegalStateException("Could not load rules.yaml: " + e.getMessage(), e);
        }
    }

    public RulesConfig getRulesConfig() {
        return rulesConfig;
    }
}
