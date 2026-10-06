package com.fraud.simulator.service;

import com.fraud.common.model.Location;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CityLocationService {

    private final Map<String, Location> cityMap = new LinkedHashMap<>();

    public CityLocationService() {
        cityMap.put("Hà Nội", new Location(21.0285, 105.8542));
        cityMap.put("TP.HCM", new Location(10.8231, 106.6297));
        cityMap.put("Đà Nẵng", new Location(16.0544, 108.2022));
        cityMap.put("Hải Phòng", new Location(20.8449, 106.6881));
        cityMap.put("Cần Thơ", new Location(10.0452, 105.7469));
        cityMap.put("Singapore", new Location(1.3521, 103.8198));
        cityMap.put("Tokyo", new Location(35.6762, 139.6503));
    }

    public List<String> getSupportedCities() {
        return new ArrayList<>(cityMap.keySet());
    }

    public Location resolveLocation(String city, Location fallbackLocation) {
        if (city != null) {
            String trimmed = city.trim();
            for (Map.Entry<String, Location> entry : cityMap.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(trimmed)) {
                    return entry.getValue();
                }
            }
        }
        if (fallbackLocation != null) {
            return fallbackLocation;
        }
        return cityMap.get("Hà Nội");
    }

    public String resolveCityName(Location location) {
        if (location == null) {
            return "Hà Nội";
        }
        for (Map.Entry<String, Location> entry : cityMap.entrySet()) {
            Location loc = entry.getValue();
            if (Math.abs(loc.getLat() - location.getLat()) < 0.05 && Math.abs(loc.getLon() - location.getLon()) < 0.05) {
                return entry.getKey();
            }
        }
        return String.format("%.2f, %.2f", location.getLat(), location.getLon());
    }
}
