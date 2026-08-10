package com.ltc.logisticsproject.service;

import com.ltc.logisticsproject.dto.CostEstimateResponse;
import com.ltc.logisticsproject.entity.Cargo;
import com.ltc.logisticsproject.entity.CargoType;
import com.ltc.logisticsproject.entity.Trailer;
import com.ltc.logisticsproject.entity.UrgencyLevel;
import com.ltc.logisticsproject.entity.Vehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

// Real yük daşıma şirkətlərinin qiymət təklifini necə qurduğunun sadələşdirilmiş
// modeli — real fuel/toll qiymət API-si qoşulu olmadığı üçün sabitlər
// (yanacaq qiyməti, sürücü saatlıq tarifi, servis norması) Azərbaycan
// bazarına uyğun təxmini dəyərlərdir, amma DÜSTUR real təcrübədəki kimidir:
//
//   ümumi xərc = (yanacaq + sürücü əməkhaqqı + texniki xidmət/amortizasiya + baza xərc)
//                × (1 + yük növü əlavəsi) × (1 + təcililik əlavəsi)
//
// — yanacaq: məsafə × (L/100km) × yanacaq qiyməti, ağır yükdə +15%-ə qədər əlavə;
// — sürücü: səyahət müddəti (məsafə/orta sürət) × saatlıq tarif;
// — texniki xidmət: məsafə × km-başı norma (təkər/servis/amortizasiya);
// — baza xərc: yükləmə/boşaltma + sənədləşmə (məsafədən asılı olmayan sabit);
// — yük növü əlavəsi: FRAGILE/REFRIGERATED/HAZARDOUS xüsusi baxım/icazə tələb edir;
// — təcililik əlavəsi: EXPRESS üçün prioritet planlaşdırma/əlavə iş vaxtı.
//
// Dispetçerə bu, "Reys yarat" formasında Təxmini məsafə/xərc xanalarını
// avtomatik dolduran başlanğıc TƏKLİF kimi göstərilir — məcburi qiymət
// deyil, istənilən vaxt əl ilə dəyişdirilə bilər (bax CargoQueue.jsx).
@Service
@RequiredArgsConstructor
public class TripCostEstimationService {

    private final RouteEstimationService routeEstimationService;

    private static final double FUEL_PRICE_PER_LITER = 1.60; // AZN/L, dizel — təxmini bazar qiyməti
    private static final double DEFAULT_FUEL_CONSUMPTION_L_PER_100KM = 30.0; // Vehicle.fuelConsumption boşdursa
    private static final double DRIVER_RATE_PER_HOUR = 8.0; // AZN/saat
    private static final double AVG_SPEED_KMH = 45.0; // RouteEstimationService ilə eyni fərziyyə
    private static final double MAINTENANCE_RATE_PER_KM = 0.30; // AZN/km
    private static final double BASE_FEE = 40.0; // AZN — yükləmə/boşaltma + sənədləşmə
    private static final double MAX_LOAD_FUEL_SURCHARGE = 0.15; // tam yüklü tırda yanacaq sərfinin +15%-ə qədər artması

    public CostEstimateResponse estimate(List<Cargo> cargos, Vehicle vehicle, Trailer trailer) {
        if (cargos == null || cargos.isEmpty()) {
            return CostEstimateResponse.builder()
                    .distanceKm(0.0).fuelCost(0.0).driverCost(0.0).maintenanceCost(0.0)
                    .baseFee(BASE_FEE).handlingSurchargePercent(0.0).urgencySurchargePercent(0.0)
                    .totalCost(BASE_FEE)
                    .build();
        }

        // Təmsilçi marşrut kimi ilk seçilmiş yükün alış->təhvil məsafəsi
        // götürülür — reysə birləşdirilən digər yüklər adətən eyni ümumi
        // istiqamətdə olur (LiveTripResponse/TripBroadcastService-də də eyni
        // "ilk yük təmsil edir" yanaşması istifadə olunur).
        Cargo primary = cargos.get(0);
        double distanceKm = 0.0;
        if (primary.getPickupLatitude() != null && primary.getPickupLongitude() != null
                && primary.getDestinationLatitude() != null && primary.getDestinationLongitude() != null) {
            distanceKm = routeEstimationService.estimateRoadDistanceKm(
                    primary.getPickupLatitude(), primary.getPickupLongitude(),
                    primary.getDestinationLatitude(), primary.getDestinationLongitude());
        }

        double totalWeightTons = cargos.stream()
                .mapToDouble(c -> c.getWeight() != null ? c.getWeight() : 0.0)
                .sum() / 1000.0;

        double fuelConsumption = (vehicle != null && vehicle.getFuelConsumption() != null && vehicle.getFuelConsumption() > 0)
                ? vehicle.getFuelConsumption()
                : DEFAULT_FUEL_CONSUMPTION_L_PER_100KM;

        // Ağır yük daha çox yanacaq yandırır — QOŞQUNUN tutumuna nisbətdə
        // əlavə (Vehicle-in özündə capacity sahəsi yoxdur, çünki tır yalnız
        // kəllə hissəsidir və yükü özü daşımır — bax Vehicle.java izahı).
        // Qoşqu seçilməyibsə (CargoQueue.jsx çəkisi olan yükdə bunu artıq
        // məcburi edir) bu amil sadəcə təsirsiz qalır (loadFactor = 1.0).
        double loadFactor = 1.0;
        if (trailer != null && trailer.getCapacity() != null && trailer.getCapacity() > 0) {
            double loadRatio = Math.min(1.0, totalWeightTons / trailer.getCapacity());
            loadFactor = 1.0 + loadRatio * MAX_LOAD_FUEL_SURCHARGE;
        }

        double fuelLiters = (distanceKm / 100.0) * fuelConsumption * loadFactor;
        double fuelCost = fuelLiters * FUEL_PRICE_PER_LITER;

        double travelHours = distanceKm / AVG_SPEED_KMH;
        double driverCost = travelHours * DRIVER_RATE_PER_HOUR;

        double maintenanceCost = distanceKm * MAINTENANCE_RATE_PER_KM;

        // Bir reysdə bir neçə yük ola bilər — ən "ağır" tələb (təhlükəli >
        // soyuducu > kövrək > adi) bütün reysi müəyyən edir, çünki sürücü
        // hamısını eyni anda daşıyır.
        double handlingPct = cargos.stream()
                .mapToDouble(c -> handlingSurchargePercent(c.getCargoType()))
                .max().orElse(0.0);
        boolean anyExpress = cargos.stream().anyMatch(c -> c.getUrgency() == UrgencyLevel.EXPRESS);
        double urgencyPct = anyExpress ? 20.0 : 0.0;

        double subtotal = fuelCost + driverCost + maintenanceCost + BASE_FEE;
        double afterHandling = subtotal * (1 + handlingPct / 100.0);
        double total = afterHandling * (1 + urgencyPct / 100.0);

        return CostEstimateResponse.builder()
                .distanceKm(round2(distanceKm))
                .fuelCost(round2(fuelCost))
                .driverCost(round2(driverCost))
                .maintenanceCost(round2(maintenanceCost))
                .baseFee(round2(BASE_FEE))
                .handlingSurchargePercent(handlingPct)
                .urgencySurchargePercent(urgencyPct)
                .totalCost(round2(total))
                .build();
    }

    private double handlingSurchargePercent(CargoType type) {
        if (type == null) return 0.0;
        return switch (type) {
            case GENERAL -> 0.0;
            case FRAGILE -> 8.0;
            case REFRIGERATED -> 18.0;
            case HAZARDOUS -> 25.0;
        };
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
