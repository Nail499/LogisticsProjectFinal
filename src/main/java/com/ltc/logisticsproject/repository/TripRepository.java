package com.ltc.logisticsproject.repository;

import com.ltc.logisticsproject.entity.Trip;
import com.ltc.logisticsproject.entity.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findByDriverIdAndStatusNot(Long driverId, TripStatus status);
    List<Trip> findByDriverIdAndStatus(Long driverId, TripStatus status);
}
