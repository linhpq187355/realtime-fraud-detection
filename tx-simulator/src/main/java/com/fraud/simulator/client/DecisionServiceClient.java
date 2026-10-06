package com.fraud.simulator.client;

import com.fraud.simulator.dto.DecisionCheckRequest;
import com.fraud.simulator.dto.DecisionCheckResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;

@Component
public class DecisionServiceClient {

    private static final Logger log = LoggerFactory.getLogger(DecisionServiceClient.class);

    private final RestClient restClient;
    private final String baseUrl;

    public DecisionServiceClient(@Value("${decision.service.url:http://localhost:8082}") String decisionServiceUrl) {
        this.baseUrl = decisionServiceUrl.replaceAll("/+$", "");
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .baseUrl(this.baseUrl)
                .requestFactory(factory)
                .build();
    }

    public DecisionCheckResponse checkTransaction(DecisionCheckRequest request) {
        try {
            return restClient.post()
                    .uri("/check-transaction")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(DecisionCheckResponse.class);
        } catch (RestClientResponseException e) {
            log.error("Decision API returned HTTP status {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return new DecisionCheckResponse(
                    request.getTransactionId(),
                    "LOI_DECISION_API",
                    null,
                    "HTTP " + e.getStatusCode(),
                    null,
                    0
            );
        } catch (Exception e) {
            log.warn("Failed to reach Decision API at {}/check-transaction: {}", baseUrl, e.getMessage(), e);
            return new DecisionCheckResponse(
                    request.getTransactionId(),
                    "KHONG_THE_KET_NOI",
                    null,
                    "Không thể kết nối Decision API (port 8082): " + e.getMessage(),
                    null,
                    0
            );
        }
    }
}
