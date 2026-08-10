package com.ltc.logisticsproject.repository;

import com.ltc.logisticsproject.entity.Trailer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrailerRepository extends JpaRepository<Trailer, Long> {
    Optional<Trailer> findByDriverId(Long driverId);
}
