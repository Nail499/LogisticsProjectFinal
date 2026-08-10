package com.ltc.logisticsproject.dto;

import com.ltc.logisticsproject.entity.TripStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

// One "blip" on the Dispatcher Control Tower map (Stage 4). Built from a
// Trip + its most recent TrackingLog ping. Kept separate from the raw Trip
// entity so we never leak driver/vehicle internals beyond what the map
// actually needs.
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LiveTripResponse {
    Long tripId;
    TripStatus status;

    String driverName;
    String driverPhone;
    String vehiclePlate;
    String trailerPlate;

    Double lastLatitude;
    Double lastLongitude;
    String lastUpdatedAt;

    String pickupAddress;
    String destinationAddress;
    Double destinationLatitude;
    Double destinationLongitude;

    String routeInfo;
    Double estimatedDistanceKm;
    Double estimatedCost;

    // Reysin "ətraflı görünüş" kartlarında (Sürücü/Dispetçer) göstərilir —
    // bax DriverController#tripHistory / TripDetailModal.jsx.
    String startedAt;
    String deliveredAt;

    // Yükü verən müştəri(lər) — Control Tower/Bütün reyslər-də klikləyəndə
    // ətraflı məlumat göstərmək üçün (bax CustomerSummary).
    List<CustomerSummary> customers;
}
