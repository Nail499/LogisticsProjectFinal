package com.ltc.logisticsproject.repository;

import com.ltc.logisticsproject.entity.FatigueAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FatigueAlertRepository extends JpaRepository<FatigueAlert, Long> {
    List<FatigueAlert> findByResolvedFalseOrderByCreatedAtDesc();
}
