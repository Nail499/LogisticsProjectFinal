package com.ltc.logisticsproject.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

// Reys yaradanda dispetçerə göstərilən "sistem təklifi" — real yük daşıma
// təcrübəsindəki kimi bir neçə komponentin cəmi (bax TripCostEstimationService
// üçün fərziyyələrin sənədləşdirilməsi). Dispetçer "Təxmini xərc" xanasında
// bu rəqəmi istənilən vaxt əl ilə dəyişə bilər — bu, sadəcə başlanğıc
// təklifdir, məcburi qiymət deyil.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CostEstimateResponse {
    Double distanceKm;
    Double fuelCost;
    Double driverCost;
    Double maintenanceCost;
    Double baseFee;
    Double handlingSurchargePercent;
    Double urgencySurchargePercent;
    Double totalCost;
}
