package com.ltc.logisticsproject.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Sürücü panelində "Qazancım" statistika kartı üçün (bax
// DriverTripService#getEarningsSummary). Rəqəmlər sürücüyə faktiki ödəniş
// deyil — dispetçerin reys yaradanda təyin etdiyi "Təxmini xərc"
// (Trip.estimatedCost, müştəriyə göstərilən qiymət) əsasında hesablanır,
// çünki sistemdə ayrıca sürücü-komissiya modeli yoxdur. Ona görə frontend
// bunu "təxmini gəlir" kimi göstərir, dəqiq maaş kimi yox.
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DriverEarningsSummary {
    int tripsThisMonth;
    double earningsThisMonth;
    int tripsTotal;
    double earningsTotal;
}
