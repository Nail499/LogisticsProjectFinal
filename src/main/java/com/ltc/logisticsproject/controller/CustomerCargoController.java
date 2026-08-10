package com.ltc.logisticsproject.controller;

import com.ltc.logisticsproject.dto.CargoRequest;
import com.ltc.logisticsproject.dto.customs.CustomsEstimateRequest;
import com.ltc.logisticsproject.entity.*;
import com.ltc.logisticsproject.repository.*;
import com.ltc.logisticsproject.service.CustomsDutyService;
import com.ltc.logisticsproject.service.NotificationService;
import com.ltc.logisticsproject.service.PricingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer/cargo")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerCargoController {

    final CargoRepository cargoRepository;
    final WarehouseRepository warehouseRepository;
    final UserRepository userRepository;
    final CustomerRepository customerRepository;
    final CustomsDutyService customsDutyService;
    final NotificationService notificationService;
    final PricingService pricingService;

    @PostMapping
    public ResponseEntity<Cargo> create(@RequestBody CargoRequest request, Authentication authentication) {
        Customer customer = currentCustomer(authentication);

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
                .customer(customer)
                .customerName(customer.getFullName())
                .customerPhone(customer.getPhone())
                .status(CargoStatus.PENDING)
                .requiresCustoms(request.isRequiresCustoms())
                .preferredTransportMode(request.getPreferredTransportMode())
                .incoterm(request.getIncoterm())
                .originCountry(request.getOriginCountry())
                .destinationCountry(request.getDestinationCountry())
                .transitCountries(request.getTransitCountries())
                .build();

        cargo.setPrice(pricingService.calculatePrice(cargo));
        final Cargo savedCargo = cargoRepository.save(cargo);

        // "final Cargo savedCargo" qəsdən ayrıca dəyişən — lambda daxilində
        // yalnız effectively-final dəyişənlərə istinad oluna bilər, "cargo"
        // isə save()-dən sonra yenidən mənimsədildiyi üçün buna uyğun deyil.
        userRepository.findByCustomerId(customer.getId()).ifPresent(user ->
                notificationService.notifyWithEmail(
                        user.getId(), customer.getEmail(), NotificationType.ORDER_CREATED,
                        "Sifarişiniz qəbul edildi",
                        savedCargo.getTrackingNumber() + " nömrəli göndəriş sifarişiniz qeydə alındı. Dispetçer tezliklə sürücü təyin edəcək.",
                        "/customer/orders", "Fleetra — sifarişiniz qəbul edildi", "Sifarişə bax"
                )
        );

        // Dispetçer/admin komandası "Gözləyən yüklər" siyahısına yeni işin
        // düşdüyünü zəng ikonundan dərhal görsün (bax NotificationService#notifyDispatchers).
        notificationService.notifyDispatchers(
                "Yeni sifariş daxil oldu",
                savedCargo.getTrackingNumber() + " nömrəli yeni sifariş (" + customer.getFullName() + ") gözləyən yüklər siyahısına düşdü.",
                "/dispatcher/queue"
        );

        return ResponseEntity.ok(savedCargo);
    }

    @GetMapping
    public ResponseEntity<List<Cargo>> myOrders(Authentication authentication) {
        Customer customer = currentCustomer(authentication);
        return ResponseEntity.ok(cargoRepository.findByCustomerId(customer.getId()));
    }

    @GetMapping("/warehouses")
    public ResponseEntity<List<Warehouse>> warehouses() {
        return ResponseEntity.ok(warehouseRepository.findAll());
    }

    // Gömrük kalkulyatoru — müştəri hələ sifariş yaratmadan, sadəcə mal
    // növü və dəyərə görə təxmini rüsum/ƏDV/ödəniləcək məbləği görmək
    // istəyəndə istifadə olunur. Heç bir Cargo/CustomsDeclaration yaratmır,
    // yalnız CustomsDutyService-in eyni real hesablama məntiqini işlədir
    // (bax dispetçerin gömrük bəyannaməsi paneli — eyni tarif cədvəlini
    // istifadə edir, ona görə nəticələr üst-üstə düşür).
    @PostMapping("/customs-estimate")
    public ResponseEntity<CustomsDutyService.DutyCalculation> estimateCustoms(@RequestBody CustomsEstimateRequest request) {
        double declaredValue = request.getDeclaredValue() != null ? request.getDeclaredValue() : 0.0;
        return ResponseEntity.ok(customsDutyService.calculate(request.getCargoType(), declaredValue));
    }

    private Customer currentCustomer(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("İstifadəçi tapılmadı"));
        return customerRepository.findById(user.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Müştəri tapılmadı"));
    }
}