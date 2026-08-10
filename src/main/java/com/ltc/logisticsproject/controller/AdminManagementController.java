package com.ltc.logisticsproject.controller;

import com.ltc.logisticsproject.dto.auth.AdminSetPasswordRequest;
import com.ltc.logisticsproject.dto.customs.CustomsTariffRequest;
import com.ltc.logisticsproject.dto.DispatcherAdminView;
import com.ltc.logisticsproject.dto.DispatcherRequest;
import com.ltc.logisticsproject.dto.DriverAdminView;
import com.ltc.logisticsproject.dto.rating.RatingDetailResponse;
import com.ltc.logisticsproject.dto.TrailerRequest;
import com.ltc.logisticsproject.dto.VehicleRequest;
import com.ltc.logisticsproject.dto.WarehouseRequest;
import com.ltc.logisticsproject.entity.*;
import com.ltc.logisticsproject.repository.*;
import com.ltc.logisticsproject.service.AuditLogService;
import com.ltc.logisticsproject.service.RatingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminManagementController {

    final UserRepository userRepository;
    final WarehouseRepository warehouseRepository;
    final VehicleRepository vehicleRepository;
    final TrailerRepository trailerRepository;
    final DriverRepository driverRepository;
    final TripRepository tripRepository;
    final PasswordEncoder passwordEncoder;
    final CustomsTariffRepository customsTariffRepository;
    final RatingService ratingService;
    final AuditLogService auditLogService;

    @PostMapping("/dispatchers")
    public ResponseEntity<?> createDispatcher(@RequestBody DispatcherRequest request, Authentication authentication) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Bu istifadəçi adı artıq mövcuddur");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.DISPATCHER)
                .fullName(request.getFullName())
                .enabled(true)
                .build();
        userRepository.save(user);

        auditLogService.log(authentication, "DISPATCHER_CREATED", "User",
                request.getFullName() + " (" + request.getUsername() + ") dispetçer hesabı yaradıldı");

        return ResponseEntity.ok("Dispatcher hesabı yaradıldı");
    }

    @GetMapping("/dispatchers")
    public ResponseEntity<List<DispatcherAdminView>> getDispatchers() {
        List<DispatcherAdminView> result = userRepository.findByRole(Role.DISPATCHER).stream()
                .map(u -> DispatcherAdminView.builder()
                        .id(u.getId())
                        .fullName(u.getFullName())
                        .username(u.getUsername())
                        .enabled(u.getEnabled())
                        .build())
                .toList();
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/dispatchers/{id}")
    public ResponseEntity<?> deleteDispatcher(@PathVariable Long id, Authentication authentication) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dispatcher tapılmadı"));
        if (user.getRole() != Role.DISPATCHER) {
            return ResponseEntity.badRequest().body(Map.of("message", "Bu istifadəçi dispetçer deyil"));
        }
        try {
            userRepository.delete(user);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Bu dispetçer silinə bilmir — ona bağlı məlumatlar var"));
        }
        auditLogService.log(authentication, "DISPATCHER_DELETED", "User",
                user.getFullName() + " (" + user.getUsername() + ") dispetçer hesabı silindi");
        return ResponseEntity.ok(Map.of("message", "Dispetçer silindi"));
    }

    // Stage 10 — Admin "Sürücülər" list + password reset. Unlike the
    // self-service /api/profile/credentials endpoint, this doesn't require
    // the driver's current password — the admin's own ADMIN-role auth is
    // the authority check (see SecurityConfig: /api/admin/** hasRole(ADMIN)).
    @GetMapping("/drivers")
    public ResponseEntity<List<DriverAdminView>> getDrivers() {
        List<DriverAdminView> result = driverRepository.findAll().stream()
                .map(driver -> {
                    RatingService.RatingSummary ratingSummary = ratingService.getDriverSummary(driver.getId());
                    long deliveredCount = tripRepository.findByDriverIdAndStatus(driver.getId(), TripStatus.DELIVERED).size();
                    return DriverAdminView.builder()
                            .driverId(driver.getId())
                            .fullName(driver.getFullName())
                            .phone(driver.getPhone())
                            .licenseNumber(driver.getLicenseNumber())
                            .status(driver.getStatus())
                            .username(userRepository.findByDriverId(driver.getId())
                                    .map(User::getUsername)
                                    .orElse(null))
                            .averageRating(ratingSummary.average())
                            .ratingCount(ratingSummary.count())
                            .deliveredTripsCount(deliveredCount)
                            .build();
                })
                .toList();
        return ResponseEntity.ok(result);
    }

    // "Reytinqlər" səhifəsi — bütün sürücülər üzrə hansı reysdən nə qiymət/
    // şərh gəlib, ətraflı görmək üçün (bax RatingService#getAllRatingsDetailed).
    @GetMapping("/ratings")
    public ResponseEntity<List<RatingDetailResponse>> allRatings() {
        return ResponseEntity.ok(ratingService.getAllRatingsDetailed());
    }

    // AdminDrivers.jsx-də bir sürücünün "Ətraflı bax" düyməsi — həmin
    // sürücünün bütün qiymətləndirmələrini ayrıca sorğu ilə gətirir.
    @GetMapping("/ratings/driver/{driverId}")
    public ResponseEntity<List<RatingDetailResponse>> driverRatings(@PathVariable Long driverId) {
        return ResponseEntity.ok(ratingService.getDriverRatingsDetailed(driverId));
    }

    @PutMapping("/drivers/{driverId}/password")
    public ResponseEntity<?> resetDriverPassword(@PathVariable Long driverId, @RequestBody AdminSetPasswordRequest request, Authentication authentication) {
        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Yeni şifrə boş ola bilməz"));
        }

        User user = userRepository.findByDriverId(driverId)
                .orElseThrow(() -> new RuntimeException("Bu sürücüyə aid istifadəçi hesabı tapılmadı"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        auditLogService.log(authentication, "DRIVER_PASSWORD_RESET", "Driver",
                user.getUsername() + " istifadəçisinin şifrəsi sıfırlandı (sürücü ID: " + driverId + ")");

        return ResponseEntity.ok(Map.of("message", "Şifrə yeniləndi"));
    }

    // Sürücü şifrə sıfırlanması ilə eyni məntiq (bax yuxarıdakı
    // resetDriverPassword) — dispetçerin özü user sətridir (Driver kimi ayrı
    // entity yoxdur), ona görə birbaşa user id ilə işləyir. "Test datasını
    // sıfırla" (bax MaintenanceService) əməliyyatından sonra saxlanılan
    // dispetçer hesabının köhnə şifrəsi bilinməyəndə istifadə üçün lazımdır.
    @PutMapping("/dispatchers/{id}/password")
    public ResponseEntity<?> resetDispatcherPassword(@PathVariable Long id, @RequestBody AdminSetPasswordRequest request, Authentication authentication) {
        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Yeni şifrə boş ola bilməz"));
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dispetçer tapılmadı"));
        if (user.getRole() != Role.DISPATCHER) {
            return ResponseEntity.badRequest().body(Map.of("message", "Bu istifadəçi dispetçer deyil"));
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        auditLogService.log(authentication, "DISPATCHER_PASSWORD_RESET", "User",
                user.getUsername() + " istifadəçisinin şifrəsi sıfırlandı");

        return ResponseEntity.ok(Map.of("message", "Şifrə yeniləndi"));
    }

    // Sürücü şifrə sıfırlanması ilə eyni məntiq, müştəri üçün (bax
    // resetDriverPassword) — customerId ilə əlaqəli user sətri tapılır.
    @PutMapping("/customers/{customerId}/password")
    public ResponseEntity<?> resetCustomerPassword(@PathVariable Long customerId, @RequestBody AdminSetPasswordRequest request, Authentication authentication) {
        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Yeni şifrə boş ola bilməz"));
        }

        User user = userRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Bu müştəriyə aid istifadəçi hesabı tapılmadı"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        auditLogService.log(authentication, "CUSTOMER_PASSWORD_RESET", "Customer",
                user.getUsername() + " istifadəçisinin şifrəsi sıfırlandı (müştəri ID: " + customerId + ")");

        return ResponseEntity.ok(Map.of("message", "Şifrə yeniləndi"));
    }

    // Trip.driver (FK) hər zaman keçmiş DELIVERED reysləri də özündə saxlayır
    // — sürücünün heç vaxt bir dəfə də olsa reysi olmuşsa, əvvəlki versiya
    // (sərt delete) həmişə FK xətası ilə rədd edilirdi, admin sürücünü heç
    // vaxt silə bilmirdi. İndi fərq qoyuruq:
    //   - Hazırda AKTİV (DELIVERED olmayan) reysi varsa -> silinmir, çünki
    //     bu davam edən çatdırılmanı "sürücüsüz" qoyardı.
    //   - Yalnız tamamlanmış (DELIVERED) reysləri varsa -> həmin reyslərin
    //     driver əlaqəsini boşaldırıq (tarixçə/Cargo/TrackingLog qalır),
    //     sonra sürücünü və login hesabını silirik.
    @DeleteMapping("/drivers/{driverId}")
    public ResponseEntity<?> deleteDriver(@PathVariable Long driverId, Authentication authentication) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Sürücü tapılmadı"));

        List<Trip> trips = tripRepository.findByDriverId(driverId);
        // DELIVERED (bitmiş), REJECTED (sürücünün imtina etdiyi) və CANCELLED
        // (dispetçerin ləğv etdiyi) reyslər — hamısı artıq "ölü uc" — silinməyə
        // mane olmamalıdır.
        boolean hasActiveTrip = trips.stream()
                .anyMatch(t -> t.getStatus() != TripStatus.DELIVERED
                        && t.getStatus() != TripStatus.REJECTED
                        && t.getStatus() != TripStatus.CANCELLED);
        if (hasActiveTrip) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Bu sürücünün hazırda davam edən reysi var — əvvəlcə onu başqa sürücüyə keçirin və ya reysi tamamlayın"));
        }

        trips.forEach(t -> t.setDriver(null));
        tripRepository.saveAll(trips);

        // Sürücü təsdiqləndikdə ona öz tırı avtomatik təhkim olunur
        // (Vehicle.driver, @OneToOne) — bu əlaqəni də boşaltmasaq, Hibernate
        // flush zamanı "unsaved transient instance of Driver" xətası verir,
        // çünki Vehicle hələ də silinən Driver-ə istinad edir. Tırın özü
        // qalır, sadəcə sürücü təhkimatı ləğv olunur.
        vehicleRepository.findByDriverId(driverId).ifPresent(v -> {
            v.setDriver(null);
            vehicleRepository.save(v);
        });

        try {
            driverRepository.delete(driver);
            driverRepository.flush();
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Bu sürücü silinə bilmir — ona bağlı məlumatlar var"));
        }
        userRepository.findByDriverId(driverId).ifPresent(userRepository::delete);
        auditLogService.log(authentication, "DRIVER_DELETED", "Driver",
                driver.getFullName() + " (ID: " + driverId + ") sürücüsü silindi");
        return ResponseEntity.ok(Map.of("message", "Sürücü silindi"));
    }

    @GetMapping("/warehouses")
    public ResponseEntity<List<Warehouse>> getWarehouses() {
        return ResponseEntity.ok(warehouseRepository.findAll());
    }

    @PostMapping("/warehouses")
    public ResponseEntity<Warehouse> createWarehouse(@RequestBody WarehouseRequest request, Authentication authentication) {
        Warehouse warehouse = Warehouse.builder()
                .name(request.getName())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();
        Warehouse saved = warehouseRepository.save(warehouse);
        auditLogService.log(authentication, "WAREHOUSE_CREATED", "Warehouse", saved.getName() + " anbarı yaradıldı");
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/warehouses/{id}")
    public ResponseEntity<Warehouse> updateWarehouse(@PathVariable Long id, @RequestBody WarehouseRequest request, Authentication authentication) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Anbar tapılmadı"));
        warehouse.setName(request.getName());
        warehouse.setAddress(request.getAddress());
        warehouse.setLatitude(request.getLatitude());
        warehouse.setLongitude(request.getLongitude());
        Warehouse saved = warehouseRepository.save(warehouse);
        auditLogService.log(authentication, "WAREHOUSE_UPDATED", "Warehouse", saved.getName() + " anbarı yeniləndi");
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/warehouses/{id}")
    public ResponseEntity<?> deleteWarehouse(@PathVariable Long id, Authentication authentication) {
        Warehouse warehouse = warehouseRepository.findById(id).orElse(null);
        try {
            warehouseRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            // Cargo.originWarehouse (FK) hələ bu anbara istinad edir —
            // əvvəllər bu sükutla uğursuz olur, admin heç nə görmürdü.
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Bu anbar silinə bilmir — ona bağlı yüklər var"));
        }
        auditLogService.log(authentication, "WAREHOUSE_DELETED", "Warehouse",
                (warehouse != null ? warehouse.getName() : ("ID: " + id)) + " anbarı silindi");
        return ResponseEntity.ok(Map.of("message", "Anbar silindi"));
    }

    @GetMapping("/vehicles")
    public ResponseEntity<List<Vehicle>> getVehicles() {
        return ResponseEntity.ok(vehicleRepository.findAll());
    }

    // ownerType boş buraxılsa COMPANY qəbul edilir (bax Vehicle.prePersist).
    // DRIVER_OWNED seçilibsə driverId mütləqdir — məs. artıq aktiv olan bir
    // sürücü sonradan öz tırını əldə edibsə, admin bunu əl ilə qeyd edə bilər
    // (adətən DRIVER_OWNED tırlar iş müraciəti təsdiqi zamanı avtomatik
    // yaranır — bax AdminApplicationService#approve).
    @PostMapping("/vehicles")
    public ResponseEntity<?> createVehicle(@RequestBody VehicleRequest request, Authentication authentication) {
        Driver owner = null;
        if (request.getOwnerType() == OwnerType.DRIVER_OWNED) {
            if (request.getDriverId() == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Sürücüyə məxsus tır üçün sürücü seçilməlidir"));
            }
            owner = driverRepository.findById(request.getDriverId())
                    .orElseThrow(() -> new RuntimeException("Sürücü tapılmadı"));
            if (vehicleRepository.findByDriverId(owner.getId()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Bu sürücünün artıq öz tırı var"));
            }
        }
        Vehicle vehicle = Vehicle.builder()
                .plateNumber(request.getPlateNumber())
                .brand(request.getBrand())
                .fuelConsumption(request.getFuelConsumption())
                .transportMode(request.getTransportMode())
                .ownerType(request.getOwnerType())
                .driver(owner)
                .build();
        Vehicle saved = vehicleRepository.save(vehicle);
        auditLogService.log(authentication, "VEHICLE_CREATED", "Vehicle", saved.getPlateNumber() + " nömrəli tır yaradıldı");
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/trailers")
    public ResponseEntity<List<Trailer>> getTrailers() {
        return ResponseEntity.ok(trailerRepository.findAll());
    }

    @PostMapping("/trailers")
    public ResponseEntity<?> createTrailer(@RequestBody TrailerRequest request, Authentication authentication) {
        Driver owner = null;
        if (request.getOwnerType() == OwnerType.DRIVER_OWNED) {
            if (request.getDriverId() == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Sürücüyə məxsus qoşqu üçün sürücü seçilməlidir"));
            }
            owner = driverRepository.findById(request.getDriverId())
                    .orElseThrow(() -> new RuntimeException("Sürücü tapılmadı"));
            if (trailerRepository.findByDriverId(owner.getId()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Bu sürücünün artıq öz qoşqusu var"));
            }
        }
        Trailer trailer = Trailer.builder()
                .plateNumber(request.getPlateNumber())
                .capacity(request.getCapacity())
                .ownerType(request.getOwnerType())
                .driver(owner)
                .build();
        Trailer saved = trailerRepository.save(trailer);
        auditLogService.log(authentication, "TRAILER_CREATED", "Trailer", saved.getPlateNumber() + " nömrəli qoşqu yaradıldı");
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/trailers/{id}")
    public ResponseEntity<?> deleteTrailer(@PathVariable Long id, Authentication authentication) {
        Trailer trailer = trailerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Qoşqu tapılmadı"));

        List<Trip> trips = tripRepository.findByTrailerId(id);
        // DELIVERED (bitmiş), REJECTED (sürücünün imtina etdiyi) və CANCELLED
        // (dispetçerin ləğv etdiyi) reyslər — hamısı artıq "ölü uc" — silinməyə
        // mane olmamalıdır.
        boolean hasActiveTrip = trips.stream()
                .anyMatch(t -> t.getStatus() != TripStatus.DELIVERED
                        && t.getStatus() != TripStatus.REJECTED
                        && t.getStatus() != TripStatus.CANCELLED);
        if (hasActiveTrip) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Bu qoşqunun hazırda davam edən reysi var — əvvəlcə onu başqa qoşquya keçirin və ya reysi tamamlayın"));
        }

        trips.forEach(t -> t.setTrailer(null));
        tripRepository.saveAll(trips);

        try {
            trailerRepository.delete(trailer);
            trailerRepository.flush();
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Bu qoşqu silinə bilmir — ona bağlı məlumatlar var"));
        }
        auditLogService.log(authentication, "TRAILER_DELETED", "Trailer", trailer.getPlateNumber() + " nömrəli qoşqu silindi");
        return ResponseEntity.ok(Map.of("message", "Qoşqu silindi"));
    }

    // Sürücü silmədə olduğu kimi eyni səbəb: Trip.vehicle (FK) tırın bir
    // dəfə də olsa reysi olubsa silinməsinə mane olurdu — indi yalnız
    // HAZIRDA davam edən reys varsa bloklanır, tamamlanmış reyslərin tır
    // əlaqəsi boşaldılıb (tarixçə qalır) sonra tır silinir.
    @DeleteMapping("/vehicles/{id}")
    public ResponseEntity<?> deleteVehicle(@PathVariable Long id, Authentication authentication) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tır tapılmadı"));

        List<Trip> trips = tripRepository.findByVehicleId(id);
        // DELIVERED (bitmiş), REJECTED (sürücünün imtina etdiyi) və CANCELLED
        // (dispetçerin ləğv etdiyi) reyslər — hamısı artıq "ölü uc" — silinməyə
        // mane olmamalıdır.
        boolean hasActiveTrip = trips.stream()
                .anyMatch(t -> t.getStatus() != TripStatus.DELIVERED
                        && t.getStatus() != TripStatus.REJECTED
                        && t.getStatus() != TripStatus.CANCELLED);
        if (hasActiveTrip) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Bu tırın hazırda davam edən reysi var — əvvəlcə onu başqa tıra keçirin və ya reysi tamamlayın"));
        }

        trips.forEach(t -> t.setVehicle(null));
        tripRepository.saveAll(trips);

        try {
            vehicleRepository.delete(vehicle);
            vehicleRepository.flush();
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Bu tır silinə bilmir — ona bağlı məlumatlar var"));
        }
        auditLogService.log(authentication, "VEHICLE_DELETED", "Vehicle", vehicle.getPlateNumber() + " nömrəli tır silindi");
        return ResponseEntity.ok(Map.of("message", "Tır silindi"));
    }

    // Beynəlxalq göndərişlər üçün gömrük rüsumu/ƏDV tarif cədvəli —
    // CustomsDutyService bu cədvələ görə hesablayır (bax service paketi).
    // Mal növü (CargoType) üzrə unikal olduğu üçün eyni növ üçün yeni sətir
    // göndərilsə köhnəsi yenilənir (upsert).
    @GetMapping("/customs-tariffs")
    public ResponseEntity<List<CustomsTariff>> getCustomsTariffs() {
        return ResponseEntity.ok(customsTariffRepository.findAll());
    }

    @PostMapping("/customs-tariffs")
    public ResponseEntity<CustomsTariff> upsertCustomsTariff(@RequestBody CustomsTariffRequest request, Authentication authentication) {
        if (request.getCargoType() == null) {
            throw new RuntimeException("Mal növü seçilməlidir");
        }
        CustomsTariff tariff = customsTariffRepository.findByCargoType(request.getCargoType())
                .orElse(CustomsTariff.builder().cargoType(request.getCargoType()).build());
        tariff.setDutyRatePercent(request.getDutyRatePercent());
        tariff.setVatRatePercent(request.getVatRatePercent());
        tariff.setDescription(request.getDescription());
        CustomsTariff saved = customsTariffRepository.save(tariff);
        auditLogService.log(authentication, "CUSTOMS_TARIFF_UPDATED", "CustomsTariff",
                saved.getCargoType() + " üçün tarif yeniləndi (rüsum " + saved.getDutyRatePercent() + "%, ƏDV " + saved.getVatRatePercent() + "%)");
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/customs-tariffs/{id}")
    public ResponseEntity<?> deleteCustomsTariff(@PathVariable Long id, Authentication authentication) {
        CustomsTariff tariff = customsTariffRepository.findById(id).orElse(null);
        customsTariffRepository.deleteById(id);
        auditLogService.log(authentication, "CUSTOMS_TARIFF_DELETED", "CustomsTariff",
                (tariff != null ? tariff.getCargoType().toString() : ("ID: " + id)) + " tarifi silindi");
        return ResponseEntity.ok(Map.of("message", "Tarif silindi"));
    }
}