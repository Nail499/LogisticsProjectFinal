package com.ltc.logisticsproject.dto.customs;

import com.ltc.logisticsproject.entity.CargoType;
import lombok.*;
import lombok.experimental.FieldDefaults;

// Müştəri gömrük kalkulyatoru üçün — hələ sifariş yaradılmadan təxmini
// rüsum/ƏDV görmək istəyəndə göndərilir (bax CustomerCargoController).
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomsEstimateRequest {
    CargoType cargoType;
    Double declaredValue;
}
