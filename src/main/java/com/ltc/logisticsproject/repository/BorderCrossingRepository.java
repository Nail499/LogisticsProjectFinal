package com.ltc.logisticsproject.repository;

import com.ltc.logisticsproject.entity.BorderCrossing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BorderCrossingRepository extends JpaRepository<BorderCrossing, Long> {
    List<BorderCrossing> findByTripIdOrderByCrossedAtAsc(Long tripId);
}
