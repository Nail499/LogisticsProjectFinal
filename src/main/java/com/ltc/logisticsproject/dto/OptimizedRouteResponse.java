package com.ltc.logisticsproject.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OptimizedRouteResponse {
    RouteStop start;
    List<RouteStop> orderedStops;
    Double totalDistanceKm;
    Double naiveDistanceKm;
    String routeSummary;
}
