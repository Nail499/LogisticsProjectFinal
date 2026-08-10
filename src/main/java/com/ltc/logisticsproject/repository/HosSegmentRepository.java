package com.ltc.logisticsproject.repository;

import com.ltc.logisticsproject.entity.HosSegment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HosSegmentRepository extends JpaRepository<HosSegment, Long> {
    // Ən çoxu bir açıq (hazırda davam edən) seqment ola bilər — bax
    // HosService#toggle.
    Optional<HosSegment> findByTripIdAndEndedAtIsNull(Long tripId);

    // Bugünkü ümumi sürücülük vaxtını hesablamaq üçün (bax
    // HosService#computeTodayDrivingSeconds) — bütün seqmentlər üzərindən
    // gedilir, reys üzrə say adətən kiçikdir.
    List<HosSegment> findByTripIdOrderByStartedAtAsc(Long tripId);
}
