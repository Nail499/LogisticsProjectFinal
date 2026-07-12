package com.ltc.logisticsproject.repository;

import com.ltc.logisticsproject.entity.Cargo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CargoRepository extends JpaRepository<Cargo, Long> {
    Optional<Cargo> findByTrackingNumber(String trackingNumber);
}
