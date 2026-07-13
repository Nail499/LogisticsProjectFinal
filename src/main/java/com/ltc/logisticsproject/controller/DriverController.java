package com.ltc.logisticsproject.controller;

import com.ltc.logisticsproject.dto.TripStatusUpdateRequest;
import com.ltc.logisticsproject.entity.*;
import com.ltc.logisticsproject.repository.*;
import com.ltc.logisticsproject.service.DriverTripService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/driver")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DriverController {

    final TripRepository tripRepository;
    final UserRepository userRepository;
    final DriverTripService driverTripService;

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

    @PostMapping("/trips/{id}/status")
    public ResponseEntity<Trip> updateStatus(@PathVariable Long id,
                                             @RequestBody TripStatusUpdateRequest request,
                                             Authentication authentication) {
        Long driverId = currentDriverId(authentication);
        return ResponseEntity.ok(driverTripService.updateStatus(id, driverId, request.getStatus()));
    }

    private Long currentDriverId(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("İstifadəçi tapılmadı"));
        if (user.getDriverId() == null) {
            throw new RuntimeException("Bu istifadəçi sürücü deyil");
        }
        return user.getDriverId();
    }
}