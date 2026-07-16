package com.ltc.logisticsproject.service;

import com.ltc.logisticsproject.entity.*;
import com.ltc.logisticsproject.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExpenseService {

    final TripExpenseRepository tripExpenseRepository;
    final TripRepository tripRepository;

    public TripExpense addExpense(Long tripId, ExpenseCategory category, Double amount, String description) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Reys tapılmadı"));

        boolean anomaly = detectAnomaly(category, amount);

        TripExpense expense = TripExpense.builder()
                .trip(trip)
                .category(category)
                .amount(amount)
                .description(description)
                .isAnomaly(anomaly)
                .build();

        return tripExpenseRepository.save(expense);
    }

    private boolean detectAnomaly(ExpenseCategory category, Double amount) {
        List<TripExpense> history = tripExpenseRepository.findByCategory(category);

        if (history.size() < 5) {
            return false;
        }

        double mean = history.stream().mapToDouble(TripExpense::getAmount).average().orElse(0);
        double variance = history.stream()
                .mapToDouble(e -> Math.pow(e.getAmount() - mean, 2))
                .average().orElse(0);
        double stdDev = Math.sqrt(variance);
        double threshold = mean + (2 * stdDev);

        return amount > threshold;
    }
}