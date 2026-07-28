package com.ltc.logisticsproject.controller;

import com.ltc.logisticsproject.dto.BorderCrossingRequest;
import com.ltc.logisticsproject.dto.ExpenseRequest;
import com.ltc.logisticsproject.dto.FatigueAlertRequest;
import com.ltc.logisticsproject.dto.LiveTripResponse;
import com.ltc.logisticsproject.dto.LocationRequest;
import com.ltc.logisticsproject.dto.TripStatusUpdateRequest;
import com.ltc.logisticsproject.entity.*;
import com.ltc.logisticsproject.repository.*;
import com.ltc.logisticsproject.service.DriverTripService;
import com.ltc.logisticsproject.service.ExpenseService;
import com.ltc.logisticsproject.service.FileStorageService;
import com.ltc.logisticsproject.service.TripBroadcastService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/driver")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DriverController {

    final TripRepository tripRepository;
    final UserRepository userRepository;
    final DriverTripService driverTripService;
    final TrackingLogRepository trackingLogRepository;
    final ExpenseService expenseService;
    final FatigueAlertRepository fatigueAlertRepository;
    final TripBroadcastService tripBroadcastService;
    final FileStorageService fileStorageService;
    final BorderCrossingRepository borderCrossingRepository;
    final VehicleRepository vehicleRepository;

    // "Əsas 1 şəkil + ətraflı 3-4 şəkil" — professional vehicle-gallery UX
    // (tək əsas şəkil sürətli tanınma üçün, məhdud sayda detal şəkli isə
    // qalereyanı nizamlı saxlayır, sürücü onlarla şəkil yükləyib
    // "istifadəçi görsün" məqsədini itirmir).
    private static final int MAX_DETAIL_PHOTOS = 4;

    @GetMapping("/trips/current")
    public ResponseEntity<List<Trip>> currentTrips(Authentication authentication) {
        Long driverId = currentDriverId(authentication);
        return ResponseEntity.ok(tripRepository.findByDriverIdAndStatusNot(driverId, TripStatus.DELIVERED));
    }

    @GetMapping("/trips/history")
    public ResponseEntity<List<Trip>> tripHistory(Authentication authentication) {
        Long driverId = currentDriverId(authentication);
        return ResponseEntity.ok(tripRepository.findByDriverIdAndStatus(driverId, TripStatus.DELIVERED));
    }

    // Stage 5 — Driver PWA: the plain /trips/current above returns a bare
    // Trip (Cargo is @JsonIgnore-d off it), so the mobile app has no
    // destination address/coordinates to build a Waze/Google Maps deep
    // link with. This reuses the same enrichment shape the Dispatcher
    // Control Tower uses (see DispatcherController#liveTrips) scoped to
    // just this driver's active trips.
    @GetMapping("/trips/current/live")
    public ResponseEntity<List<LiveTripResponse>> currentTripsLive(Authentication authentication) {
        Long driverId = currentDriverId(authentication);
        List<Trip> trips = tripRepository.findByDriverIdAndStatusNot(driverId, TripStatus.DELIVERED);

        List<LiveTripResponse> result = trips.stream().map(trip -> {
            Double lat = null, lng = null;
            String lastUpdated = null;
            List<TrackingLog> logs = trackingLogRepository.findByTripIdOrderByRecordedAtAsc(trip.getId());
            if (!logs.isEmpty()) {
                TrackingLog last = logs.get(logs.size() - 1);
                lat = last.getLatitude();
                lng = last.getLongitude();
                lastUpdated = last.getRecordedAt().toString();
            }

            String destinationAddress = null;
            Double destLat = null, destLng = null;
            if (trip.getCargos() != null && !trip.getCargos().isEmpty()) {
                Cargo firstCargo = trip.getCargos().get(0);
                destinationAddress = firstCargo.getDestinationAddress();
                destLat = firstCargo.getDestinationLatitude();
                destLng = firstCargo.getDestinationLongitude();
                if (lat == null) {
                    lat = firstCargo.getPickupLatitude();
                    lng = firstCargo.getPickupLongitude();
                }
            }

            return LiveTripResponse.builder()
                    .tripId(trip.getId())
                    .status(trip.getStatus())
                    .driverName(trip.getDriver() != null ? trip.getDriver().getFullName() : null)
                    .vehiclePlate(trip.getVehicle() != null ? trip.getVehicle().getPlateNumber() : null)
                    .lastLatitude(lat)
                    .lastLongitude(lng)
                    .lastUpdatedAt(lastUpdated)
                    .destinationAddress(destinationAddress)
                    .destinationLatitude(destLat)
                    .destinationLongitude(destLng)
                    .vehicleCapacity(trip.getVehicle() != null ? trip.getVehicle().getCapacity() : null)
                    .routeInfo(trip.getRouteInfo())
                    .estimatedDistanceKm(trip.getEstimatedDistanceKm())
                    .estimatedCost(trip.getEstimatedCost())
                    .build();
        }).toList();

        return ResponseEntity.ok(result);
    }

    @PostMapping("/trips/{id}/status")
    public ResponseEntity<Trip> updateStatus(@PathVariable Long id,
                                             @RequestBody TripStatusUpdateRequest request,
                                             Authentication authentication) {
        Long driverId = currentDriverId(authentication);
        Trip updated = driverTripService.updateStatus(id, driverId, request.getStatus());
        // Stage 6: push the status change immediately (Control Tower badge +
        // customer tracking timeline) instead of waiting for the next poll.
        tripBroadcastService.broadcastTripUpdate(id);
        return ResponseEntity.ok(updated);
    }

    private Long currentDriverId(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("İstifadəçi tapılmadı"));
        if (user.getDriverId() == null) {
            throw new RuntimeException("Bu istifadəçi sürücü deyil");
        }
        return user.getDriverId();
    }

    @PostMapping("/trips/{id}/tracking")
    public ResponseEntity<TrackingLog> sendLocation(@PathVariable Long id,
                                                    @RequestBody LocationRequest request,
                                                    Authentication authentication) {
        Long driverId = currentDriverId(authentication);
        Trip trip = tripRepository.findById(id).orElseThrow(() -> new RuntimeException("Reys tapılmadı"));
        if (!trip.getDriver().getId().equals(driverId)) {
            throw new RuntimeException("Bu reys sizə aid deyil");
        }

        TrackingLog log = TrackingLog.builder()
                .trip(trip)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();

        TrackingLog saved = trackingLogRepository.save(log);
        // Stage 6: push the new GPS position live to the Control Tower map
        // and any open customer tracking pages for this trip's cargo.
        tripBroadcastService.broadcastTripUpdate(id);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/trips/{id}/expenses")
    public ResponseEntity<TripExpense> addExpense(@PathVariable Long id,
                                                  @RequestBody ExpenseRequest request,
                                                  Authentication authentication) {
        Long driverId = currentDriverId(authentication);
        Trip trip = tripRepository.findById(id).orElseThrow(() -> new RuntimeException("Reys tapılmadı"));
        if (!trip.getDriver().getId().equals(driverId)) {
            throw new RuntimeException("Bu reys sizə aid deyil");
        }

        return ResponseEntity.ok(expenseService.addExpense(id, request.getCategory(), request.getAmount(), request.getDescription()));
    }

    // Stage 8 — mobile driver app: attach a delivery-proof photo, taken with
    // the phone camera, to a trip (typically right before/at marking it
    // DELIVERED).
    @PostMapping(value = "/trips/{id}/proof", consumes = "multipart/form-data")
    public ResponseEntity<Trip> uploadProof(@PathVariable Long id,
                                            @RequestParam("photo") MultipartFile photo,
                                            Authentication authentication) {
        Long driverId = currentDriverId(authentication);
        Trip trip = tripRepository.findById(id).orElseThrow(() -> new RuntimeException("Reys tapılmadı"));
        if (!trip.getDriver().getId().equals(driverId)) {
            throw new RuntimeException("Bu reys sizə aid deyil");
        }

        String url = fileStorageService.store(photo);
        trip.setProofOfDeliveryUrl(url);
        Trip saved = tripRepository.save(trip);
        tripBroadcastService.broadcastTripUpdate(id);
        return ResponseEntity.ok(saved);
    }

    // Stage 5 — Driver PWA "Rest Mode": the driver's app tracks continuous
    // driving time client-side and calls this once it crosses the 4.5h
    // threshold, so the Dispatcher Control Tower can surface a real alert
    // (see DispatcherController#fatigueAlerts) instead of a purely local
    // notification only the driver would ever see.
    @PostMapping("/trips/{id}/fatigue-alert")
    public ResponseEntity<FatigueAlert> raiseFatigueAlert(@PathVariable Long id,
                                                           @RequestBody FatigueAlertRequest request,
                                                           Authentication authentication) {
        Long driverId = currentDriverId(authentication);
        Trip trip = tripRepository.findById(id).orElseThrow(() -> new RuntimeException("Reys tapılmadı"));
        if (!trip.getDriver().getId().equals(driverId)) {
            throw new RuntimeException("Bu reys sizə aid deyil");
        }

        FatigueAlert alert = FatigueAlert.builder()
                .trip(trip)
                .driverName(trip.getDriver() != null ? trip.getDriver().getFullName() : null)
                .vehiclePlate(trip.getVehicle() != null ? trip.getVehicle().getPlateNumber() : null)
                .continuousDrivingHours(request.getContinuousDrivingHours())
                .resolved(false)
                .build();

        return ResponseEntity.ok(fatigueAlertRepository.save(alert));
    }

    // Tranzit zamanı sürücü özü sərhəd/gömrük məntəqəsindən keçdiyini qeyd
    // edə bilsin — dispetçerin bunu əl ilə daxil etməsini gözləməyə ehtiyac
    // qalmır (bax DispatcherController-də eyni əməliyyatın dispetçer
    // versiyası).
    @GetMapping("/trips/{id}/border-crossings")
    public ResponseEntity<List<BorderCrossing>> borderCrossings(@PathVariable Long id, Authentication authentication) {
        Long driverId = currentDriverId(authentication);
        Trip trip = tripRepository.findById(id).orElseThrow(() -> new RuntimeException("Reys tapılmadı"));
        if (!trip.getDriver().getId().equals(driverId)) {
            throw new RuntimeException("Bu reys sizə aid deyil");
        }
        return ResponseEntity.ok(borderCrossingRepository.findByTripIdOrderByCrossedAtAsc(id));
    }

    @PostMapping("/trips/{id}/border-crossings")
    public ResponseEntity<BorderCrossing> addBorderCrossing(@PathVariable Long id,
                                                             @RequestBody BorderCrossingRequest request,
                                                             Authentication authentication) {
        Long driverId = currentDriverId(authentication);
        Trip trip = tripRepository.findById(id).orElseThrow(() -> new RuntimeException("Reys tapılmadı"));
        if (!trip.getDriver().getId().equals(driverId)) {
            throw new RuntimeException("Bu reys sizə aid deyil");
        }

        BorderCrossing crossing = BorderCrossing.builder()
                .trip(trip)
                .borderPointName(request.getBorderPointName())
                .country(request.getCountry())
                .customsStatus(request.getCustomsStatus() != null ? request.getCustomsStatus() : BorderCustomsStatus.PENDING)
                .recordedBy(trip.getDriver().getFullName())
                .notes(request.getNotes())
                .build();
        BorderCrossing saved = borderCrossingRepository.save(crossing);
        tripBroadcastService.broadcastTripUpdate(id);
        return ResponseEntity.ok(saved);
    }

    // Sürücünün öz nəqliyyat vasitəsinin şəkilləri — 1 əsas + ən çoxu
    // MAX_DETAIL_PHOTOS ətraflı şəkil. Müştəri bunları PublicTrackingResponse
    // vasitəsilə tracking səhifəsində görür (bax PublicTrackingController).
    @GetMapping("/vehicle")
    public ResponseEntity<Vehicle> myVehicle(Authentication authentication) {
        return ResponseEntity.ok(currentVehicle(authentication));
    }

    @PostMapping(value = "/vehicle/main-photo", consumes = "multipart/form-data")
    public ResponseEntity<Vehicle> uploadVehicleMainPhoto(@RequestParam("photo") MultipartFile photo, Authentication authentication) {
        Vehicle vehicle = currentVehicle(authentication);
        vehicle.setMainPhotoUrl(fileStorageService.store(photo));
        return ResponseEntity.ok(vehicleRepository.save(vehicle));
    }

    @PostMapping(value = "/vehicle/detail-photos", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadVehicleDetailPhoto(@RequestParam("photo") MultipartFile photo, Authentication authentication) {
        Vehicle vehicle = currentVehicle(authentication);
        List<String> photos = vehicle.getDetailPhotoUrls();
        if (photos == null) {
            photos = new ArrayList<>();
            vehicle.setDetailPhotoUrls(photos);
        }
        if (photos.size() >= MAX_DETAIL_PHOTOS) {
            return ResponseEntity.status(400).body(Map.of("message", "Ən çoxu " + MAX_DETAIL_PHOTOS + " ətraflı şəkil əlavə edə bilərsiniz"));
        }
        photos.add(fileStorageService.store(photo));
        return ResponseEntity.ok(vehicleRepository.save(vehicle));
    }

    @DeleteMapping("/vehicle/detail-photos")
    public ResponseEntity<Vehicle> deleteVehicleDetailPhoto(@RequestParam("url") String url, Authentication authentication) {
        Vehicle vehicle = currentVehicle(authentication);
        if (vehicle.getDetailPhotoUrls() != null) {
            vehicle.getDetailPhotoUrls().remove(url);
        }
        return ResponseEntity.ok(vehicleRepository.save(vehicle));
    }

    private Vehicle currentVehicle(Authentication authentication) {
        Long driverId = currentDriverId(authentication);
        return vehicleRepository.findByDriverId(driverId)
                .orElseThrow(() -> new RuntimeException("Nəqliyyat vasitəsi tapılmadı"));
    }
}
