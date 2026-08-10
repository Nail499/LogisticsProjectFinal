package com.ltc.logisticsproject.dto;

import com.ltc.logisticsproject.entity.OwnerType;
import com.ltc.logisticsproject.entity.TransportMode;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VehicleRequest {
    String plateNumber;
    String brand;
    Double fuelConsumption;
    TransportMode transportMode;
    // Boş buraxılsa Vehicle.prePersist COMPANY təyin edir.
    OwnerType ownerType;
    // Yalnız ownerType=DRIVER_OWNED olduqda tələb olunur.
    Long driverId;
}