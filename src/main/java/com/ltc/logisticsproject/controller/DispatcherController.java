package com.ltc.logisticsproject.controller;

import com.ltc.logisticsproject.dto.AnomalyExpenseResponse;
import com.ltc.logisticsproject.dto.customs.BorderCrossingRequest;
import com.ltc.logisticsproject.dto.CargoRejectRequest;
import com.ltc.logisticsproject.dto.CostEstimateResponse;
import com.ltc.logisticsproject.dto.CustomerSummary;
import com.ltc.logisticsproject.dto.customs.CustomsDeclarationRequest;
import com.ltc.logisticsproject.dto.DispatcherAnalyticsResponse;
import com.ltc.logisticsproject.dto.DispatcherCargoRequest;
import com.ltc.logisticsproject.dto.DispatcherKpiResponse;
import com.ltc.logisticsproject.dto.DriverAvailabilityResponse;
import com.ltc.logisticsproject.dto.DriverSuggestionResponse;
import com.ltc.logisticsproject.dto.LiveTripResponse;
import com.ltc.logisticsproject.dto.rating.RatingDetailResponse;
import com.ltc.logisticsproject.dto.TrailerPoolResponse;
import com.ltc.logisticsproject.dto.TripRequest;
import com.ltc.logisticsproject.entity.*;
import com.ltc.logisticsproject.repository.*;
import com.ltc.logisticsproject.service.AdminReportService;
import com.ltc.logisticsproject.service.CustomsDutyService;
import com.ltc.logisticsproject.service.DispatcherService;
import com.ltc.logisticsproject.service.DriverSuggestionService;
import com.ltc.logisticsproject.service.FileStorageService;
import com.ltc.logisticsproject.service.HosService;
import com.ltc.logisticsproject.service.NotificationService;
import com.ltc.logisticsproject.service.RatingService;
import com.ltc.logisticsproject.service.TripCostEstimationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dispatcher")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DispatcherController {

    final CargoRepository cargoRepository;
    final DriverRepository driverRepository;
    final VehicleRepository vehicleRepository;
    final TrailerRepository trailerRepository;
    final TripRepository tripRepository;
    final WarehouseRepository warehouseRepository;
    final TrackingLogRepository trackingLogRepository;
    final FatigueAlertRepository fatigueAlertRepository;
    final DispatcherService dispatcherService;
    // Reused from the Admin reporting domain (Stage 4): dispatchers cannot
    // hit /api/admin/** directly (see SecurityConfig), so we expose thin
    // dispatcher-scoped wrappers around the same service instead of
    // duplicating the aggregation logic.
    final AdminReportService adminReportService;

    // Beynəlxalq göndəriş bloku: sənədləşmə, gömrük bəyannaməsi/hesablama,
    // sərhəd keçidi jurnalı.
    final TradeDocumentRepository tradeDocumentRepository;
    final CustomsDeclarationRepository customsDeclarationRepository;
    final BorderCrossingRepository borderCrossingRepository;
    final FileStorageService fileStorageService;
    final CustomsDutyService customsDutyService;
    final TripCostEstimationService tripCostEstimationService;
    // Dispetçer "Reytinqlər" səhifəsi — hansı reysdən sürücüyə nə qiymət/şərh
    // gəldiyini görsün deyə (bax RatingService#getAllRatingsDetailed,
    // AdminManagementController-dəki eyni endpoint-in dispetçer üçün analoqu).
    final RatingService ratingService;
    // "İmtina et" — yükü ləğv edəndə müştəriyə bildiriş göndərmək üçün
    // (bax rejectCargo aşağıda).
    final UserRepository userRepository;
    final NotificationService notificationService;
    // Sürücünün yolda bildirdiyi fövqəladə hallar (bax
    // DriverController#reportIncident) — Control Tower-da qırmızı banner.
    final TripIncidentRepository tripIncidentRepository;
    // Reys öncəsi/sonrası yoxlama siyahısı (DVIR) — defekt qeyd olunanlar
    // Control Tower-da banner kimi göstərilir (bax DriverController#submitDvir).
    final DvirInspectionRepository dvirInspectionRepository;
    // Dispetçerin sürücü seçimində HOS (iş saatı) vəziyyətini görməsi üçün
    // (bax availableDrivers).
    final HosService hosService;
    // "Ən uyğun sürücü" təklifi (bax suggestDrivers) — HOS+DVIR+məsafə+tutum+
    // reytinq amillərini birləşdirən ayrıca servis.
    final DriverSuggestionService driverSuggestionService;

    @GetMapping("/cargo/pending")
    public ResponseEntity<List<Cargo>> pendingCargo() {
        return ResponseEntity.ok(cargoRepository.findByStatus(CargoStatus.PENDING));
    }

    // Dispetçer "Gözləyən yüklər" siyahısından xoşuna gəlməyən/qəbul etmək
    // istəmədiyi yükü imtina edir — real DB silinməsi əvəzinə CANCELLED
    // statusuna keçirilir (bax CargoStatus qeydi) ki, müştəri öz sifariş
    // tarixçəsində "Ləğv edildi" kimi görsün, tamamilə yoxa çıxmasın.
    // Yalnız hələ təhkim olunmamış (PENDING) yüklər üçün — reysə bağlanmış
    // yükün ləğvi ayrı bir axın tələb edir, bu scope-da deyil.
    @PostMapping("/cargo/{id}/reject")
    public ResponseEntity<Cargo> rejectCargo(@PathVariable Long id, @RequestBody(required = false) CargoRejectRequest request) {
        Cargo cargo = cargoRepository.findById(id).orElseThrow(() -> new RuntimeException("Yük tapılmadı"));
        if (cargo.getStatus() != CargoStatus.PENDING) {
            throw new RuntimeException("Yalnız gözləyən (hələ təhkim olunmamış) yüklər imtina edilə bilər");
        }

        String reason = request != null ? request.getReason() : null;
        cargo.setStatus(CargoStatus.CANCELLED);
        cargo.setCancelReason(reason);
        Cargo saved = cargoRepository.save(cargo);

        if (cargo.getCustomer() != null) {
            userRepository.findByCustomerId(cargo.getCustomer().getId()).ifPresent(user ->
                    notificationService.notifyWithEmail(
                            user.getId(), cargo.getCustomer().getEmail(), NotificationType.ORDER_CANCELLED,
                            "Sifarişiniz ləğv edildi",
                            saved.getTrackingNumber() + " nömrəli sifarişiniz dispetçer tərəfindən ləğv edildi."
                                    + (reason != null && !reason.isBlank() ? " Səbəb: " + reason : ""),
                            "/customer/orders", "Fleetra — sifarişiniz ləğv edildi", "Sifarişə bax"
                    )
            );
        }

        return ResponseEntity.ok(saved);
    }

    @GetMapping("/ratings")
    public ResponseEntity<List<RatingDetailResponse>> allRatings() {
        return ResponseEntity.ok(ratingService.getAllRatingsDetailed());
    }

    // Lets a dispatcher manually enter an order (e.g. a phone-in request)
    // straight into the same PENDING queue that customer self-service
    // orders land in — reuses CargoQueue as-is, no separate UI needed once
    // the order exists. No Customer account is required; customerName/Phone
    // are stored as plain text exactly like a registered customer's order.
    @PostMapping("/cargo")
    public ResponseEntity<Cargo> createCargo(@RequestBody DispatcherCargoRequest request) {
        Warehouse warehouse = null;
        if (request.getOriginWarehouseId() != null) {
            warehouse = warehouseRepository.findById(request.getOriginWarehouseId())
                    .orElseThrow(() -> new RuntimeException("Anbar tapılmadı"));
        }

        Cargo cargo = Cargo.builder()
                .description(request.getDescription())
                .weight(request.getWeight())
                .volume(request.getVolume())
                .originWarehouse(warehouse)
                .pickupAddress(request.getPickupAddress())
                .pickupLatitude(request.getPickupLatitude())
                .pickupLongitude(request.getPickupLongitude())
                .destinationAddress(request.getDestinationAddress())
                .destinationLatitude(request.getDestinationLatitude())
                .destinationLongitude(request.getDestinationLongitude())
                .cargoType(request.getCargoType())
                .urgency(request.getUrgency())
                .requestedPickupDate(request.getRequestedPickupDate())
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .status(CargoStatus.PENDING)
                .requiresCustoms(request.isRequiresCustoms())
                .preferredTransportMode(request.getPreferredTransportMode())
                .incoterm(request.getIncoterm())
                .originCountry(request.getOriginCountry())
                .destinationCountry(request.getDestinationCountry())
                .transitCountries(request.getTransitCountries())
                .build();

        return ResponseEntity.ok(cargoRepository.save(cargo));
    }

    // Sadəcə ACTIVE sürücü siyahısı deyil — hər sürücü üçün HOS (iş saatı)
    // vəziyyəti və həll olunmamış DVIR defekti də əlavə olunur ki, dispetçer
    // "Reys yarat" formasında uyğun olmayan sürücünü seçməzdən ƏVVƏL görsün
    // (bax DriverAvailabilityResponse, CargoQueue.jsx driver select — real
    // TMS platformalarında (Motive/Samsara) bu, dispatch axınının nüvəsidir).
    @GetMapping("/drivers/available")
    public ResponseEntity<List<DriverAvailabilityResponse>> availableDrivers() {
        List<Driver> drivers = driverRepository.findByStatus(DriverStatus.ACTIVE);
        List<DriverAvailabilityResponse> result = drivers.stream().map(d -> {
            HosService.DriverHosSnapshot hos = hosService.getDriverSnapshot(d.getId());
            boolean hasDefect = !dvirInspectionRepository
                    .findByTrip_Driver_IdAndHasDefectsTrueAndResolvedFalse(d.getId()).isEmpty();
            RatingService.RatingSummary ratingSummary = ratingService.getDriverSummary(d.getId());
            return DriverAvailabilityResponse.builder()
                    .id(d.getId())
                    .fullName(d.getFullName())
                    .phone(d.getPhone())
                    .hasActiveTrip(hos.hasActiveTrip())
                    .hosStatus(hos.hosStatus())
                    .todayDrivingHours(hos.todayDrivingHours())
                    .remainingDrivingHours(hos.remainingDrivingHours())
                    .fatigueWarning(hos.fatigueWarning())
                    .hasUnresolvedDvirDefect(hasDefect)
                    .ratingAverage(ratingSummary.average())
                    .ratingCount(ratingSummary.count())
                    .build();
        }).toList();
        return ResponseEntity.ok(result);
    }

    // Seçilmiş yük(lər) üçün "ən uyğun sürücü" sıralanmış təklifi (bax
    // DriverSuggestionService, CargoQueue.jsx "Təklif et" düyməsi). Sırf
    // görünürlük/köməkdir — heç nəyi məcburi etmir, dispetçer siyahıdakı hər
    // hansı sürücünü seçə bilər.
    @GetMapping("/drivers/suggest")
    public ResponseEntity<List<DriverSuggestionResponse>> suggestDrivers(@RequestParam List<Long> cargoIds) {
        return ResponseEntity.ok(driverSuggestionService.suggest(cargoIds));
    }

    // driverId veriləndə (Reys yarat formasında sürücü seçildikdə) siyahı
    // filtrlənir: şirkət tırları (COMPANY) həmişə görünür, sürücüyə məxsus
    // (DRIVER_OWNED) tırlar isə YALNIZ öz sahibi seçiləndə görünür — başqa
    // sürücüyə heç təklif olunmur. driverId verilməyəndə (səhifə ilk
    // açılanda) hər şey görünür ki, dispetçer əvvəlcədən filo ilə tanış ola
    // bilsin. Backend tərəfdə də DispatcherService#createTrip bunu təsdiqləyir.
    @GetMapping("/vehicles")
    public ResponseEntity<List<Vehicle>> allVehicles(@RequestParam(required = false) Long driverId) {
        List<Vehicle> vehicles = vehicleRepository.findAll();
        if (driverId != null) {
            vehicles = vehicles.stream()
                    .filter(v -> v.getOwnerType() != OwnerType.DRIVER_OWNED
                            || (v.getDriver() != null && v.getDriver().getId().equals(driverId)))
                    .toList();
        }
        return ResponseEntity.ok(vehicles);
    }

    @GetMapping("/trailers")
    public ResponseEntity<List<Trailer>> allTrailers(@RequestParam(required = false) Long driverId) {
        List<Trailer> trailers = trailerRepository.findAll();
        if (driverId != null) {
            trailers = trailers.stream()
                    .filter(t -> t.getOwnerType() != OwnerType.DRIVER_OWNED
                            || (t.getDriver() != null && t.getDriver().getId().equals(driverId)))
                    .toList();
        }
        return ResponseEntity.ok(trailers);
    }

    // Qoşqu hovuzu (Trailer Pool) — hər qoşqunun HAZIRKI vəziyyəti: bazadadır/
    // boşdur, yoxsa hansısa reysə bağlıdır (bağlıdırsa yüklü/boş və son GPS
    // mövqeyi ilə). Əvvəllər qoşqunun statusu heç yerdə görünmürdü, yalnız
    // "Reys yarat" formasındakı seçim siyahısında adı keçirdi. Drop-and-hook
    // idarəetməsi üçün əsas ekran (bax McLeod/böyük TMS platformaları).
    private static final List<TripStatus> ACTIVE_TRIP_STATUSES =
            List.of(TripStatus.PLANNED, TripStatus.PICKED_UP, TripStatus.IN_TRANSIT);

    @GetMapping("/trailers/pool")
    public ResponseEntity<List<TrailerPoolResponse>> trailerPool() {
        List<Trailer> trailers = trailerRepository.findAll();
        List<TrailerPoolResponse> result = trailers.stream().map(tr -> {
            List<Trip> activeTrips = tripRepository.findByTrailerIdAndStatusIn(tr.getId(), ACTIVE_TRIP_STATUSES);
            TrailerPoolResponse.TrailerPoolResponseBuilder builder = TrailerPoolResponse.builder()
                    .id(tr.getId())
                    .plateNumber(tr.getPlateNumber())
                    .capacity(tr.getCapacity())
                    .ownerType(tr.getOwnerType() != null ? tr.getOwnerType().name() : "COMPANY")
                    .ownerDriverName(tr.getDriver() != null ? tr.getDriver().getFullName() : null)
                    .onTrip(false);

            if (!activeTrips.isEmpty()) {
                Trip trip = activeTrips.get(0);
                List<TrackingLog> logs = trackingLogRepository.findByTripIdOrderByRecordedAtAsc(trip.getId());
                TrackingLog last = logs.isEmpty() ? null : logs.get(logs.size() - 1);
                builder.onTrip(true)
                        .activeTripId(trip.getId())
                        .tripStatus(trip.getStatus().name())
                        .loaded(trip.getStatus() == TripStatus.PICKED_UP || trip.getStatus() == TripStatus.IN_TRANSIT)
                        .driverName(trip.getDriver() != null ? trip.getDriver().getFullName() : null)
                        .vehiclePlate(trip.getVehicle() != null ? trip.getVehicle().getPlateNumber() : null)
                        .lastLatitude(last != null ? last.getLatitude() : null)
                        .lastLongitude(last != null ? last.getLongitude() : null)
                        .lastUpdatedAt(last != null ? last.getRecordedAt().toString() : null);
            }
            return builder.build();
        }).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/trips")
    public ResponseEntity<List<Trip>> allTrips() {
        return ResponseEntity.ok(tripRepository.findAll());
    }

    @PostMapping("/trips")
    public ResponseEntity<Trip> createTrip(@RequestBody TripRequest request) {
        return ResponseEntity.ok(dispatcherService.createTrip(request));
    }

    // Sürücü qəbul/imtina etmirsə (və ya ödəniş çox gözləyirsə), dispetçer
    // reysi əl ilə ləğv edib yükü yenidən növbəyə qaytara bilsin — bax
    // DispatcherService#cancelTrip.
    @PostMapping("/trips/{id}/cancel")
    public ResponseEntity<Trip> cancelTrip(@PathVariable Long id) {
        return ResponseEntity.ok(dispatcherService.cancelTrip(id));
    }

    // "Reys yarat" formasında seçilmiş yüklər + tır üçün başlanğıc təxmini
    // məsafə/xərc təklifi (bax TripCostEstimationService — real yük daşıma
    // qiymətləndirməsinin sadələşdirilmiş modeli: yanacaq + sürücü + servis +
    // baza xərc, üzərinə yük növü/təcililik əlavələri). Dispetçer nəticəni
    // formadakı xanalarda istənilən vaxt əl ilə dəyişə bilər.
    @GetMapping("/trips/cost-estimate")
    public ResponseEntity<CostEstimateResponse> estimateTripCost(@RequestParam List<Long> cargoIds,
                                                                   @RequestParam(required = false) Long vehicleId,
                                                                   @RequestParam(required = false) Long trailerId) {
        List<Cargo> cargos = cargoRepository.findAllById(cargoIds);
        Vehicle vehicle = vehicleId != null ? vehicleRepository.findById(vehicleId).orElse(null) : null;
        Trailer trailer = trailerId != null ? trailerRepository.findById(trailerId).orElse(null) : null;
        return ResponseEntity.ok(tripCostEstimationService.estimate(cargos, vehicle, trailer));
    }

    // Stage 4 — Control Tower: warehouse list, needed client-side for the
    // "closest 3 warehouses" geospatial lookup on truck click (computed via
    // haversine in the frontend, so no dedicated /nearest endpoint needed).
    @GetMapping("/warehouses")
    public ResponseEntity<List<Warehouse>> warehouses() {
        return ResponseEntity.ok(warehouseRepository.findAll());
    }

    // Stage 4 — Control Tower map + Dispatcher trips table + backhaul
    // matcher all share this single enriched-trip shape: every trip (any
    // status) with its most recent GPS ping, driver/vehicle display info
    // and destination coordinates. Delivered trips are still included
    // (frontend filters them out of the live map) because the backhaul
    // "recommended return cargo" widget needs a DELIVERED trip's last
    // destination to match nearby pending pickups. This is a polling
    // endpoint for now; Stage 6 will add a WebSocket channel pushing the
    // same shape in real time so the frontend component won't need to change.
    @GetMapping("/trips/live")
    public ResponseEntity<List<LiveTripResponse>> liveTrips() {
        List<Trip> allTrips = tripRepository.findAll();

        List<LiveTripResponse> result = allTrips.stream().map(trip -> {
            Double lat = null, lng = null;
            String lastUpdated = null;
            List<TrackingLog> logs = trackingLogRepository.findByTripIdOrderByRecordedAtAsc(trip.getId());
            if (!logs.isEmpty()) {
                TrackingLog last = logs.get(logs.size() - 1);
                lat = last.getLatitude();
                lng = last.getLongitude();
                lastUpdated = last.getRecordedAt().toString();
            }

            // No live ping yet -> fall back to the first cargo's pickup
            // point so the truck still shows up on the map at a sane spot.
            String pickupAddress = null;
            String destinationAddress = null;
            Double destLat = null, destLng = null;
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
            }

            // Yükü verən müştəri(lər) — bir reysə bir neçə yük birləşdirilə
            // bildiyi üçün (bax CargoQueue) siyahı şəklindədir.
            List<CustomerSummary> customers = trip.getCargos() == null ? List.of()
                    : trip.getCargos().stream().map(CustomerSummary::from).toList();

            return LiveTripResponse.builder()
                    .tripId(trip.getId())
                    .status(trip.getStatus())
                    .driverName(trip.getDriver() != null ? trip.getDriver().getFullName() : null)
                    .driverPhone(trip.getDriver() != null ? trip.getDriver().getPhone() : null)
                    .vehiclePlate(trip.getVehicle() != null ? trip.getVehicle().getPlateNumber() : null)
                    .trailerPlate(trip.getTrailer() != null ? trip.getTrailer().getPlateNumber() : null)
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
        }).toList();

        return ResponseEntity.ok(result);
    }

    // Stage 4 — crimson anomaly banner. Dispatcher can't reach
    // /api/admin/reports/anomalies (ADMIN-only), so this exposes the same
    // AdminReportService data under the dispatcher's own security scope.
    @GetMapping("/reports/anomalies")
    public ResponseEntity<List<AnomalyExpenseResponse>> anomalies() {
        return ResponseEntity.ok(adminReportService.getAnomalies());
    }

    // Stage 4 — Recharts widgets: monthly expense totals + estimated
    // monthly carbon footprint (see AdminReportService for the estimate's
    // documented assumptions).
    @GetMapping("/reports/analytics")
    public ResponseEntity<DispatcherAnalyticsResponse> analytics() {
        return ResponseEntity.ok(adminReportService.getMonthlyAnalytics());
    }

    // Dispetçer KPI kartları (vaxtında çatdırma/boş kilometraj TƏXMİNLƏRİ +
    // qoşqu utilizasiyası) — bax AdminReportService#getDispatcherKpis,
    // DispatcherKpiResponse-dəki qeyd (sadələşdirilmiş modeldir).
    @GetMapping("/reports/kpi")
    public ResponseEntity<DispatcherKpiResponse> kpi() {
        return ResponseEntity.ok(adminReportService.getDispatcherKpis());
    }

    // Stage 5 — Driver PWA "Rest Mode" alerts land here so the Control
    // Tower can show them alongside the crimson expense-anomaly banner.
    @GetMapping("/fatigue-alerts")
    public ResponseEntity<List<FatigueAlert>> fatigueAlerts() {
        return ResponseEntity.ok(fatigueAlertRepository.findByResolvedFalseOrderByCreatedAtDesc());
    }

    // Sürücünün yolda bildirdiyi fövqəladə hallar — qırmızı banner (bax
    // FatigueAlertBanner-in eyni naxışı, IncidentBanner.jsx).
    @GetMapping("/incidents")
    public ResponseEntity<List<TripIncident>> incidents() {
        return ResponseEntity.ok(tripIncidentRepository.findByResolvedFalseOrderByCreatedAtDesc());
    }

    @PostMapping("/incidents/{id}/resolve")
    public ResponseEntity<TripIncident> resolveIncident(@PathVariable Long id) {
        TripIncident incident = tripIncidentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hadisə tapılmadı"));
        incident.setResolved(true);
        incident.setResolvedAt(java.time.LocalDateTime.now());
        return ResponseEntity.ok(tripIncidentRepository.save(incident));
    }

    // Reys öncəsi/sonrası yoxlama siyahısında (DVIR) defekt qeyd olunanlar —
    // eyni naxış (FatigueAlert/TripIncident).
    @GetMapping("/dvir")
    public ResponseEntity<List<DvirInspection>> dvirDefects() {
        return ResponseEntity.ok(dvirInspectionRepository.findByHasDefectsTrueAndResolvedFalseOrderByCreatedAtDesc());
    }

    @PostMapping("/dvir/{id}/resolve")
    public ResponseEntity<DvirInspection> resolveDvir(@PathVariable Long id) {
        DvirInspection inspection = dvirInspectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Yoxlama qeydi tapılmadı"));
        inspection.setResolved(true);
        inspection.setResolvedAt(java.time.LocalDateTime.now());
        return ResponseEntity.ok(dvirInspectionRepository.save(inspection));
    }

    @PostMapping("/fatigue-alerts/{id}/resolve")
    public ResponseEntity<FatigueAlert> resolveFatigueAlert(@PathVariable Long id) {
        FatigueAlert alert = fatigueAlertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Xəbərdarlıq tapılmadı"));
        alert.setResolved(true);
        return ResponseEntity.ok(fatigueAlertRepository.save(alert));
    }

    // ---- Beynəlxalq göndəriş: Sənədləşmə (TradeDocument) ----------------

    @GetMapping("/cargo/{cargoId}/documents")
    public ResponseEntity<List<TradeDocument>> getDocuments(@PathVariable Long cargoId) {
        return ResponseEntity.ok(tradeDocumentRepository.findByCargoIdOrderByCreatedAtDesc(cargoId));
    }

    @PostMapping(value = "/cargo/{cargoId}/documents", consumes = "multipart/form-data")
    public ResponseEntity<TradeDocument> uploadDocument(@PathVariable Long cargoId,
                                                         @RequestParam("file") MultipartFile file,
                                                         @RequestParam("type") DocumentType type,
                                                         @RequestParam(value = "uploadedByName", required = false) String uploadedByName) {
        Cargo cargo = cargoRepository.findById(cargoId)
                .orElseThrow(() -> new RuntimeException("Yük tapılmadı"));

        String url = fileStorageService.store(file);
        TradeDocument doc = TradeDocument.builder()
                .cargo(cargo)
                .type(type)
                .fileUrl(url)
                .originalFileName(file.getOriginalFilename())
                .uploadedByName(uploadedByName)
                .status(DocumentStatus.PENDING)
                .build();
        return ResponseEntity.ok(tradeDocumentRepository.save(doc));
    }

    @PostMapping("/documents/{id}/verify")
    public ResponseEntity<TradeDocument> verifyDocument(@PathVariable Long id) {
        TradeDocument doc = tradeDocumentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sənəd tapılmadı"));
        doc.setStatus(DocumentStatus.VERIFIED);
        doc.setVerifiedAt(java.time.LocalDateTime.now());
        return ResponseEntity.ok(tradeDocumentRepository.save(doc));
    }

    @PostMapping("/documents/{id}/reject")
    public ResponseEntity<TradeDocument> rejectDocument(@PathVariable Long id) {
        TradeDocument doc = tradeDocumentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sənəd tapılmadı"));
        doc.setStatus(DocumentStatus.REJECTED);
        return ResponseEntity.ok(tradeDocumentRepository.save(doc));
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id) {
        tradeDocumentRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Sənəd silindi"));
    }

    // ---- Beynəlxalq göndəriş: Gömrük bəyannaməsi (CustomsDeclaration) ----

    @GetMapping("/cargo/{cargoId}/customs-declaration")
    public ResponseEntity<CustomsDeclaration> getDeclaration(@PathVariable Long cargoId) {
        return customsDeclarationRepository.findByCargoId(cargoId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    // Yaradır (ilk dəfə) və ya mövcud bəyannaməni yeniləyir; hər iki halda
    // rüsum/ƏDV CustomsDutyService ilə yenidən hesablanır ki, dəyər dəyişəndə
    // köhnəlmiş məbləğ qalmasın.
    @PostMapping("/cargo/{cargoId}/customs-declaration")
    public ResponseEntity<CustomsDeclaration> upsertDeclaration(@PathVariable Long cargoId,
                                                                  @RequestBody CustomsDeclarationRequest request) {
        Cargo cargo = cargoRepository.findById(cargoId)
                .orElseThrow(() -> new RuntimeException("Yük tapılmadı"));

        CustomsDeclaration declaration = customsDeclarationRepository.findByCargoId(cargoId)
                .orElse(CustomsDeclaration.builder().cargo(cargo).status(DeclarationStatus.DRAFT).build());

        if (declaration.getStatus() == DeclarationStatus.CLEARED) {
            throw new RuntimeException("Artıq gömrükdən keçmiş bəyannamə dəyişdirilə bilməz");
        }

        declaration.setOriginCountry(request.getOriginCountry());
        declaration.setDestinationCountry(request.getDestinationCountry());
        declaration.setHsCode(request.getHsCode());
        declaration.setDeclaredValue(request.getDeclaredValue());
        if (request.getCurrency() != null) declaration.setCurrency(request.getCurrency());

        double declaredValue = request.getDeclaredValue() != null ? request.getDeclaredValue() : 0.0;
        CustomsDutyService.DutyCalculation calc = customsDutyService.calculate(cargo, declaredValue);
        declaration.setDutyRatePercent(calc.dutyRatePercent());
        declaration.setVatRatePercent(calc.vatRatePercent());
        declaration.setDutyAmount(calc.dutyAmount());
        declaration.setVatAmount(calc.vatAmount());
        declaration.setTotalPayable(calc.totalPayable());
        if (declaration.getStatus() == null) declaration.setStatus(DeclarationStatus.DRAFT);

        CustomsDeclaration saved = customsDeclarationRepository.save(declaration);

        cargo.setRequiresCustoms(true);
        cargoRepository.save(cargo);

        return ResponseEntity.ok(saved);
    }

    @PostMapping("/customs-declaration/{id}/submit")
    public ResponseEntity<CustomsDeclaration> submitDeclaration(@PathVariable Long id) {
        CustomsDeclaration declaration = customsDeclarationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bəyannamə tapılmadı"));
        if (declaration.getDeclaredValue() == null || declaration.getDeclaredValue() <= 0) {
            throw new RuntimeException("Bəyan edilmiş dəyər düzgün daxil edilməyib");
        }
        declaration.setStatus(DeclarationStatus.SUBMITTED);
        declaration.setSubmittedAt(java.time.LocalDateTime.now());
        return ResponseEntity.ok(customsDeclarationRepository.save(declaration));
    }

    @PostMapping("/customs-declaration/{id}/clear")
    public ResponseEntity<CustomsDeclaration> clearDeclaration(@PathVariable Long id) {
        CustomsDeclaration declaration = customsDeclarationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bəyannamə tapılmadı"));
        if (declaration.getStatus() != DeclarationStatus.SUBMITTED) {
            throw new RuntimeException("Yalnız göndərilmiş (SUBMITTED) bəyannamə gömrükdən keçirilə bilər");
        }
        declaration.setStatus(DeclarationStatus.CLEARED);
        declaration.setClearedAt(java.time.LocalDateTime.now());
        return ResponseEntity.ok(customsDeclarationRepository.save(declaration));
    }

    @PostMapping("/customs-declaration/{id}/reject")
    public ResponseEntity<CustomsDeclaration> rejectDeclaration(@PathVariable Long id) {
        CustomsDeclaration declaration = customsDeclarationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bəyannamə tapılmadı"));
        declaration.setStatus(DeclarationStatus.REJECTED);
        return ResponseEntity.ok(customsDeclarationRepository.save(declaration));
    }

    // ---- Beynəlxalq göndəriş: Sərhəd keçidi (BorderCrossing) -------------

    @GetMapping("/trips/{tripId}/border-crossings")
    public ResponseEntity<List<BorderCrossing>> getBorderCrossings(@PathVariable Long tripId) {
        return ResponseEntity.ok(borderCrossingRepository.findByTripIdOrderByCrossedAtAsc(tripId));
    }

    @PostMapping("/trips/{tripId}/border-crossings")
    public ResponseEntity<BorderCrossing> addBorderCrossing(@PathVariable Long tripId,
                                                             @RequestBody BorderCrossingRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Reys tapılmadı"));

        BorderCrossing crossing = BorderCrossing.builder()
                .trip(trip)
                .borderPointName(request.getBorderPointName())
                .country(request.getCountry())
                .customsStatus(request.getCustomsStatus() != null ? request.getCustomsStatus() : BorderCustomsStatus.PENDING)
                .recordedBy("Dispetçer")
                .notes(request.getNotes())
                .build();
        return ResponseEntity.ok(borderCrossingRepository.save(crossing));
    }
}
