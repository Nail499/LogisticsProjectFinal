package com.ltc.logisticsproject.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RouteStop {
    Long cargoId;
    String label;
    String address;
    Double lat;
    Double lng;
}
