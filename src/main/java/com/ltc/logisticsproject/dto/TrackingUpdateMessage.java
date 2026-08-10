package com.ltc.logisticsproject.dto;

import com.ltc.logisticsproject.entity.CargoStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

// Stage 6 — lightweight payload pushed over /topic/tracking/{trackingNumber}.
// Deliberately small: just the fields that actually change while a trip is
// moving. The customer tracking UI merges this into the PublicTrackingResponse
// it already fetched over REST, instead of refetching the whole thing.
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TrackingUpdateMessage {
    String trackingNumber;
    CargoStatus status;
    Double lastLatitude;
    Double lastLongitude;
    String lastUpdatedAt;
    Integer estimatedEtaMinutes;
}
