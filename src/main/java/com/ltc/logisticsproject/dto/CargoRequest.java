package com.ltc.logisticsproject.dto;

import com.ltc.logisticsproject.entity.CargoType;
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
    String destinationAddress;
    Double destinationLatitude;
    Double destinationLongitude;
    CargoType cargoType;
    UrgencyLevel urgency;
    LocalDate requestedPickupDate;
}