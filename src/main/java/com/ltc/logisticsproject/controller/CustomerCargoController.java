package com.ltc.logisticsproject.controller;

import com.ltc.logisticsproject.dto.CargoRequest;
import com.ltc.logisticsproject.entity.*;
import com.ltc.logisticsproject.repository.*;
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
    final WareHouseRepository warehouseRepository;
    final UserRepository userRepository;
    final CustomerRepository customerRepository;

    @PostMapping
    public ResponseEntity<Cargo> create(@RequestBody CargoRequest request, Authentication authentication) {
        Customer customer = currentCustomer(authentication);

        WareHouse warehouse = null;
        if (request.getOriginWarehouseId() != null) {
            warehouse = warehouseRepository.findById(request.getOriginWarehouseId())
                    .orElseThrow(() -> new RuntimeException("Anbar tapılmadı"));
        }

        Cargo cargo = Cargo.builder()
                .description(request.getDescription())
                .weight(request.getWeight())
                .volume(request.getVolume())
                .originWarehouse(warehouse)
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
                .build();

        cargo = cargoRepository.save(cargo);
        return ResponseEntity.ok(cargo);
    }

    @GetMapping
    public ResponseEntity<List<Cargo>> myOrders(Authentication authentication) {
        Customer customer = currentCustomer(authentication);
        return ResponseEntity.ok(cargoRepository.findByCustomerId(customer.getId()));
    }

    private Customer currentCustomer(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("İstifadəçi tapılmadı"));
        return customerRepository.findById(user.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Müştəri tapılmadı"));
    }
}