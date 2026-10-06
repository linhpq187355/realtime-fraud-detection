package com.fraud.simulator.service;

import com.fraud.common.model.Location;
import com.fraud.simulator.dto.AutoModeRequest;
import com.fraud.simulator.model.CardProfile;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AutoModeService {

    private static final Logger log = LoggerFactory.getLogger(AutoModeService.class);

    private final TransactionService transactionService;
    private final DemoCardService demoCardService;
    private final CityLocationService cityLocationService;

    private final AtomicBoolean enabled = new AtomicBoolean(false);
    private final AtomicInteger ratePerSecond = new AtomicInteger(5);

    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> scheduledTask;
    private final Random random = new Random();

    private final List<String> merchants = List.of("Shopee", "Grab", "Circle K", "ATM", "Tiki", "Lazada", "Starbucks");

    public AutoModeService(TransactionService transactionService,
                           DemoCardService demoCardService,
                           CityLocationService cityLocationService) {
        this.transactionService = transactionService;
        this.demoCardService = demoCardService;
        this.cityLocationService = cityLocationService;
    }

    public synchronized void setAutoMode(boolean enable, int targetRate) {
        int boundedRate = Math.max(1, Math.min(500, targetRate));
        this.ratePerSecond.set(boundedRate);

        if (enable && !enabled.get()) {
            enabled.set(true);
            startAutoGeneration();
            log.info("Auto mode enabled at {} tx/sec", boundedRate);
        } else if (!enable && enabled.get()) {
            enabled.set(false);
            stopAutoGeneration();
            log.info("Auto mode disabled");
        } else if (enable && enabled.get()) {
            // Rate adjustment
            stopAutoGeneration();
            startAutoGeneration();
            log.info("Auto mode rate adjusted to {} tx/sec", boundedRate);
        }
    }

    public AutoModeRequest getStatus() {
        return new AutoModeRequest(enabled.get(), ratePerSecond.get());
    }

    private synchronized void startAutoGeneration() {
        if (executorService == null || executorService.isShutdown()) {
            int poolSize = Math.max(2, Math.min(8, Runtime.getRuntime().availableProcessors()));
            executorService = Executors.newScheduledThreadPool(poolSize, r -> {
                Thread t = new Thread(r, "auto-mode-generator");
                t.setDaemon(true);
                return t;
            });
        }

        long periodMicros = Math.max(1000L, 1_000_000L / ratePerSecond.get());
        scheduledTask = executorService.scheduleAtFixedRate(this::generateSingleTransaction, 0, periodMicros, TimeUnit.MICROSECONDS);
    }

    private synchronized void stopAutoGeneration() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            scheduledTask = null;
        }
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
    }

    private void generateSingleTransaction() {
        if (!enabled.get()) {
            return;
        }

        try {
            int cardIdx = 1 + random.nextInt(DemoCardService.DEMO_CARD_COUNT);
            String cardId = String.format("card-%04d", cardIdx);

            CardProfile profile = demoCardService.getCard(cardId).orElse(null);
            BigDecimal avg = (profile != null && profile.getHistoricalAverageAmount() != null)
                    ? profile.getHistoricalAverageAmount()
                    : BigDecimal.valueOf(500_000);

            // 95% regular variation, 5% spike
            BigDecimal amount;
            if (random.nextDouble() < 0.05) {
                amount = avg.multiply(BigDecimal.valueOf(15 + random.nextInt(10)));
            } else {
                double factor = 0.7 + (random.nextDouble() * 0.6);
                amount = avg.multiply(BigDecimal.valueOf(factor)).setScale(0, RoundingMode.HALF_UP);
            }

            String merchant = merchants.get(random.nextInt(merchants.size()));
            List<String> cities = cityLocationService.getSupportedCities();
            String city = cities.get(random.nextInt(cities.size()));
            Location loc = cityLocationService.resolveLocation(city, null);

            transactionService.processTransaction(cardId, amount, merchant, city, loc, Instant.now());
        } catch (Exception e) {
            log.debug("Auto mode generation error: {}", e.getMessage());
        }
    }

    @PreDestroy
    public synchronized void shutdown() {
        enabled.set(false);
        stopAutoGeneration();
    }
}
