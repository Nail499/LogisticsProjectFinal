package com.ltc.logisticsproject.repository;

import com.ltc.logisticsproject.entity.Trip;
import com.ltc.logisticsproject.entity.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findByDriverIdAndStatusNot(Long driverId, TripStatus status);
    List<Trip> findByDriverIdAndStatus(Long driverId, TripStatus status);
    // Reys qəbul/imtina: "aktiv" (PLANNED/PICKED_UP/IN_TRANSIT) reysləri
    // PENDING_ACCEPTANCE/REJECTED-dən ayırmaq üçün (bax
    // DriverController#currentTrips, currentTripsLive).
    List<Trip> findByDriverIdAndStatusIn(Long driverId, List<TripStatus> statuses);
    // Used by AdminManagementController#deleteDriver — needs every trip
    // (any status) to decide whether the driver can be safely removed.
    List<Trip> findByDriverId(Long driverId);
    // Same idea for AdminManagementController#deleteVehicle.
    List<Trip> findByVehicleId(Long vehicleId);
    // Same idea for AdminManagementController#deleteTrailer.
    List<Trip> findByTrailerId(Long trailerId);
    // Qoşqu hovuzu görünürlüyü (Trailer Pool) — bu qoşqu HAZIRDA hansısa
    // reysə bağlıdırmı (bax DispatcherController#trailerPool). Terminal
    // olmayan statuslar veriləndə boş siyahı = qoşqu bazada/boşdadır.
    List<Trip> findByTrailerIdAndStatusIn(Long trailerId, List<TripStatus> statuses);
}
