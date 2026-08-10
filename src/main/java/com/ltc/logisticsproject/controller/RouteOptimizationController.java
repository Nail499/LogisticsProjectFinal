package com.ltc.logisticsproject.controller;

import com.ltc.logisticsproject.dto.OptimizedRouteResponse;
import com.ltc.logisticsproject.dto.RouteOptimizeRequest;
import com.ltc.logisticsproject.dto.RouteStop;
import com.ltc.logisticsproject.entity.Cargo;
import com.ltc.logisticsproject.repository.CargoRepository;
import com.ltc.logisticsproject.service.RouteOptimizationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Dispetçerin "Gözləyən yüklər" ekranında bir neçə yükü eyni reysə
// birləşdirərkən (bax CargoQueue.jsx) təhvil ünvanlarının ən qısa gəzinti
// sırasını görməsi üçün (bax RouteOptimizationService). "/api/dispatcher/**"
// SecurityConfig-də artıq hasAnyRole("DISPATCHER","ADMIN") ilə qorunur.
@RestController
@RequestMapping("/api/dispatcher/route")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RouteOptimizationController {

    final RouteOptimizationService routeOptimizationService;
    final CargoRepository cargoRepository;

    @PostMapping("/optimize")
    public ResponseEntity<?> optimize(@RequestBody RouteOptimizeRequest request) {
        if (request.getCargoIds() == null || request.getCargoIds().isEmpty()) {
            throw new RuntimeException("Ən azı bir yük seçilməlidir");
        }

        List<Cargo> cargos = cargoRepository.findAllById(request.getCargoIds());
        List<Cargo> usable = cargos.stream()
                .filter(c -> c.getDestinationLatitude() != null && c.getDestinationLongitude() != null)
                .toList();

        if (usable.isEmpty()) {
            throw new RuntimeException("Seçilmiş yüklərin heç birində təhvil koordinatı yoxdur");
        }

        // Başlanğıc nöqtə: koordinatı olan ilk yükün götürülmə ünvanı — birgə
        // daşınan yüklər adətən eyni anbardan/ünvandan götürülür, ona görə
        // sadəcə ilk mövcud pickup nöqtəsi kifayət qədər dəqiq başlanğıcdır.
        Cargo withPickup = usable.stream()
                .filter(c -> c.getPickupLatitude() != null && c.getPickupLongitude() != null)
                .findFirst().orElse(usable.get(0));

        RouteStop start = RouteStop.builder()
                .label("Yükləmə nöqtəsi")
                .address(withPickup.getPickupAddress())
                .lat(withPickup.getPickupLatitude())
                .lng(withPickup.getPickupLongitude())
                .build();

        List<RouteStop> destinations = usable.stream()
                .map(c -> RouteStop.builder()
                        .cargoId(c.getId())
                        .label(c.getTrackingNumber())
                        .address(c.getDestinationAddress())
                        .lat(c.getDestinationLatitude())
                        .lng(c.getDestinationLongitude())
                        .build())
                .toList();

        OptimizedRouteResponse result = routeOptimizationService.optimize(start, destinations);
        return ResponseEntity.ok(result);
    }
}
