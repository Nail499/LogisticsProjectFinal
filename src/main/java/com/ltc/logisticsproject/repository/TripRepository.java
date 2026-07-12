package com.ltc.logisticsproject.repository;

import com.ltc.logisticsproject.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripRepository extends JpaRepository<Trip, Long> {
}
