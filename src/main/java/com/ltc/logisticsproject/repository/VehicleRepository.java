package com.ltc.logisticsproject.repository;

import com.ltc.logisticsproject.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Optional<Vehicle> findByDriverId(Long driverId);
}
