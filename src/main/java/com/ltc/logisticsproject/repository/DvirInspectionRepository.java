package com.ltc.logisticsproject.repository;

import com.ltc.logisticsproject.entity.DvirInspection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DvirInspectionRepository extends JpaRepository<DvirInspection, Long> {
    // Sürücü panelində "bu reys üçün öncəki/sonrakı yoxlama artıq
    // doldurulub?" göstərmək üçün (bax DriverController#dvirList).
    List<DvirInspection> findByTripIdOrderByCreatedAtDesc(Long tripId);

    // Control Tower banner-i — bax DispatcherController#dvirDefects (eyni
    // naxış: FatigueAlert/TripIncident).
    List<DvirInspection> findByHasDefectsTrueAndResolvedFalseOrderByCreatedAtDesc();

    // Dispetçerin "Reys yarat" formasında sürücü seçərkən — bu sürücünün
    // (istənilən reysindən) həll olunmamış DVIR defekti varmı (bax
    // DispatcherController#availableDrivers). DvirInspection-da birbaşa
    // driver FK-si yoxdur, trip üzərindən naviqasiya olunur (nested property
    // path — Spring Data bunu "trip.driver.id" olaraq tanıyır).
    List<DvirInspection> findByTrip_Driver_IdAndHasDefectsTrueAndResolvedFalse(Long driverId);
}
