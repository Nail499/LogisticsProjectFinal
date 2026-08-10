package com.ltc.logisticsproject.dto;

import com.ltc.logisticsproject.entity.CargoType;
import com.ltc.logisticsproject.entity.Incoterm;
import com.ltc.logisticsproject.entity.TransportMode;
import com.ltc.logisticsproject.entity.UrgencyLevel;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

// Dispatcher-created order (e.g. a phone-in request that never went through
// the customer self-service form). No linked Customer account is required —
// unlike CargoRequest, this carries the customer's name/phone as plain text
// the same way Cargo.customerName/customerPhone already store it for
// registered customers, so downstream screens (CargoQueue, tracking, etc.)
// don't need to special-case dispatcher-entered orders.
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DispatcherCargoRequest {
    String customerName;
    String customerPhone;
    String description;
    Double weight;
    Double volume;
    Long originWarehouseId;
    String pickupAddress;
    Double pickupLatitude;
    Double pickupLongitude;
    String destinationAddress;
    Double destinationLatitude;
    Double destinationLongitude;
    CargoType cargoType;
    UrgencyLevel urgency;
    LocalDate requestedPickupDate;

    boolean requiresCustoms;
    TransportMode preferredTransportMode;
    Incoterm incoterm;
    String originCountry;
    String destinationCountry;
    String transitCountries;
}
