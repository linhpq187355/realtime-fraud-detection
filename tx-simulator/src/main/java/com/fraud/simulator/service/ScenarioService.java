package com.fraud.simulator.service;

import com.fraud.common.model.Location;
import com.fraud.simulator.dto.ScenarioResponse;
import com.fraud.simulator.dto.SimulationResult;
import com.fraud.simulator.model.CardProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class ScenarioService {

    private static final Logger log = LoggerFactory.getLogger(ScenarioService.class);

    private final TransactionService transactionService;
    private final DemoCardService demoCardService;
    private final CityLocationService cityLocationService;

    public ScenarioService(TransactionService transactionService,
                           DemoCardService demoCardService,
                           CityLocationService cityLocationService) {
        this.transactionService = transactionService;
        this.demoCardService = demoCardService;
        this.cityLocationService = cityLocationService;
    }

    public ScenarioResponse executeScenario(String scenarioName, String requestedCardId) {
        if (scenarioName == null) {
            throw new IllegalArgumentException("Scenario name cannot be null");
        }

        String normalized = scenarioName.trim().toLowerCase();
        return switch (normalized) {
            case "rapid-fire", "quet-don-dap" -> executeRapidFire(requestedCardId);
            case "impossible-travel", "di-chuyen-bat-kha-thi" -> executeImpossibleTravel(requestedCardId);
            case "unusual-amount", "chi-tieu-bat-thuong" -> executeUnusualAmount(requestedCardId);
            default -> throw new IllegalArgumentException("Unknown scenario: " + scenarioName +
                    ". Supported: rapid-fire, impossible-travel, unusual-amount");
        };
    }

    private ScenarioResponse executeRapidFire(String cardId) {
        String effectiveCardId = (cardId != null && !cardId.isBlank()) ? cardId.trim() : "card-0001";
        log.info("Executing rapid-fire scenario on card {}", effectiveCardId);

        List<SimulationResult> results = new ArrayList<>();
        Instant baseTime = Instant.now();
        String triggeredOn = null;
        String finalDecision = "CHO_QUA";

        for (int i = 1; i <= 10; i++) {
            Instant txTime = baseTime.plusMillis(i * 100L);
            BigDecimal amount = BigDecimal.valueOf(100_000);
            SimulationResult result = transactionService.processTransaction(
                    effectiveCardId,
                    amount,
                    "Shopee",
                    "Hà Nội",
                    cityLocationService.resolveLocation("Hà Nội", null),
                    txTime
            );
            results.add(result);

            if ("CHAN".equalsIgnoreCase(result.getDecision())) {
                finalDecision = "CHAN";
                if (triggeredOn == null) {
                    triggeredOn = String.format("Giao dịch #%d bắt đầu bị CHAN (quy tắc: %s)",
                            i, result.getTriggeredRule() != null ? result.getTriggeredRule() : "vượt ngưỡng tần suất");
                }
            }
        }

        if (triggeredOn == null) {
            triggeredOn = "Tất cả giao dịch được thông qua";
        }

        return new ScenarioResponse(
                "rapid-fire",
                "Quẹt dồn dập: Gửi 10 giao dịch liên tiếp trong thời gian ngắn cho cùng 1 thẻ (" + effectiveCardId + ")",
                triggeredOn,
                finalDecision,
                results
        );
    }

    private ScenarioResponse executeImpossibleTravel(String cardId) {
        String effectiveCardId = (cardId != null && !cardId.isBlank()) ? cardId.trim() : "card-0002";
        log.info("Executing impossible-travel scenario on card {}", effectiveCardId);

        List<SimulationResult> results = new ArrayList<>();
        Instant t1Time = Instant.now();
        Instant t2Time = t1Time.plusSeconds(120); // 2 minutes later

        // Tx 1 in Hà Nội
        SimulationResult r1 = transactionService.processTransaction(
                effectiveCardId,
                BigDecimal.valueOf(120_000),
                "Circle K",
                "Hà Nội",
                cityLocationService.resolveLocation("Hà Nội", null),
                t1Time
        );
        results.add(r1);

        // Tx 2 in TP.HCM (1140km away, only 2 minutes elapsed -> speed > 34,000 km/h)
        SimulationResult r2 = transactionService.processTransaction(
                effectiveCardId,
                BigDecimal.valueOf(180_000),
                "Grab",
                "TP.HCM",
                cityLocationService.resolveLocation("TP.HCM", null),
                t2Time
        );
        results.add(r2);

        String triggeredOn;
        String finalDecision = r2.getDecision();

        if ("CHAN".equalsIgnoreCase(r2.getDecision())) {
            triggeredOn = String.format("Giao dịch #2 tại TP.HCM bị CHAN ngay lập tức (quy tắc: %s)",
                    r2.getTriggeredRule() != null ? r2.getTriggeredRule() : "di_chuyen_bat_kha_thi");
        } else {
            triggeredOn = "Giao dịch #2 có quyết định: " + r2.getDecision();
        }

        return new ScenarioResponse(
                "impossible-travel",
                "Impossible travel: Giao dịch 1 tại Hà Nội, giao dịch 2 sau đó 2 phút tại TP.HCM (khoảng cách 1,140km, thẻ " + effectiveCardId + ")",
                triggeredOn,
                finalDecision,
                results
        );
    }

    private ScenarioResponse executeUnusualAmount(String cardId) {
        String effectiveCardId = (cardId != null && !cardId.isBlank()) ? cardId.trim() : "card-0003";
        log.info("Executing unusual-amount scenario on card {}", effectiveCardId);

        BigDecimal avgAmount = BigDecimal.valueOf(500_000);
        CardProfile profile = demoCardService.getCard(effectiveCardId).orElse(null);
        if (profile != null && profile.getHistoricalAverageAmount() != null) {
            avgAmount = profile.getHistoricalAverageAmount();
        }

        BigDecimal unusualAmount = avgAmount.multiply(BigDecimal.valueOf(20));

        SimulationResult result = transactionService.processTransaction(
                effectiveCardId,
                unusualAmount,
                "ATM Rút tiền",
                "Đà Nẵng",
                cityLocationService.resolveLocation("Đà Nẵng", null),
                Instant.now()
        );

        List<SimulationResult> results = List.of(result);
        String triggeredOn = String.format("Giao dịch %s VNĐ (gấp 20 lần trung bình lịch sử %s VNĐ) -> Quyết định: %s%s",
                unusualAmount.toPlainString(),
                avgAmount.toPlainString(),
                result.getDecision(),
                result.getTriggeredRule() != null ? " (Quy tắc: " + result.getTriggeredRule() + ")" :
                        (result.getRiskScore() != null ? " (Điểm ML: " + result.getRiskScore() + ")" : ""));

        return new ScenarioResponse(
                "unusual-amount",
                "Chi tiêu bất thường: Gửi 1 giao dịch có số tiền gấp 20 lần trung bình lịch sử của thẻ " + effectiveCardId,
                triggeredOn,
                result.getDecision(),
                results
        );
    }
}
