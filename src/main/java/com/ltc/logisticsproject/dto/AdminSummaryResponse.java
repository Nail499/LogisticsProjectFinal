package com.ltc.logisticsproject.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminSummaryResponse {
    long totalDrivers;
    long totalVehicles;
    long totalTrips;
    long deliveredTrips;
    long pendingApplications;
    long pendingCargo;
    double totalExpenses;
    long anomalyCount;
}