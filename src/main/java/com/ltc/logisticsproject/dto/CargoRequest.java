package com.ltc.logisticsproject.dto;

import com.ltc.logisticsproject.entity.CargoType;
import com.ltc.logisticsproject.entity.Incoterm;
import com.ltc.logisticsproject.entity.TransportMode;
import com.ltc.logisticsproject.entity.UrgencyLevel;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CargoRequest {
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

    // Beynəlxalq göndəriş sahələri — müştəri "Beynəlxalq göndərişdir"
    // seçəndə frontend bunları doldurur, daxili sifarişlərdə boş qalır.
    boolean requiresCustoms;
    TransportMode preferredTransportMode;
    Incoterm incoterm;
    String originCountry;
    String destinationCountry;
    String transitCountries;
}