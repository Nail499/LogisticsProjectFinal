package com.ltc.logisticsproject.repository;

import com.ltc.logisticsproject.entity.Cargo;
import com.ltc.logisticsproject.entity.CargoStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CargoRepository extends JpaRepository<Cargo, Long> {
    Optional<Cargo> findByTrackingNumber(String trackingNumber);
    List<Cargo> findByCustomerId(Long customerId);
    List<Cargo> findByStatus(CargoStatus status);
    List<Cargo> findByTripId(Long tripId);
}
