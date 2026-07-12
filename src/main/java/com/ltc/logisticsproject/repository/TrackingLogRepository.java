package com.ltc.logisticsproject.repository;

import com.ltc.logisticsproject.entity.TrackingLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrackingLogRepository extends JpaRepository<TrackingLog,Long> {
    List<TrackingLog> findByTripIdOrderByRecordedAtAsc(Long tripId);
}
