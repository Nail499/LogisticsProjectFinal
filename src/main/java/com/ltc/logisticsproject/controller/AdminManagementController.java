package com.ltc.logisticsproject.controller;

import com.ltc.logisticsproject.dto.AdminSetPasswordRequest;
import com.ltc.logisticsproject.dto.CustomsTariffRequest;
import com.ltc.logisticsproject.dto.DispatcherAdminView;
import com.ltc.logisticsproject.dto.DispatcherRequest;
import com.ltc.logisticsproject.dto.DriverAdminView;
import com.ltc.logisticsproject.dto.VehicleRequest;
import com.ltc.logisticsproject.dto.WarehouseRequest;
import com.ltc.logisticsproject.entity.*;
import com.ltc.logisticsproject.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    final DriverRepository driverRepository;
    final TripRepository tripRepository;
    final PasswordEncoder passwordEncoder;
    final CustomsTariffRepository customsTariffRepository;

    @PostMapping("/dispatchers")
    public ResponseEntity<?> createDispatcher(@RequestBody DispatcherRequest request) {
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
    public ResponseEntity<?> deleteDispatcher(@PathVariable Long id) {
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
        return ResponseEntity.ok(Map.of("message", "Dispetçer silindi"));
    }

    // Stage 10 — Admin "Sürücülər" list + password reset. Unlike the
    // self-service /api/profile/credentials endpoint, this doesn't require
    // the driver's current password — the admin's own ADMIN-role auth is
    // the authority check (see SecurityConfig: /api/admin/** hasRole(ADMIN)).
    @GetMapping("/drivers")
    public ResponseEntity<List<DriverAdminView>> getDrivers() {
        List<DriverAdminView> result = driverRepository.findAll().stream()
                .map(driver -> DriverAdminView.builder()
                        .driverId(driver.getId())
                        .fullName(driver.getFullName())
                        .phone(driver.getPhone())
                        .licenseNumber(driver.getLicenseNumber())
                        .status(driver.getStatus())
                        .username(userRepository.findByDriverId(driver.getId())
                                .map(User::getUsername)
                                .orElse(null))
                        .build())
                .toList();
        return ResponseEntity.ok(result);
    }

    @PutMapping("/drivers/{driverId}/password")
    public ResponseEntity<?> resetDriverPassword(@PathVariable Long driverId, @RequestBody AdminSetPasswordRequest request) {
        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Yeni şifrə boş ola bilməz"));
        }

        User user = userRepository.findByDriverId(driverId)
                .orElseThrow(() -> new RuntimeException("Bu sürücüyə aid istifadəçi hesabı tapılmadı"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

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
    public ResponseEntity<?> deleteDriver(@PathVariable Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Sürücü tapılmadı"));

        List<Trip> trips = tripRepository.findByDriverId(driverId);
        boolean hasActiveTrip = trips.stream().anyMatch(t -> t.getStatus() != TripStatus.DELIVERED);
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
        return ResponseEntity.ok(Map.of("message", "Sürücü silindi"));
    }

    @GetMapping("/warehouses")
    public ResponseEntity<List<Warehouse>> getWarehouses() {
        return ResponseEntity.ok(warehouseRepository.findAll());
    }

    @PostMapping("/warehouses")
    public ResponseEntity<Warehouse> createWarehouse(@RequestBody WarehouseRequest request) {
        Warehouse warehouse = Warehouse.builder()
                .name(request.getName())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();
        return ResponseEntity.ok(warehouseRepository.save(warehouse));
    }

    @PutMapping("/warehouses/{id}")
    public ResponseEntity<Warehouse> updateWarehouse(@PathVariable Long id, @RequestBody WarehouseRequest request) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Anbar tapılmadı"));
        warehouse.setName(request.getName());
        warehouse.setAddress(request.getAddress());
        warehouse.setLatitude(request.getLatitude());
        warehouse.setLongitude(request.getLongitude());
        return ResponseEntity.ok(warehouseRepository.save(warehouse));
    }

    @DeleteMapping("/warehouses/{id}")
    public ResponseEntity<?> deleteWarehouse(@PathVariable Long id) {
        try {
            warehouseRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            // Cargo.originWarehouse (FK) hələ bu anbara istinad edir —
            // əvvəllər bu sükutla uğursuz olur, admin heç nə görmürdü.
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Bu anbar silinə bilmir — ona bağlı yüklər var"));
        }
        return ResponseEntity.ok(Map.of("message", "Anbar silindi"));
    }

    @GetMapping("/vehicles")
    public ResponseEntity<List<Vehicle>> getVehicles() {
        return ResponseEntity.ok(vehicleRepository.findAll());
    }

    @PostMapping("/vehicles")
    public ResponseEntity<Vehicle> createVehicle(@RequestBody VehicleRequest request) {
        Vehicle vehicle = Vehicle.builder()
                .plateNumber(request.getPlateNumber())
                .brand(request.getBrand())
                .capacity(request.getCapacity())
                .fuelConsumption(request.getFuelConsumption())
                .transportMode(request.getTransportMode())
                .build();
        return ResponseEntity.ok(vehicleRepository.save(vehicle));
    }

    // Sürücü silmədə olduğu kimi eyni səbəb: Trip.vehicle (FK) tırın bir
    // dəfə də olsa reysi olubsa silinməsinə mane olurdu — indi yalnız
    // HAZIRDA davam edən reys varsa bloklanır, tamamlanmış reyslərin tır
    // əlaqəsi boşaldılıb (tarixçə qalır) sonra tır silinir.
    @DeleteMapping("/vehicles/{id}")
    public ResponseEntity<?> deleteVehicle(@PathVariable Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tır tapılmadı"));

        List<Trip> trips = tripRepository.findByVehicleId(id);
        boolean hasActiveTrip = trips.stream().anyMatch(t -> t.getStatus() != TripStatus.DELIVERED);
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
    public ResponseEntity<CustomsTariff> upsertCustomsTariff(@RequestBody CustomsTariffRequest request) {
        if (request.getCargoType() == null) {
            throw new RuntimeException("Mal növü seçilməlidir");
        }
        CustomsTariff tariff = customsTariffRepository.findByCargoType(request.getCargoType())
                .orElse(CustomsTariff.builder().cargoType(request.getCargoType()).build());
        tariff.setDutyRatePercent(request.getDutyRatePercent());
        tariff.setVatRatePercent(request.getVatRatePercent());
        tariff.setDescription(request.getDescription());
        return ResponseEntity.ok(customsTariffRepository.save(tariff));
    }

    @DeleteMapping("/customs-tariffs/{id}")
    public ResponseEntity<?> deleteCustomsTariff(@PathVariable Long id) {
        customsTariffRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Tarif silindi"));
    }
}