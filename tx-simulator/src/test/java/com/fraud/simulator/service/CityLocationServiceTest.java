package com.fraud.simulator.service;

import com.fraud.common.model.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CityLocationServiceTest {

    private CityLocationService cityLocationService;

    @BeforeEach
    void setUp() {
        cityLocationService = new CityLocationService();
    }

    @Test
    void shouldResolveKnownCities() {
        Location hanoi = cityLocationService.resolveLocation("Hà Nội", null);
        assertNotNull(hanoi);
        assertEquals(21.0285, hanoi.getLat(), 0.001);
        assertEquals(105.8542, hanoi.getLon(), 0.001);

        Location hcm = cityLocationService.resolveLocation("TP.HCM", null);
        assertNotNull(hcm);
        assertEquals(10.8231, hcm.getLat(), 0.001);
        assertEquals(106.6297, hcm.getLon(), 0.001);
    }

    @Test
    void shouldFallbackWhenCityUnknown() {
        Location custom = new Location(15.0, 108.0);
        Location resolved = cityLocationService.resolveLocation("Unknown City", custom);
        assertEquals(custom.getLat(), resolved.getLat());
        assertEquals(custom.getLon(), resolved.getLon());
    }

    @Test
    void shouldResolveCityNameFromCoordinates() {
        String name = cityLocationService.resolveCityName(new Location(21.0285, 105.8542));
        assertEquals("Hà Nội", name);

        String hcmName = cityLocationService.resolveCityName(new Location(10.8231, 106.6297));
        assertEquals("TP.HCM", hcmName);
    }
}
