package com.ltc.logisticsproject.controller;

import com.ltc.logisticsproject.dto.customs.BorderCrossingView;
import com.ltc.logisticsproject.dto.customs.CustomsDeclarationView;
import com.ltc.logisticsproject.dto.PublicTrackingResponse;
import com.ltc.logisticsproject.dto.customs.TradeDocumentView;
import com.ltc.logisticsproject.dto.TripExpenseView;
import com.ltc.logisticsproject.entity.Cargo;
import com.ltc.logisticsproject.entity.TrackingLog;
import com.ltc.logisticsproject.entity.Trip;
import com.ltc.logisticsproject.repository.BorderCrossingRepository;
import com.ltc.logisticsproject.repository.CargoRepository;
import com.ltc.logisticsproject.repository.CustomsDeclarationRepository;
import com.ltc.logisticsproject.repository.TrackingLogRepository;
import com.ltc.logisticsproject.repository.TradeDocumentRepository;
import com.ltc.logisticsproject.repository.TripExpenseRepository;
import com.ltc.logisticsproject.repository.VehicleRepository;
import com.ltc.logisticsproject.entity.Vehicle;
import com.ltc.logisticsproject.service.RouteEstimationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PublicTrackingController {

    final CargoRepository cargoRepository;
    final TrackingLogRepository trackingLogRepository;
    final TripExpenseRepository tripExpenseRepository;
    final TradeDocumentRepository tradeDocumentRepository;
    final CustomsDeclarationRepository customsDeclarationRepository;
    final BorderCrossingRepository borderCrossingRepository;
    final VehicleRepository vehicleRepository;
    // Stage 6: ETA now goes through the shared simulated-routing service
    // (road-distance factor applied to haversine) instead of raw straight-
    // line distance — see RouteEstimationService for the documented estimate.
    final RouteEstimationService routeEstimationService;

    @GetMapping("/{trackingNumber}")
    public ResponseEntity<PublicTrackingResponse> track(@PathVariable String trackingNumber) {
        Cargo cargo = cargoRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new RuntimeException("Göndəriş tapılmadı"));

        Double lat = null, lng = null;
        String lastUpdated = null;
        String driverName = null, driverPhone = null, driverPhotoUrl = null, vehiclePlate = null;
        String vehicleMainPhotoUrl = null;
        List<String> vehicleDetailPhotoUrls = List.of();
        String tripStartedAt = null;
        String tripDeliveredAt = null;
        String proofOfDeliveryUrl = null;
        Trip trip = cargo.getTrip();

        if (trip != null) {
            List<TrackingLog> logs = trackingLogRepository.findByTripIdOrderByRecordedAtAsc(trip.getId());
            if (!logs.isEmpty()) {
                TrackingLog last = logs.get(logs.size() - 1);
                lat = last.getLatitude();
                lng = last.getLongitude();
                lastUpdated = last.getRecordedAt().toString();
            }
            if (trip.getDriver() != null) {
                driverName = trip.getDriver().getFullName();
                driverPhone = trip.getDriver().getPhone();
                driverPhotoUrl = trip.getDriver().getPhotoUrl();
            }
            if (trip.getVehicle() != null) {
                vehiclePlate = trip.getVehicle().getPlateNumber();
                vehicleMainPhotoUrl = trip.getVehicle().getMainPhotoUrl();
                if (trip.getVehicle().getDetailPhotoUrls() != null) {
                    vehicleDetailPhotoUrls = trip.getVehicle().getDetailPhotoUrls();
                }
            }
            // Fallback: reys yaradılanda dispetçerin seçdiyi "Vehicle" qeydi
            // sürücünün öz profilində şəkil yüklədiyi "Vehicle" qeydindən fərqli
            // ola bilər (iki ayrı DB sətri eyni fiziki maşın üçün — məs.
            // "77my678" vs "77-MY-678"). Əsas/reys üzrə təhkim olunan maşında
            // şəkil yoxdursa, sürücünün öz (driverId ilə bağlı) avtomobilindəki
            // şəkillərə keç ki, müştəri hər halda maşını görə bilsin.
            if (vehicleMainPhotoUrl == null && vehicleDetailPhotoUrls.isEmpty() && trip.getDriver() != null) {
                Vehicle driverOwnVehicle = vehicleRepository.findByDriverId(trip.getDriver().getId()).orElse(null);
                if (driverOwnVehicle != null) {
                    vehicleMainPhotoUrl = driverOwnVehicle.getMainPhotoUrl();
                    if (driverOwnVehicle.getDetailPhotoUrls() != null) {
                        vehicleDetailPhotoUrls = driverOwnVehicle.getDetailPhotoUrls();
                    }
                }
            }
            if (trip.getStartedAt() != null) {
                tripStartedAt = trip.getStartedAt().toString();
            }
            if (trip.getDeliveredAt() != null) {
                tripDeliveredAt = trip.getDeliveredAt().toString();
            }
            proofOfDeliveryUrl = trip.getProofOfDeliveryUrl();
        }

        // Fall back to the pickup point when no GPS ping has arrived yet,
        // so the map/ETA still has a sensible starting position.
        Double fromLat = lat != null ? lat : cargo.getPickupLatitude();
        Double fromLng = lng != null ? lng : cargo.getPickupLongitude();

        Integer etaMinutes = routeEstimationService.estimateEtaMinutes(
                fromLat, fromLng, cargo.getDestinationLatitude(), cargo.getDestinationLongitude());

        // Yol boyu xərclər (fuel/toll/food/...) — reys yaranıbsa müştəriyə
        // göstərmək üçün, şübhəli (anomaly) qeyd olunanlar daxil olmaqla.
        List<TripExpenseView> expenses = List.of();
        if (trip != null) {
            expenses = tripExpenseRepository.findByTripIdOrderByRecordedAtDesc(trip.getId()).stream()
                    .map(exp -> TripExpenseView.builder()
                            .id(exp.getId())
                            .category(exp.getCategory())
                            .amount(exp.getAmount())
                            .description(exp.getDescription())
                            .isAnomaly(exp.getIsAnomaly())
                            .recordedAt(exp.getRecordedAt() != null ? exp.getRecordedAt().toString() : null)
                            .build())
                    .toList();
        }

        // Beynəlxalq göndəriş məlumatları — yalnız Cargo.requiresCustoms=true
        // olduqda mənalıdır, amma sənəd/sərhəd siyahıları hər halda (boş
        // qalsa belə) qaytarılır ki, frontend şərtsiz oxuya bilsin.
        List<TradeDocumentView> documents = tradeDocumentRepository.findByCargoIdOrderByCreatedAtDesc(cargo.getId())
                .stream().map(TradeDocumentView::from).toList();
        CustomsDeclarationView declarationView = customsDeclarationRepository.findByCargoId(cargo.getId())
                .map(CustomsDeclarationView::from).orElse(null);
        List<BorderCrossingView> borderCrossings = trip != null
                ? borderCrossingRepository.findByTripIdOrderByCrossedAtAsc(trip.getId()).stream().map(BorderCrossingView::from).toList()
                : List.of();

        return ResponseEntity.ok(PublicTrackingResponse.builder()
                .trackingNumber(cargo.getTrackingNumber())
                .status(cargo.getStatus())
                .description(cargo.getDescription())
                .orderCreatedAt(cargo.getCreatedAt() != null ? cargo.getCreatedAt().toString() : null)
                .tripDeliveredAt(tripDeliveredAt)
                .pickupAddress(cargo.getPickupAddress())
                .pickupLatitude(cargo.getPickupLatitude())
                .pickupLongitude(cargo.getPickupLongitude())
                .destinationAddress(cargo.getDestinationAddress())
                .destinationLatitude(cargo.getDestinationLatitude())
                .destinationLongitude(cargo.getDestinationLongitude())
                .lastLatitude(lat)
                .lastLongitude(lng)
                .lastUpdatedAt(lastUpdated)
                .driverName(driverName)
                .driverPhone(driverPhone)
                .driverPhotoUrl(driverPhotoUrl)
                .vehiclePlate(vehiclePlate)
                .vehicleMainPhotoUrl(vehicleMainPhotoUrl)
                .vehicleDetailPhotoUrls(vehicleDetailPhotoUrls)
                .estimatedEtaMinutes(etaMinutes)
                .tripStartedAt(tripStartedAt)
                .proofOfDeliveryUrl(proofOfDeliveryUrl)
                .expenses(expenses)
                .requiresCustoms(Boolean.TRUE.equals(cargo.getRequiresCustoms()))
                .preferredTransportMode(cargo.getPreferredTransportMode())
                .incoterm(cargo.getIncoterm())
                .originCountry(cargo.getOriginCountry())
                .destinationCountry(cargo.getDestinationCountry())
                .transitCountries(cargo.getTransitCountries())
                .documents(documents)
                .customsDeclaration(declarationView)
                .borderCrossings(borderCrossings)
                .build());
    }
}
