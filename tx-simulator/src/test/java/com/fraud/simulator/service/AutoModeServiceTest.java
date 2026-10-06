package com.fraud.simulator.service;

import com.fraud.simulator.dto.AutoModeRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AutoModeServiceTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private DemoCardService demoCardService;

    private CityLocationService cityLocationService;
    private AutoModeService autoModeService;

    @BeforeEach
    void setUp() {
        cityLocationService = new CityLocationService();
        autoModeService = new AutoModeService(transactionService, demoCardService, cityLocationService);
    }

    @AfterEach
    void tearDown() {
        autoModeService.shutdown();
    }

    @Test
    void shouldSupportAndBoundRateRange1To500() {
        // Test normal range within 1-500
        autoModeService.setAutoMode(false, 250);
        AutoModeRequest status = autoModeService.getStatus();
        assertEquals(250, status.getRatePerSecond());
        assertFalse(status.isEnabled());

        // Test boundary 500
        autoModeService.setAutoMode(false, 500);
        assertEquals(500, autoModeService.getStatus().getRatePerSecond());

        // Test upper bound clamping > 500 -> 500
        autoModeService.setAutoMode(false, 1000);
        assertEquals(500, autoModeService.getStatus().getRatePerSecond());

        // Test boundary 1
        autoModeService.setAutoMode(false, 1);
        assertEquals(1, autoModeService.getStatus().getRatePerSecond());

        // Test lower bound clamping < 1 -> 1
        autoModeService.setAutoMode(false, 0);
        assertEquals(1, autoModeService.getStatus().getRatePerSecond());
    }

    @Test
    void shouldToggleEnabledState() {
        autoModeService.setAutoMode(true, 10);
        assertTrue(autoModeService.getStatus().isEnabled());
        assertEquals(10, autoModeService.getStatus().getRatePerSecond());

        autoModeService.setAutoMode(false, 10);
        assertFalse(autoModeService.getStatus().isEnabled());
    }
}
