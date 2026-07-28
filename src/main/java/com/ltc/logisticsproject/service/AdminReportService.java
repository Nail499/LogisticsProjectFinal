package com.ltc.logisticsproject.service;

import com.ltc.logisticsproject.dto.AdminSummaryResponse;
import com.ltc.logisticsproject.dto.MonthlyMetricPoint;
import com.ltc.logisticsproject.dto.DispatcherAnalyticsResponse;
import com.ltc.logisticsproject.entity.*;
import com.ltc.logisticsproject.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminReportService {

    final DriverRepository driverRepository;
    final VehicleRepository vehicleRepository;
    final TripRepository tripRepository;
    final JobApplicationRepository jobApplicationRepository;
    final CargoRepository cargoRepository;
    final TripExpenseRepository tripExpenseRepository;

    // Rough, documented estimate — real diesel trucks average roughly
    // 0.7-1.0 kg CO2 per km depending on load/vehicle. Not a measured value;
    // used purely so the Dispatcher Control Tower has a directional carbon
    // metric until real per-vehicle fuel telemetry exists (Stage 6+).
    private static final double CARBON_KG_PER_KM_ESTIMATE = 0.85;

    public AdminSummaryResponse getSummary() {
        List<Trip> allTrips = tripRepository.findAll();
        List<TripExpense> allExpenses = tripExpenseRepository.findAll();

        long delivered = allTrips.stream().filter(t -> t.getStatus() == TripStatus.DELIVERED).count();
        long pendingApps = jobApplicationRepository.findAll().stream()
                .filter(a -> a.getStatus() == ApplicationStatus.PENDING).count();
        long pendingCargo = cargoRepository.findByStatus(CargoStatus.PENDING).size();
        double totalExpenses = allExpenses.stream().mapToDouble(TripExpense::getAmount).sum();
        long anomalyCount = allExpenses.stream().filter(TripExpense::getIsAnomaly).count();

        return AdminSummaryResponse.builder()
                .totalDrivers(driverRepository.count())
                .totalVehicles(vehicleRepository.count())
                .totalTrips(allTrips.size())
                .deliveredTrips(delivered)
                .pendingApplications(pendingApps)
                .pendingCargo(pendingCargo)
                .totalExpenses(totalExpenses)
                .anomalyCount(anomalyCount)
                .build();
    }

    public List<TripExpense> getAnomalies() {
        return tripExpenseRepository.findAll().stream()
                .filter(TripExpense::getIsAnomaly)
                .collect(Collectors.toList());
    }

    public Map<String, Long> getTopRoutes() {
        return tripRepository.findAll().stream()
                .filter(t -> t.getRouteInfo() != null)
                .collect(Collectors.groupingBy(Trip::getRouteInfo, Collectors.counting()));
    }

    // Stage 4 (Dispatcher Control Tower): monthly expense totals + an
    // estimated monthly carbon footprint, both bucketed by calendar month.
    // Reused by both the ADMIN and DISPATCHER report endpoints.
    public DispatcherAnalyticsResponse getMonthlyAnalytics() {
        Map<YearMonth, Double> expensesByMonth = new TreeMap<>();
        for (TripExpense e : tripExpenseRepository.findAll()) {
            if (e.getRecordedAt() == null || e.getAmount() == null) continue;
            YearMonth ym = YearMonth.from(e.getRecordedAt());
            expensesByMonth.merge(ym, e.getAmount(), Double::sum);
        }

        Map<YearMonth, Double> carbonByMonth = new TreeMap<>();
        for (Trip t : tripRepository.findAll()) {
            if (t.getCreatedAt() == null || t.getEstimatedDistanceKm() == null) continue;
            YearMonth ym = YearMonth.from(t.getCreatedAt());
            carbonByMonth.merge(ym, t.getEstimatedDistanceKm() * CARBON_KG_PER_KM_ESTIMATE, Double::sum);
        }

        return DispatcherAnalyticsResponse.builder()
                .monthlyExpenses(toPoints(expensesByMonth))
                .monthlyCarbonFootprintKg(toPoints(carbonByMonth))
                .build();
    }

    private List<MonthlyMetricPoint> toPoints(Map<YearMonth, Double> byMonth) {
        return byMonth.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> MonthlyMetricPoint.builder()
                        .label(entry.getKey().getMonth().getDisplayName(TextStyle.SHORT, new Locale("az")) + " " + entry.getKey().getYear())
                        .value(Math.round(entry.getValue() * 100.0) / 100.0)
                        .build())
                .collect(Collectors.toList());
    }
}
