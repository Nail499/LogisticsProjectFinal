package com.ltc.logisticsproject.controller;

import com.ltc.logisticsproject.dto.customs.BorderCrossingRequest;
import com.ltc.logisticsproject.dto.CargoWaybillDetail;
import com.ltc.logisticsproject.dto.CustomerSummary;
import com.ltc.logisticsproject.dto.DriverEarningsSummary;
import com.ltc.logisticsproject.dto.DvirSubmitRequest;
import com.ltc.logisticsproject.dto.ExpenseRequest;
import com.ltc.logisticsproject.dto.FatigueAlertRequest;
import com.ltc.logisticsproject.dto.HosStatusResponse;
import com.ltc.logisticsproject.dto.LiveTripResponse;
import com.ltc.logisticsproject.dto.LocationRequest;
import com.ltc.logisticsproject.dto.rating.RatingDetailResponse;
import com.ltc.logisticsproject.dto.TripRejectRequest;
import com.ltc.logisticsproject.dto.TripStatusUpdateRequest;
import com.ltc.logisticsproject.entity.*;
import com.ltc.logisticsproject.repository.*;
import com.ltc.logisticsproject.service.DriverTripService;
import com.ltc.logisticsproject.service.ExpenseService;
import com.ltc.logisticsproject.service.FileStorageService;
import com.ltc.logisticsproject.service.HosService;
import com.ltc.logisticsproject.service.NotificationService;
import com.ltc.logisticsproject.service.RatingService;
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
    // Yolda fövqəladə hal bildirişi (bax reportIncident aşağıda) —
    // FatigueAlert ilə eyni naxış, sadəcə dispetçerə TƏCİLİ (email daxil)
    // bildiriş getdiyi üçün NotificationService birbaşa çağırılır.
    final TripIncidentRepository tripIncidentRepository;
    final NotificationService notificationService;
    // "Reytinqlərim" səhifəsi — sürücü öz aldığı ulduz/şərhləri, hansı
    // reysdən gəldiyini görsün deyə (bax RatingService#getDriverRatingsDetailed).
    final RatingService ratingService;
    // Server-tərəfli HOS (iş saatı) qeydiyyatı — bax HosService qeydi
    // (RestModeCard.jsx-in köhnə client-side stopwatch-ını əvəz edir).
    final HosService hosService;
    // Reys öncəsi/sonrası yoxlama siyahısı (DVIR) — bax entity/DvirInspection.
    final DvirInspectionRepository dvirInspectionRepository;

    // "Əsas 1 şəkil + ətraflı 3-4 şəkil" — professional vehicle-gallery UX
    // (tək əsas şəkil sürətli tanınma üçün, məhdud sayda detal şəkli isə
    // qalereyanı nizamlı saxlayır, sürücü onlarla şəkil yükləyib
    // "istifadəçi görsün" məqsədini itirmir).
    private static final int MAX_DETAIL_PHOTOS = 4;

    // "Aktiv" reyslər siyahısı — PENDING_ACCEPTANCE (hələ qəbul edilməyib) və
    // REJECTED (artıq imtina edilib, sürücüyə aid deyil) bura DAXIL DEYİL,
    // yalnız PLANNED/PICKED_UP/IN_TRANSIT (bax pendingAcceptanceTrips aşağıda
    // ayrıca endpoint).
    private static final List<TripStatus> ACTIVE_STATUSES =
            List.of(TripStatus.PLANNED, TripStatus.PICKED_UP, TripStatus.IN_TRANSIT);

    @GetMapping("/trips/current")
    public ResponseEntity<List<Trip>> currentTrips(Authentication authentication) {
        Long driverId = currentDriverId(authentication);
        return ResponseEntity.ok(tripRepository.findByDriverIdAndStatusIn(driverId, ACTIVE_STATUSES));
    }

    // Sürücüyə göndərilib, hələ qəbul/imtina edilməmiş reyslər — Sürücü
    // panelində "Yeni reys" kartı kimi göstərilir, Qəbul et/İmtina et
    // düymələri ilə (bax accept/reject aşağıda).
    @GetMapping("/trips/pending-acceptance")
    public ResponseEntity<List<LiveTripResponse>> pendingAcceptanceTrips(Authentication authentication) {
        Long driverId = currentDriverId(authentication);
        List<Trip> trips = tripRepository.findByDriverIdAndStatus(driverId, TripStatus.PENDING_ACCEPTANCE);
        return ResponseEntity.ok(trips.stream().map(this::buildLiveTripResponse).toList());
    }

    @PostMapping("/trips/{id}/accept")
    public ResponseEntity<Trip> acceptTrip(@PathVariable Long id, Authentication authentication) {
        Long driverId = currentDriverId(authentication);
        Trip updated = driverTripService.acceptTrip(id, driverId);
        tripBroadcastService.broadcastTripUpdate(id);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/trips/{id}/reject")
    public ResponseEntity<Trip> rejectTrip(@PathVariable Long id,
                                           @RequestBody(required = false) TripRejectRequest request,
                                           Authentication authentication) {
        Long driverId = currentDriverId(authentication);
        String reason = request != null ? request.getReason() : null;
        Trip updated = driverTripService.rejectTrip(id, driverId, reason);
        tripBroadcastService.broadcastTripUpdate(id);
        return ResponseEntity.ok(updated);
    }

    // Əvvəllər bare List<Trip> qaytarırdı — Trip.cargos @JsonIgnore olduğu
    // üçün frontend-də (DriverHistory.jsx) ünvan/tracking № kimi heç bir
    // detal göstərilə bilmirdi (bax "ətraflı göstər" istəyi). İndi
    // currentTripsLive ilə eyni zənginləşdirilmiş forma (bax
    // buildLiveTripResponse) — ətraflı görünüş üçün lazım olan
    // trackingNumber/pickup/destination/tarixlər buradan gəlir.
    @GetMapping("/trips/history")
    public ResponseEntity<List<LiveTripResponse>> tripHistory(Authentication authentication) {
        Long driverId = currentDriverId(authentication);
        List<Trip> trips = tripRepository.findByDriverIdAndStatus(driverId, TripStatus.DELIVERED);
        return ResponseEntity.ok(trips.stream().map(this::buildLiveTripResponse).toList());
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
        List<Trip> trips = tripRepository.findByDriverIdAndStatusIn(driverId, ACTIVE_STATUSES);
        return ResponseEntity.ok(trips.stream().map(this::buildLiveTripResponse).toList());
    }

    // /trips/current/live və /trips/history hər ikisi eyni "reys +
    // son GPS ping + təmsilçi yükün ünvanları + müştəri(lər)" formasını
    // paylaşır (bax LiveTripResponse) — məntiq təkrarlanmasın deyə ortaq
    // helper-ə çıxarıldı.
    private LiveTripResponse buildLiveTripResponse(Trip trip) {
        Double lat = null, lng = null;
        String lastUpdated = null;
        List<TrackingLog> logs = trackingLogRepository.findByTripIdOrderByRecordedAtAsc(trip.getId());
        if (!logs.isEmpty()) {
            TrackingLog last = logs.get(logs.size() - 1);
            lat = last.getLatitude();
            lng = last.getLongitude();
            lastUpdated = last.getRecordedAt().toString();
        }

        String pickupAddress = null;
        String destinationAddress = null;
        Double destLat = null, destLng = null;
        List<CustomerSummary> customers = List.of();
        if (trip.getCargos() != null && !trip.getCargos().isEmpty()) {
            Cargo firstCargo = trip.getCargos().get(0);
            pickupAddress = firstCargo.getPickupAddress();
            destinationAddress = firstCargo.getDestinationAddress();
            destLat = firstCargo.getDestinationLatitude();
            destLng = firstCargo.getDestinationLongitude();
            if (lat == null) {
                lat = firstCargo.getPickupLatitude();
                lng = firstCargo.getPickupLongitude();
            }
            customers = trip.getCargos().stream().map(CustomerSummary::from).toList();
        }

        return LiveTripResponse.builder()
                .tripId(trip.getId())
                .status(trip.getStatus())
                .driverName(trip.getDriver() != null ? trip.getDriver().getFullName() : null)
                .vehiclePlate(trip.getVehicle() != null ? trip.getVehicle().getPlateNumber() : null)
                .lastLatitude(lat)
                .lastLongitude(lng)
                .lastUpdatedAt(lastUpdated)
                .pickupAddress(pickupAddress)
                .destinationAddress(destinationAddress)
                .destinationLatitude(destLat)
                .destinationLongitude(destLng)
                .routeInfo(trip.getRouteInfo())
                .estimatedDistanceKm(trip.getEstimatedDistanceKm())
                .estimatedCost(trip.getEstimatedCost())
                .startedAt(trip.getStartedAt() != null ? trip.getStartedAt().toString() : null)
                .deliveredAt(trip.getDeliveredAt() != null ? trip.getDeliveredAt().toString() : null)
                .customers(customers)
                .build();
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

    @GetMapping("/ratings")
    public ResponseEntity<List<RatingDetailResponse>> myRatings(Authentication authentication) {
        Long driverId = currentDriverId(authentication);
        return ResponseEntity.ok(ratingService.getDriverRatingsDetailed(driverId));
    }

    // Mobil tətbiqin "mənim reytinqim" kartı üçün — orta bal + say.
    // RatingService#getDriverSummary artıq mövcud idi, sadəcə heç bir
    // controller onu sürücünün özünə açmırdı (yalnız admin/dispetçer/
    // DriverSuggestionService daxili istifadə edirdi).
    @GetMapping("/ratings/summary")
    public ResponseEntity<RatingService.RatingSummary> myRatingSummary(Authentication authentication) {
        Long driverId = currentDriverId(authentication);
        return ResponseEntity.ok(ratingService.getDriverSummary(driverId));
    }

    // "Yük qaiməsi" (bax dto/CargoWaybillDetail, DriverTripService#getWaybill)
    // — yolda nəzarət yoxlaması üçün lazım olan sənəd, kommersiya
    // fakturasından (məbləğ daşıyan) fərqlidir. Sürücü yalnız öz reysinin
    // yükü üçün baxa bilir (sahiblik yoxlanışı servisdə edilir).
    @GetMapping("/cargo/{cargoId}/waybill")
    public ResponseEntity<CargoWaybillDetail> waybill(@PathVariable Long cargoId, Authentication authentication) {
        Long driverId = currentDriverId(authentication);
        return ResponseEntity.ok(driverTripService.getWaybill(cargoId, driverId));
    }

    // Sürücü qazanc görünürlüyü — bax DriverEarningsSummary/DriverTripService
    // #getEarningsSummary qeydi (dəqiq maaş deyil, DELIVERED reyslərin
    // Trip.estimatedCost cəmi).
    @GetMapping("/earnings")
    public ResponseEntity<DriverEarningsSummary> earnings(Authentication authentication) {
        Long driverId = currentDriverId(authentication);
        return ResponseEntity.ok(driverTripService.getEarningsSummary(driverId));
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

    @PostMapping(value = "/trips/{id}/expenses", consumes = "application/json")
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

    // Qəbz fotosu ilə xərc əlavə etmə — foto könüllüdür (bax
    // ExpenseService#addExpense overload). Köhnə (yalnız JSON, fotosuz)
    // endpoint yuxarıda saxlanılır ki, foto seçilməyəndə frontend sadə JSON
    // göndərə bilsin; bu multipart versiya yalnız foto varsa istifadə olunur.
    @PostMapping(value = "/trips/{id}/expenses", consumes = "multipart/form-data")
    public ResponseEntity<TripExpense> addExpenseWithReceipt(@PathVariable Long id,
                                                              @RequestParam("category") ExpenseCategory category,
                                                              @RequestParam("amount") Double amount,
                                                              @RequestParam(value = "description", required = false) String description,
                                                              @RequestParam(value = "photo", required = false) MultipartFile photo,
                                                              Authentication authentication) {
        Long driverId = currentDriverId(authentication);
        Trip trip = tripRepository.findById(id).orElseThrow(() -> new RuntimeException("Reys tapılmadı"));
        if (!trip.getDriver().getId().equals(driverId)) {
            throw new RuntimeException("Bu reys sizə aid deyil");
        }

        String receiptPhotoUrl = (photo != null && !photo.isEmpty()) ? fileStorageService.store(photo) : null;

        return ResponseEntity.ok(expenseService.addExpense(id, category, amount, description, receiptPhotoUrl));
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

    // Server-tərəfli HOS — RestModeCard.jsx komponenti hər yüklənəndə cari
    // seqmenti (DRIVING/RESTING) və bugünkü ümumi sürücülük vaxtını bərpa
    // etmək üçün çağırır (bax HosService#getStatus).
    @GetMapping("/trips/{id}/hos/status")
    public ResponseEntity<HosStatusResponse> hosStatus(@PathVariable Long id, Authentication authentication) {
        Long driverId = currentDriverId(authentication);
        return ResponseEntity.ok(hosService.getStatus(id, driverId));
    }

    // Sürücülük/istirahət arasında keçid — açıq seqmenti bağlayıb əksini
    // açır (bax HosService#toggle). İlk çağırışda default DRIVING başlayır.
    @PostMapping("/trips/{id}/hos/toggle")
    public ResponseEntity<HosStatusResponse> hosToggle(@PathVariable Long id, Authentication authentication) {
        Long driverId = currentDriverId(authentication);
        return ResponseEntity.ok(hosService.toggle(id, driverId));
    }

    // Reys öncəsi/sonrası yoxlama siyahısı (DVIR) — sürücü bu reys üçün
    // artıq hansı yoxlamaları (PRE_TRIP/POST_TRIP) doldurduğunu görsün deyə
    // (bax DriverCurrentTrip.jsx — "tamamlanıb" nişanı).
    @GetMapping("/trips/{id}/dvir")
    public ResponseEntity<List<DvirInspection>> dvirList(@PathVariable Long id, Authentication authentication) {
        Long driverId = currentDriverId(authentication);
        Trip trip = tripRepository.findById(id).orElseThrow(() -> new RuntimeException("Reys tapılmadı"));
        if (!trip.getDriver().getId().equals(driverId)) {
            throw new RuntimeException("Bu reys sizə aid deyil");
        }
        return ResponseEntity.ok(dvirInspectionRepository.findByTripIdOrderByCreatedAtDesc(id));
    }

    // Yoxlama siyahısını təqdim edir — hər maddə üçün OK/DEFECT/NA (bax
    // dto/DvirSubmitRequest, frontend DVIR_ITEMS sabiti). Ən azı bir DEFECT
    // varsa dispetçer/admin dərhal xəbərdar olunur.
    @PostMapping("/trips/{id}/dvir")
    public ResponseEntity<DvirInspection> submitDvir(@PathVariable Long id,
                                                      @RequestBody DvirSubmitRequest request,
                                                      Authentication authentication) {
        Long driverId = currentDriverId(authentication);
        Trip trip = tripRepository.findById(id).orElseThrow(() -> new RuntimeException("Reys tapılmadı"));
        if (!trip.getDriver().getId().equals(driverId)) {
            throw new RuntimeException("Bu reys sizə aid deyil");
        }

        boolean hasDefects = request.getItems() != null
                && request.getItems().values().stream().anyMatch("DEFECT"::equals);

        DvirInspection inspection = DvirInspection.builder()
                .trip(trip)
                .type(request.getType())
                .driverName(trip.getDriver() != null ? trip.getDriver().getFullName() : null)
                .vehiclePlate(trip.getVehicle() != null ? trip.getVehicle().getPlateNumber() : null)
                .items(request.getItems())
                .hasDefects(hasDefects)
                .notes(request.getNotes())
                .resolved(false)
                .build();
        DvirInspection saved = dvirInspectionRepository.save(inspection);

        if (hasDefects) {
            notificationService.notifyDvirDefect(saved);
        }

        return ResponseEntity.ok(saved);
    }

    // Sürücü yolda fövqəladə hal (qəza, sınma, yol bağlanması, digər) bildirir
    // — foto könüllüdür. Dispetçer/admin dərhal (in-app + email) xəbərdar
    // olur, Control Tower-da qırmızı banner kimi görünür (bax
    // NotificationService#notifyIncidentReported).
    @PostMapping(value = "/trips/{id}/incidents", consumes = "multipart/form-data")
    public ResponseEntity<TripIncident> reportIncident(@PathVariable Long id,
                                                        @RequestParam("type") IncidentType type,
                                                        @RequestParam(value = "description", required = false) String description,
                                                        @RequestParam(value = "photo", required = false) MultipartFile photo,
                                                        Authentication authentication) {
        Long driverId = currentDriverId(authentication);
        Trip trip = tripRepository.findById(id).orElseThrow(() -> new RuntimeException("Reys tapılmadı"));
        if (!trip.getDriver().getId().equals(driverId)) {
            throw new RuntimeException("Bu reys sizə aid deyil");
        }

        String photoUrl = (photo != null && !photo.isEmpty()) ? fileStorageService.store(photo) : null;

        TripIncident incident = TripIncident.builder()
                .trip(trip)
                .driverName(trip.getDriver() != null ? trip.getDriver().getFullName() : null)
                .vehiclePlate(trip.getVehicle() != null ? trip.getVehicle().getPlateNumber() : null)
                .type(type)
                .description(description)
                .photoUrl(photoUrl)
                .resolved(false)
                .build();
        TripIncident saved = tripIncidentRepository.save(incident);

        notificationService.notifyIncidentReported(saved);

        return ResponseEntity.ok(saved);
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
