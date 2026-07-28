package com.ltc.logisticsproject.repository;

import com.ltc.logisticsproject.entity.ExpenseCategory;
import com.ltc.logisticsproject.entity.TripExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TripExpenseRepository extends JpaRepository<TripExpense,Long> {
    Optional<TripExpense> findByTripId(Long tripId);
    List<TripExpense> findByCategory(ExpenseCategory category);
    // Used by PublicTrackingController to surface a trip's road expenses to
    // the customer (not just admin/dispatcher) — a trip can have several
    // expense rows (fuel, toll, food...), so unlike findByTripId above this
    // returns all of them, most recent first.
    List<TripExpense> findByTripIdOrderByRecordedAtDesc(Long tripId);
}
