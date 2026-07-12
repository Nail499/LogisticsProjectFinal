package com.ltc.logisticsproject.repository;

import com.ltc.logisticsproject.entity.TripExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TripExpenseRepository extends JpaRepository<TripExpense,Long> {
    Optional<TripExpense> findByTripId(Long tripId);
}
