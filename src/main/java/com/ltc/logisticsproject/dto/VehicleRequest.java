package com.ltc.logisticsproject.dto;

import com.ltc.logisticsproject.entity.TransportMode;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VehicleRequest {
    String plateNumber;
    String brand;
    Double capacity;
    Double fuelConsumption;
    TransportMode transportMode;
}