package com.ltc.logisticsproject.service;

import com.ltc.logisticsproject.dto.TripRequest;
import com.ltc.logisticsproject.entity.*;
import com.ltc.logisticsproject.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DispatcherService {

    final TripRepository tripRepository;
    final DriverRepository driverRepository;
    final VehicleRepository vehicleRepository;
    final CargoRepository cargoRepository;

    @Transactional
    public Trip createTrip(TripRequest request) {
        Driver driver = driverRepository.findById(request.getDriverId())
                .orElseThrow(() -> new RuntimeException("Sürücü tapılmadı"));

        if (driver.getStatus() != DriverStatus.ACTIVE) {
            throw new RuntimeException("Sürücü aktiv deyil");
        }

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new RuntimeException("Tır tapılmadı"));

        List<Cargo> cargos = cargoRepository.findAllById(request.getCargoIds());

        if (cargos.isEmpty()) {
            throw new RuntimeException("Ən azı bir yük seçilməlidir");
        }

        for (Cargo cargo : cargos) {
            if (cargo.getStatus() != CargoStatus.PENDING) {
                throw new RuntimeException("Cargo #" + cargo.getId() + " artıq təhkim olunub");
            }
        }

        Trip trip = Trip.builder()
                .driver(driver)
                .vehicle(vehicle)
                .status(TripStatus.PLANNED)
                .estimatedDistanceKm(request.getEstimatedDistanceKm())
                .estimatedCost(request.getEstimatedCost())
                .routeInfo(request.getRouteInfo())
                .build();
        trip = tripRepository.save(trip);

        for (Cargo cargo : cargos) {
            cargo.setTrip(trip);
            cargo.setStatus(CargoStatus.ASSIGNED);
            cargoRepository.save(cargo);
        }

        return trip;
    }
}