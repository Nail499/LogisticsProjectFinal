package com.ltc.logisticsproject.service;

import com.ltc.logisticsproject.dto.DriverSuggestionResponse;
import com.ltc.logisticsproject.entity.*;
import com.ltc.logisticsproject.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// Dispetçer "Reys yarat" formasında yük(lər)i seçəndən sonra, sürücünü əl ilə
// axtarmaq əvəzinə sistem "ən uyğun sürücü" sırasını təklif edir — real TMS
// platformalarında (Motive/Samsara) 2026-cı il standartıdır: yük ölçüsü,
// nəqliyyat vasitəsi növü və sürücü performansına görə sıralanmış təklif.
// Burada 4 amil çəkilənir: HOS (iş saatı) vəziyyəti, DVIR defekti, sürücünün
// hazırkı mövqeyindən yükün götürülmə nöqtəsinə məsafə, və (əgər sürücü öz
// qoşqusu ilə işləyirsə) o qoşqunun tutumu yükü daşıya bilirmi. Sırf TƏKLİFDİR
// — backend heç bir yeri məcburi etmir, dispetçer istənilən sürücünü seçə bilər
// (bax DispatcherController#createTrip-də enforcement yoxdur).
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DriverSuggestionService {

    private static final List<TripStatus> ACTIVE_TRIP_STATUSES =
            List.of(TripStatus.PLANNED, TripStatus.PICKED_UP, TripStatus.IN_TRANSIT);

    // Bal düsturunun çəkiləri — təhlükəsizlik (HOS+DVIR=%50) səmərəlilikdən
    // (məsafə+tutum=%40) və reytinqdən (%10) üstün tutulur, sənayenin
    // "compliance-first dispatch" prinsipi ilə uyğun (bax boşluq analizi
    // söhbətindəki mənbələr).
    private static final double W_HOS = 0.30;
    private static final double W_DVIR = 0.20;
    private static final double W_DISTANCE = 0.25;
    private static final double W_CAPACITY = 0.15;
    private static final double W_RATING = 0.10;

    final DriverRepository driverRepository;
    final CargoRepository cargoRepository;
    final TripRepository tripRepository;
    final TrackingLogRepository trackingLogRepository;
    final TrailerRepository trailerRepository;
    final DvirInspectionRepository dvirInspectionRepository;
    final HosService hosService;
    final RatingService ratingService;
    final RouteEstimationService routeEstimationService;

    public List<DriverSuggestionResponse> suggest(List<Long> cargoIds) {
        List<Cargo> cargos = cargoIds == null || cargoIds.isEmpty()
                ? List.of() : cargoRepository.findAllById(cargoIds);

        double totalWeightKg = cargos.stream()
                .mapToDouble(c -> c.getWeight() != null ? c.getWeight() : 0.0)
                .sum();

        // Təmsilçi marşrut kimi ilk yükün götürülmə nöqtəsi (bax
        // TripCostEstimationService-dəki eyni "ilk yük təmsil edir" naxışı).
        Cargo primary = cargos.isEmpty() ? null : cargos.get(0);
        Double pickupLat = primary != null ? primary.getPickupLatitude() : null;
        Double pickupLng = primary != null ? primary.getPickupLongitude() : null;

        List<Driver> drivers = driverRepository.findByStatus(DriverStatus.ACTIVE);

        return drivers.stream()
                .map(d -> buildSuggestion(d, pickupLat, pickupLng, totalWeightKg))
                .sorted(Comparator.comparingDouble(DriverSuggestionResponse::getScore).reversed())
                .toList();
    }

    private DriverSuggestionResponse buildSuggestion(Driver d, Double pickupLat, Double pickupLng, double totalWeightKg) {
        HosService.DriverHosSnapshot hos = hosService.getDriverSnapshot(d.getId());
        boolean hasDefect = !dvirInspectionRepository
                .findByTrip_Driver_IdAndHasDefectsTrueAndResolvedFalse(d.getId()).isEmpty();
        RatingService.RatingSummary rating = ratingService.getDriverSummary(d.getId());

        Double distanceKm = computeDistanceKm(d, pickupLat, pickupLng);
        Boolean capacitySufficient = computeCapacitySufficient(d, totalWeightKg);

        double score = computeScore(hos, hasDefect, rating, distanceKm, capacitySufficient);

        return DriverSuggestionResponse.builder()
                .id(d.getId())
                .fullName(d.getFullName())
                .phone(d.getPhone())
                .hasActiveTrip(hos.hasActiveTrip())
                .hosStatus(hos.hosStatus())
                .remainingDrivingHours(hos.remainingDrivingHours())
                .fatigueWarning(hos.fatigueWarning())
                .hasUnresolvedDvirDefect(hasDefect)
                .ratingAverage(rating.average())
                .ratingCount(rating.count())
                .distanceKm(distanceKm != null ? Math.round(distanceKm * 10.0) / 10.0 : null)
                .capacitySufficient(capacitySufficient)
                .score(Math.round(score * 10.0) / 10.0)
                .build();
    }

    // Sürücünün HAZIRKI aktiv reysinin son GPS ping-indən yükün götürülmə
    // nöqtəsinə qədər (bax DispatcherController#trips/live-dəki eyni "son
    // ping" naxışı). Aktiv reysi/ping-i yoxdursa (ən çox rast gəlinən hal —
    // sürücü bazadadır) məsafə bilinmir, null qaytarılır.
    private Double computeDistanceKm(Driver d, Double pickupLat, Double pickupLng) {
        if (pickupLat == null || pickupLng == null) return null;
        List<Trip> activeTrips = tripRepository.findByDriverIdAndStatusIn(d.getId(), ACTIVE_TRIP_STATUSES);
        if (activeTrips.isEmpty()) return null;
        List<TrackingLog> logs = trackingLogRepository.findByTripIdOrderByRecordedAtAsc(activeTrips.get(0).getId());
        if (logs.isEmpty()) return null;
        TrackingLog last = logs.get(logs.size() - 1);
        return routeEstimationService.estimateRoadDistanceKm(last.getLatitude(), last.getLongitude(), pickupLat, pickupLng);
    }

    // Sürücünün ÖZ qoşqusu varsa (DRIVER_OWNED), onun tutumu yükü daşıya
    // bilirmi. Öz qoşqusu yoxdursa (şirkət avadanlığı ilə işləyəcək) null —
    // "tətbiq olunmur", real yoxlama reys yaradanda (bax CargoQueue.jsx
    // effectiveCapacityTons) aparılacaq.
    private Boolean computeCapacitySufficient(Driver d, double totalWeightKg) {
        Optional<Trailer> ownTrailer = trailerRepository.findByDriverId(d.getId());
        if (ownTrailer.isEmpty()) return null;
        Double capacityTons = ownTrailer.get().getCapacity();
        if (capacityTons == null) return null;
        return capacityTons * 1000 >= totalWeightKg;
    }

    private double computeScore(HosService.DriverHosSnapshot hos, boolean hasDefect,
                                 RatingService.RatingSummary rating, Double distanceKm, Boolean capacitySufficient) {
        double hosScore = Math.min(100.0, (hos.remainingDrivingHours() / HosService.getMaxDailyDrivingHours()) * 100.0);
        if (hos.fatigueWarning()) hosScore = Math.max(0.0, hosScore - 30.0);

        double dvirScore = hasDefect ? 0.0 : 100.0;

        // Reytinqi hələ olmayan (yeni) sürücü cəzalandırılmasın deyə neytral
        // (ortadan bir az aşağı) bal — "sübut olunmuş" sürücülər bərabər
        // şəraitdə üstünlük alsın, amma yeni sürücü siyahının dibinə düşməsin.
        double ratingScore = (rating.count() > 0) ? (rating.average() / 5.0) * 100.0 : 60.0;

        // Xətti azalma: hər 1 km məsafə 1 xal endirir (100km+ => 0). Məsafə
        // bilinmirsə (sürücü bazada, aktiv reysi yoxdur) neytral bal.
        double distanceScore = distanceKm == null ? 50.0 : Math.max(0.0, 100.0 - distanceKm);

        double capacityScore = capacitySufficient == null ? 100.0 : (capacitySufficient ? 100.0 : 0.0);

        return W_HOS * hosScore + W_DVIR * dvirScore + W_DISTANCE * distanceScore
                + W_CAPACITY * capacityScore + W_RATING * ratingScore;
    }
}
