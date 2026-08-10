package com.ltc.logisticsproject.dto.customs;

import com.ltc.logisticsproject.entity.CargoType;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomsTariffRequest {
    CargoType cargoType;
    Double dutyRatePercent;
    Double vatRatePercent;
    String description;
}
