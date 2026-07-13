package com.ltc.logisticsproject.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TripRequest {
    Long driverId;
    Long vehicleId;
    List<Long> cargoIds;
    Double estimatedDistanceKm;
    Double estimatedCost;
    String routeInfo;
}