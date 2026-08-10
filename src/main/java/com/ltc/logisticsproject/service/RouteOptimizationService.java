package com.ltc.logisticsproject.service;

import com.ltc.logisticsproject.dto.OptimizedRouteResponse;
import com.ltc.logisticsproject.dto.RouteStop;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

// Çoxdayanacaqlı marşrut optimallaşdırması — bir reysdə birləşdirilmiş
// bir neçə yükün təhvil ünvanlarını hansı sırayla gəzməyin ən qısa ümumi
// məsafəyə gətirdiyini hesablayır (bax CargoQueue.jsx "Marşrutu
// optimallaşdır" düyməsi). Klassik iki addımlı TSP həlli istifadə olunur:
//   1) Nearest-neighbor: başlanğıc nöqtədən hər zaman ən yaxın gəzilməmiş
//      dayanacağı seçərək kobud (amma sürətli) ilkin marşrut qurur.
//   2) 2-opt: ilkin marşrutda hər iki kənarı yoxlayıb yerlərini
//      dəyişdirməklə (segmenti tərsinə çevirməklə) ümumi məsafəni azaldan
//      hər hansı təkmilləşdirmə tapılana qədər təkrarlayır — real
//      dispetçer sifarişlərində (adətən <20 dayanacaq) millisaniyələr
//      ərzində işləyir, tam optimal olmasa da nearest-neighbor-dan
//      həmişə eyni və ya daha yaxşı nəticə verir.
// Məsafə hesablaması mövcud RouteEstimationService-in (haversine x 1.3 yol
// əmsalı) üzərindən gedir ki, bütün layihədə eyni "təxmini yol məsafəsi"
// məntiqi işlənsin.
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RouteOptimizationService {

    final RouteEstimationService routeEstimationService;

    public OptimizedRouteResponse optimize(RouteStop start, List<RouteStop> destinations) {
        if (destinations == null || destinations.isEmpty()) {
            return OptimizedRouteResponse.builder()
                    .start(start).orderedStops(List.of()).totalDistanceKm(0.0).naiveDistanceKm(0.0)
                    .routeSummary(start != null ? start.getLabel() : "")
                    .build();
        }

        double naiveDistance = routeLength(start, destinations);

        List<RouteStop> route = nearestNeighbor(start, destinations);
        route = twoOpt(start, route);
        double optimizedDistance = routeLength(start, route);

        StringBuilder summary = new StringBuilder(start != null ? start.getLabel() : "Başlanğıc");
        for (RouteStop s : route) {
            summary.append(" → ").append(s.getLabel());
        }

        return OptimizedRouteResponse.builder()
                .start(start)
                .orderedStops(route)
                .totalDistanceKm(round1(optimizedDistance))
                .naiveDistanceKm(round1(naiveDistance))
                .routeSummary(summary.toString())
                .build();
    }

    private List<RouteStop> nearestNeighbor(RouteStop start, List<RouteStop> stops) {
        List<RouteStop> remaining = new ArrayList<>(stops);
        List<RouteStop> route = new ArrayList<>();
        RouteStop current = start;

        while (!remaining.isEmpty()) {
            RouteStop nearest = null;
            double bestDist = Double.MAX_VALUE;
            for (RouteStop candidate : remaining) {
                double d = distance(current, candidate);
                if (d < bestDist) {
                    bestDist = d;
                    nearest = candidate;
                }
            }
            route.add(nearest);
            remaining.remove(nearest);
            current = nearest;
        }
        return route;
    }

    private List<RouteStop> twoOpt(RouteStop start, List<RouteStop> initialRoute) {
        List<RouteStop> route = new ArrayList<>(initialRoute);
        int n = route.size();
        if (n < 3) return route;

        boolean improved = true;
        int maxIterations = 200; // kiçik sifariş sayları üçün kifayət qədər, sonsuz dövrənin qarşısını alır
        int iterations = 0;

        while (improved && iterations < maxIterations) {
            improved = false;
            iterations++;
            for (int i = 0; i < n - 1; i++) {
                for (int j = i + 1; j < n; j++) {
                    List<RouteStop> candidate = twoOptSwap(route, i, j);
                    if (routeLength(start, candidate) < routeLength(start, route) - 1e-9) {
                        route = candidate;
                        improved = true;
                    }
                }
            }
        }
        return route;
    }

    private List<RouteStop> twoOptSwap(List<RouteStop> route, int i, int j) {
        List<RouteStop> newRoute = new ArrayList<>(route.subList(0, i));
        List<RouteStop> reversed = new ArrayList<>(route.subList(i, j + 1));
        java.util.Collections.reverse(reversed);
        newRoute.addAll(reversed);
        newRoute.addAll(route.subList(j + 1, route.size()));
        return newRoute;
    }

    private double routeLength(RouteStop start, List<RouteStop> stops) {
        double total = 0;
        RouteStop current = start;
        for (RouteStop s : stops) {
            total += distance(current, s);
            current = s;
        }
        return total;
    }

    private double distance(RouteStop a, RouteStop b) {
        if (a == null || b == null || a.getLat() == null || a.getLng() == null || b.getLat() == null || b.getLng() == null) {
            return 0;
        }
        return routeEstimationService.estimateRoadDistanceKm(a.getLat(), a.getLng(), b.getLat(), b.getLng());
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
