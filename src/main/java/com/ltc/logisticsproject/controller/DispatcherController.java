package com.ltc.logisticsproject.controller;

import com.ltc.logisticsproject.dto.TripRequest;
import com.ltc.logisticsproject.entity.*;
import com.ltc.logisticsproject.repository.*;
import com.ltc.logisticsproject.service.DispatcherService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dispatcher")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DispatcherController {

    final CargoRepository cargoRepository;
    final DriverRepository driverRepository;
    final DispatcherService dispatcherService;

    @GetMapping("/cargo/pending")
    public ResponseEntity<List<Cargo>> pendingCargo() {
        return ResponseEntity.ok(cargoRepository.findByStatus(CargoStatus.PENDING));
    }

    @GetMapping("/drivers/available")
    public ResponseEntity<List<Driver>> availableDrivers() {
        return ResponseEntity.ok(driverRepository.findByStatus(DriverStatus.ACTIVE));
    }

    @PostMapping("/trips")
    public ResponseEntity<Trip> createTrip(@RequestBody TripRequest request) {
        return ResponseEntity.ok(dispatcherService.createTrip(request));
    }
}