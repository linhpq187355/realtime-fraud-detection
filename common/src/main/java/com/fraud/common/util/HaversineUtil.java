package com.fraud.common.util;

import com.fraud.common.model.Location;

public final class HaversineUtil {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private HaversineUtil() {
    }

    /**
     * Calculates the great-circle distance between two locations in kilometers
     * using the Haversine formula.
     *
     * @param loc1 First location (lat, lon in degrees)
     * @param loc2 Second location (lat, lon in degrees)
     * @return Distance in kilometers, or 0.0 if either location is null
     */
    public static double calculateDistanceKm(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null) {
            return 0.0;
        }

        double lat1 = Math.toRadians(loc1.getLat());
        double lon1 = Math.toRadians(loc1.getLon());
        double lat2 = Math.toRadians(loc2.getLat());
        double lon2 = Math.toRadians(loc2.getLon());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));

        return EARTH_RADIUS_KM * c;
    }

    public static double distanceKm(Location loc1, Location loc2) {
        return calculateDistanceKm(loc1, loc2);
    }
}
