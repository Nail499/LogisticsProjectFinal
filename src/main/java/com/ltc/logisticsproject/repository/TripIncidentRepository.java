package com.ltc.logisticsproject.repository;

import com.ltc.logisticsproject.entity.TripIncident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripIncidentRepository extends JpaRepository<TripIncident, Long> {
    List<TripIncident> findByResolvedFalseOrderByCreatedAtDesc();
    // Reysin "ətraflı görünüş" tarixçəsi üçün (istəyə bağlı, gələcək istifadə).
    List<TripIncident> findByTripIdOrderByCreatedAtDesc(Long tripId);
}
