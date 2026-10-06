package com.fraud.loadtest;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class TransactionCheckSimulation extends Simulation {

    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8082");

    private static final List<String> MERCHANTS = List.of(
            "Shopee", "Grab", "Circle K", "ATM Rút tiền", "Tiki", "Lazada", "Starbucks"
    );

    private static final List<double[]> CITIES_COORDS = List.of(
            new double[]{21.0285, 105.8542}, // Hà Nội
            new double[]{10.8231, 106.6297}, // TP.HCM
            new double[]{16.0544, 108.2022}  // Đà Nẵng
    );

    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    private final Iterator<Map<String, Object>> feeder = Stream.generate((Supplier<Map<String, Object>>) () -> {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        int cardNum = rand.nextInt(1, 21);
        String cardId = String.format("card-%04d", cardNum);
        long amount = rand.nextLong(50_000, 25_000_001);
        String merchant = MERCHANTS.get(rand.nextInt(MERCHANTS.size()));
        double[] coords = CITIES_COORDS.get(rand.nextInt(CITIES_COORDS.size()));
        String txId = "tx-load-" + UUID.randomUUID().toString().substring(0, 8);
        String nowIso = Instant.now().toString();

        Map<String, Object> map = new HashMap<>();
        map.put("transactionId", txId);
        map.put("cardId", cardId);
        map.put("amount", amount);
        map.put("merchant", merchant);
        map.put("lat", coords[0]);
        map.put("lon", coords[1]);
        map.put("timestamp", nowIso);
        return map;
    }).iterator();

    private final ScenarioBuilder scn = scenario("Check Transaction Load Test")
            .feed(feeder)
            .exec(
                    http("POST /check-transaction")
                            .post("/check-transaction")
                            .body(StringBody(
                                    "{" +
                                            "\"transactionId\":\"#{transactionId}\"," +
                                            "\"cardId\":\"#{cardId}\"," +
                                            "\"amount\":#{amount}," +
                                            "\"merchant\":\"#{merchant}\"," +
                                            "\"location\":{\"lat\":#{lat},\"lon\":#{lon}}," +
                                            "\"timestamp\":\"#{timestamp}\"" +
                                            "}"
                            ))
                            .check(status().is(200))
                            .check(jsonPath("$.decision").exists())
            );

    {
        setUp(
                scn.injectOpen(
                        // Stage 1: Warm-up / baseline load at 50 tx/s for 15s
                        constantUsersPerSec(50).during(15),
                        // Stage 2: Medium load at 100 tx/s for 15s
                        constantUsersPerSec(100).during(15),
                        // Stage 3: High load at 250 tx/s for 15s
                        constantUsersPerSec(250).during(15),
                        // Stage 4: Peak load at 500 tx/s for 15s
                        constantUsersPerSec(500).during(15)
                )
        ).protocols(httpProtocol);
    }
}
