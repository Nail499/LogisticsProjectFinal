package com.ltc.logisticsproject.service;

import org.springframework.stereotype.Service;

// Stage 6 — "simple simulated routing service". Real turn-by-turn routing
// (OSRM/Google Directions) is out of scope for this sandbox (no outbound
// network access to those providers), so this replaces the Stage 3 naive
// straight-line ETA with a slightly more realistic estimate: straight-line
// (haversine) distance inflated by a fixed road-curviness factor, since
// real roads are never perfectly straight. Both constants are documented
// estimates, not measured/calibrated values — swap this class out for a
// real routing client later without touching any caller.
@Service
public class RouteEstimationService {

    // Real road networks are typically 20-40% longer than straight-line
    // distance outside dense urban cores; 1.3x is a reasonable mid estimate.
    private static final double ROAD_DISTANCE_FACTOR = 1.3;

    // Blended assumption covering highway + city + loading stops.
    private static final double ASSUMED_AVG_SPEED_KMH = 45.0;

    public double estimateRoadDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        return haversineKm(lat1, lon1, lat2, lon2) * ROAD_DISTANCE_FACTOR;
    }

    public Integer estimateEtaMinutes(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) return null;
        double roadDistanceKm = estimateRoadDistanceKm(lat1, lon1, lat2, lon2);
        return (int) Math.round((roadDistanceKm / ASSUMED_AVG_SPEED_KMH) * 60);
    }

    // AdminReportService#getDispatcherKpis-də "vaxtında çatdırma faizi"
    // təxmini üçün — verilmiş məsafəni eyni orta sürət fərziyyəsi ilə
    // saata çevirir (ASSUMED_AVG_SPEED_KMH sahəsi private olduğu üçün
    // birbaşa sahə əvəzinə bu metod açılır).
    public double estimateTravelHours(double distanceKm) {
        return distanceKm / ASSUMED_AVG_SPEED_KMH;
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
