package com.ltc.logisticsproject.controller;

import com.ltc.logisticsproject.dto.ExpenseRequest;
import com.ltc.logisticsproject.dto.LocationRequest;
import com.ltc.logisticsproject.dto.TripStatusUpdateRequest;
import com.ltc.logisticsproject.entity.*;
import com.ltc.logisticsproject.repository.*;
import com.ltc.logisticsproject.service.DriverTripService;
import com.ltc.logisticsproject.service.ExpenseService;
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
    final TrackingLogRepository trackingLogRepository;
    final ExpenseService expenseService;

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

        return ResponseEntity.ok(trackingLogRepository.save(log));
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
}