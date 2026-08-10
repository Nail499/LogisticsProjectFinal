package com.ltc.logisticsproject.service;

import com.ltc.logisticsproject.dto.AdminSummaryResponse;
import com.ltc.logisticsproject.dto.AnomalyExpenseResponse;
import com.ltc.logisticsproject.dto.CustomerSummary;
import com.ltc.logisticsproject.dto.DispatcherKpiResponse;
import com.ltc.logisticsproject.dto.MonthlyMetricPoint;
import com.ltc.logisticsproject.dto.DispatcherAnalyticsResponse;
import com.ltc.logisticsproject.entity.*;
import com.ltc.logisticsproject.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
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
    // Dispetçer KPI panelinin qoşqu utilizasiyası kartı üçün (bax
    // getDispatcherKpis).
    final TrailerRepository trailerRepository;
    final RouteEstimationService routeEstimationService;

    // Rough, documented estimate — real diesel trucks average roughly
    // 0.7-1.0 kg CO2 per km depending on load/vehicle. Not a measured value;
    // used purely so the Dispatcher Control Tower has a directional carbon
    // metric until real per-vehicle fuel telemetry exists (Stage 6+).
    private static final double CARBON_KG_PER_KM_ESTIMATE = 0.85;

    // "Vaxtında" sayılmaq üçün estimasiyaya nisbətən icazə verilən əlavə
    // vaxt — real dünyada yükləmə/boşaltma/gömrük/trafik kimi estimasiyada
    // olmayan gecikmələr var, ona görə xam estimasiyanın özü limit kimi
    // istifadə olunsa hər reys demək olar "gecikmiş" görünərdi.
    private static final double ON_TIME_TOLERANCE_FACTOR = 1.25;

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

    public List<AnomalyExpenseResponse> getAnomalies() {
        List<TripExpense> allExpenses = tripExpenseRepository.findAll();

        // Hər kateqoriya üçün orta xərci bir dəfə hesabla — "nə üçün
        // şübhəlidir" izahı (bax AnomalyExpenseResponse) elə bu ortalamaya
        // görə qurulur (ExpenseService#detectAnomaly-dəki mean+2*stdDev
        // həddi ilə eyni "mean" anlayışı).
        Map<ExpenseCategory, Double> categoryAverages = new EnumMap<>(ExpenseCategory.class);
        for (ExpenseCategory category : ExpenseCategory.values()) {
            double avg = allExpenses.stream()
                    .filter(e -> e.getCategory() == category)
                    .mapToDouble(TripExpense::getAmount)
                    .average().orElse(0);
            categoryAverages.put(category, avg);
        }

        return allExpenses.stream()
                .filter(TripExpense::getIsAnomaly)
                .map(exp -> buildAnomalyResponse(exp, categoryAverages.getOrDefault(exp.getCategory(), 0.0)))
                .collect(Collectors.toList());
    }

    private AnomalyExpenseResponse buildAnomalyResponse(TripExpense exp, double categoryAverage) {
        Trip trip = exp.getTrip();
        List<Cargo> cargos = trip != null ? cargoRepository.findByTripId(trip.getId()) : List.of();

        String trackingNumber = null;
        String pickupAddress = null;
        String destinationAddress = null;
        List<CustomerSummary> customers = List.of();
        if (!cargos.isEmpty()) {
            Cargo firstCargo = cargos.get(0);
            trackingNumber = firstCargo.getTrackingNumber();
            pickupAddress = firstCargo.getPickupAddress();
            destinationAddress = firstCargo.getDestinationAddress();
            customers = cargos.stream().map(CustomerSummary::from).toList();
        }

        double percentAbove = categoryAverage > 0
                ? Math.round(((exp.getAmount() - categoryAverage) / categoryAverage) * 1000.0) / 10.0
                : 0.0;

        return AnomalyExpenseResponse.builder()
                .id(exp.getId())
                .category(exp.getCategory())
                .amount(exp.getAmount())
                .description(exp.getDescription())
                .recordedAt(exp.getRecordedAt() != null ? exp.getRecordedAt().toString() : null)
                .receiptPhotoUrl(exp.getReceiptPhotoUrl())
                .categoryAverageAmount(Math.round(categoryAverage * 100.0) / 100.0)
                .percentAboveAverage(percentAbove)
                .tripId(trip != null ? trip.getId() : null)
                .tripStatus(trip != null ? trip.getStatus() : null)
                .trackingNumber(trackingNumber)
                .driverName(trip != null && trip.getDriver() != null ? trip.getDriver().getFullName() : null)
                .vehiclePlate(trip != null && trip.getVehicle() != null ? trip.getVehicle().getPlateNumber() : null)
                .pickupAddress(pickupAddress)
                .destinationAddress(destinationAddress)
                .customers(customers)
                .build();
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

    // Dispetçer "Control Tower" üçün əsas KPI kartları (bax
    // DispatcherKpiResponse-dəki ətraflı qeyd — onTimePercent/deadheadPercent
    // sadələşdirilmiş TƏXMİNLƏRDİR, real ölçmə deyil, çünki sistemdə nə vəd
    // edilmiş çatdırılma tarixi, nə də real GPS-lə ölçülmüş boş məsafə
    // izlənilir).
    public DispatcherKpiResponse getDispatcherKpis() {
        List<Trip> delivered = tripRepository.findAll().stream()
                .filter(t -> t.getStatus() == TripStatus.DELIVERED)
                .toList();

        Double onTimePercent = computeOnTimePercent(delivered);
        Double deadheadPercent = computeDeadheadPercent(delivered);

        List<Trailer> trailers = trailerRepository.findAll();
        double trailerUtilizationPercent = 0.0;
        Double avgCapacityUtilizationPercent = null;
        if (!trailers.isEmpty()) {
            List<TripStatus> activeStatuses = List.of(TripStatus.PLANNED, TripStatus.PICKED_UP, TripStatus.IN_TRANSIT);
            int onTripCount = 0;
            List<Double> capacityRatios = new ArrayList<>();
            for (Trailer tr : trailers) {
                List<Trip> activeTrips = tripRepository.findByTrailerIdAndStatusIn(tr.getId(), activeStatuses);
                if (activeTrips.isEmpty()) continue;
                onTripCount++;
                Trip trip = activeTrips.get(0);
                boolean loaded = trip.getStatus() == TripStatus.PICKED_UP || trip.getStatus() == TripStatus.IN_TRANSIT;
                if (loaded && tr.getCapacity() != null && tr.getCapacity() > 0) {
                    double weightKg = cargoRepository.findByTripId(trip.getId()).stream()
                            .mapToDouble(c -> c.getWeight() != null ? c.getWeight() : 0.0)
                            .sum();
                    capacityRatios.add(Math.min(1.5, weightKg / (tr.getCapacity() * 1000)) * 100.0);
                }
            }
            trailerUtilizationPercent = Math.round((onTripCount * 100.0 / trailers.size()) * 10.0) / 10.0;
            if (!capacityRatios.isEmpty()) {
                avgCapacityUtilizationPercent = Math.round(
                        capacityRatios.stream().mapToDouble(Double::doubleValue).average().orElse(0.0) * 10.0) / 10.0;
            }
        }

        return DispatcherKpiResponse.builder()
                .deliveredTripsCount(delivered.size())
                .onTimePercent(onTimePercent)
                .deadheadPercent(deadheadPercent)
                .trailerUtilizationPercent(trailerUtilizationPercent)
                .avgCapacityUtilizationPercent(avgCapacityUtilizationPercent)
                .build();
    }

    // bax DispatcherKpiResponse#onTimePercent qeydi — startedAt/deliveredAt
    // arasındakı FAKTİKİ müddət, estimatedDistanceKm-dən çıxarılan (+ 25%
    // tolerans) GÖZLƏNİLƏN müddətlə müqayisə olunur. Vəd edilmiş çatdırılma
    // tarixi sistemdə olmadığı üçün bu, ən yaxın mümkün TƏXMİNDİR.
    private Double computeOnTimePercent(List<Trip> delivered) {
        int total = 0;
        int onTime = 0;
        for (Trip t : delivered) {
            if (t.getStartedAt() == null || t.getDeliveredAt() == null || t.getEstimatedDistanceKm() == null) continue;
            total++;
            double actualHours = Duration.between(t.getStartedAt(), t.getDeliveredAt()).toMinutes() / 60.0;
            double expectedHours = routeEstimationService.estimateTravelHours(t.getEstimatedDistanceKm()) * ON_TIME_TOLERANCE_FACTOR;
            if (actualHours <= expectedHours) onTime++;
        }
        if (total == 0) return null;
        return Math.round((onTime * 100.0 / total) * 10.0) / 10.0;
    }

    // bax DispatcherKpiResponse#deadheadPercent qeydi — eyni sürücünün
    // ARDICIL iki reysi arasında (əvvəlki reysin təhvil nöqtəsi -> növbəti
    // reysin götürülmə nöqtəsi) təxmini "boş" məsafə cəmlənir və ÜMUMI
    // gedilən məsafəyə (yüklü + boş) nisbətlənir.
    private Double computeDeadheadPercent(List<Trip> delivered) {
        Map<Long, List<Trip>> byDriver = delivered.stream()
                .filter(t -> t.getDriver() != null && t.getStartedAt() != null)
                .collect(Collectors.groupingBy(t -> t.getDriver().getId()));

        double loadedKm = delivered.stream()
                .filter(t -> t.getEstimatedDistanceKm() != null)
                .mapToDouble(Trip::getEstimatedDistanceKm)
                .sum();

        double deadheadKm = 0.0;
        boolean anyPair = false;
        for (List<Trip> trips : byDriver.values()) {
            List<Trip> sorted = trips.stream()
                    .sorted(Comparator.comparing(Trip::getStartedAt))
                    .toList();
            for (int i = 1; i < sorted.size(); i++) {
                Trip prev = sorted.get(i - 1);
                Trip curr = sorted.get(i);
                Cargo prevCargo = cargoRepository.findByTripId(prev.getId()).stream().findFirst().orElse(null);
                Cargo currCargo = cargoRepository.findByTripId(curr.getId()).stream().findFirst().orElse(null);
                if (prevCargo == null || currCargo == null) continue;
                if (prevCargo.getDestinationLatitude() == null || prevCargo.getDestinationLongitude() == null
                        || currCargo.getPickupLatitude() == null || currCargo.getPickupLongitude() == null) continue;
                deadheadKm += routeEstimationService.estimateRoadDistanceKm(
                        prevCargo.getDestinationLatitude(), prevCargo.getDestinationLongitude(),
                        currCargo.getPickupLatitude(), currCargo.getPickupLongitude());
                anyPair = true;
            }
        }

        if (!anyPair || (loadedKm + deadheadKm) <= 0) return null;
        return Math.round((deadheadKm * 100.0 / (loadedKm + deadheadKm)) * 10.0) / 10.0;
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
