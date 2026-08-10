package com.ltc.logisticsproject.dto;

import com.ltc.logisticsproject.entity.OwnerType;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TrailerRequest {
    String plateNumber;
    Double capacity;
    // Boş buraxılsa Trailer.prePersist COMPANY təyin edir.
    OwnerType ownerType;
    // Yalnız ownerType=DRIVER_OWNED olduqda tələb olunur.
    Long driverId;
}
