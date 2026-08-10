package com.ltc.logisticsproject.service;

import com.ltc.logisticsproject.dto.HosStatusResponse;
import com.ltc.logisticsproject.entity.FatigueAlert;
import com.ltc.logisticsproject.entity.HosSegment;
import com.ltc.logisticsproject.entity.HosStatus;
import com.ltc.logisticsproject.entity.Trip;
import com.ltc.logisticsproject.entity.TripStatus;
import com.ltc.logisticsproject.repository.FatigueAlertRepository;
import com.ltc.logisticsproject.repository.HosSegmentRepository;
import com.ltc.logisticsproject.repository.TripRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// Server-tərəfli HOS (iş saatı) qeydiyyatı — köhnə RestModeCard tamamilə
// client-side stopwatch idi (yalnız React state-də), tarayıcı
// bağlananda/səhifə yenilənəndə sayğac sıfırlanırdı. İndi hər DRIVING/RESTING
// keçidi backend-də HosSegment kimi saxlanılır, "davam edən sürücülük" və
// "bugünkü ümumi sürücülük" server-də hesablanır — bax
// DriverController#hosStatus/hosToggle.
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HosService {

    static final double FATIGUE_THRESHOLD_HOURS = 4.5;
    // ABŞ FMCSA normasına yaxın sadələşdirilmiş gündəlik sürücülük limiti —
    // real HOS uyğunluq/qanuni sistem deyil (bax fayl başındakı qeyd), yalnız
    // dispetçerə "bu sürücüyə nə qədər saat qalıb" görünürlüyü vermək üçün
    // (bax getDriverSnapshot, DispatcherController#availableDrivers).
    static final double MAX_DAILY_DRIVING_HOURS = 11.0;

    final HosSegmentRepository hosSegmentRepository;
    final TripRepository tripRepository;
    final FatigueAlertRepository fatigueAlertRepository;

    @Transactional
    public HosStatusResponse getStatus(Long tripId, Long driverId) {
        Trip trip = requireOwnedTrip(tripId, driverId);
        return buildStatus(trip);
    }

    // Dispetçerin "Reys yarat" formasında sürücü seçərkən görməli olduğu
    // sürücü-səviyyəli (tripId-siz) HOS xülasəsi. HosSegment cədvəli trip_id
    // FK ilə bağlı olduğu üçün (bax fayl başındakı qeyd) sürücünün "bugünkü"
    // sürücülük saatı yalnız onun HAZIRDA icra etdiyi reys üzərindən
    // hesablana bilir — aktiv reysi yoxdursa sıfırdan başlayır sayılır.
    @Transactional
    public DriverHosSnapshot getDriverSnapshot(Long driverId) {
        List<Trip> activeTrips = tripRepository.findByDriverIdAndStatusIn(
                driverId, List.of(TripStatus.PLANNED, TripStatus.PICKED_UP, TripStatus.IN_TRANSIT));
        if (activeTrips.isEmpty()) {
            return new DriverHosSnapshot(false, "NONE", 0.0, MAX_DAILY_DRIVING_HOURS, false);
        }
        // Sadəlik: eyni anda birdən çox aktiv reysi olmamalıdır (dispetçer
        // axını buna imkan vermir) — ilk tapılanı təmsilçi kimi götürürük.
        Trip trip = activeTrips.get(0);
        HosStatusResponse status = buildStatus(trip);
        double todayHours = status.getTodayDrivingSeconds() / 3600.0;
        double remaining = Math.max(0.0, MAX_DAILY_DRIVING_HOURS - todayHours);
        return new DriverHosSnapshot(
                true, status.getStatus(),
                Math.round(todayHours * 10.0) / 10.0,
                Math.round(remaining * 10.0) / 10.0,
                status.isFatigueWarning());
    }

    public record DriverHosSnapshot(
            boolean hasActiveTrip,
            String hosStatus,
            double todayDrivingHours,
            double remainingDrivingHours,
            boolean fatigueWarning) {
    }

    // MAX_DAILY_DRIVING_HOURS sahəsinin özü Lombok @FieldDefaults(PRIVATE)
    // tərəfindən private edilir (açıq access modifier yazılmayıb) — başqa
    // servisdən (DriverSuggestionService#computeScore) həmin nisbəti təkrar
    // sabit kimi yazmaq əvəzinə buradan oxumaq üçün ictimai statik getter.
    public static double getMaxDailyDrivingHours() {
        return MAX_DAILY_DRIVING_HOURS;
    }

    // Açıq seqment varsa bağlayır, əks statuslu yeni seqment açır. Heç vaxt
    // toggle edilməyibsə (ilk dəfə) default olaraq DRIVING başladılır — köhnə
    // UI-da "aç" düyməsi məhz sürücülük sayğacını başladırdı.
    @Transactional
    public HosStatusResponse toggle(Long tripId, Long driverId) {
        Trip trip = requireOwnedTrip(tripId, driverId);
        LocalDateTime now = LocalDateTime.now();

        Optional<HosSegment> openOpt = hosSegmentRepository.findByTripIdAndEndedAtIsNull(tripId);
        HosStatus nextStatus = HosStatus.DRIVING;
        if (openOpt.isPresent()) {
            HosSegment open = openOpt.get();
            open.setEndedAt(now);
            hosSegmentRepository.save(open);
            nextStatus = open.getStatus() == HosStatus.DRIVING ? HosStatus.RESTING : HosStatus.DRIVING;
        }

        HosSegment newSegment = HosSegment.builder()
                .trip(trip)
                .status(nextStatus)
                .startedAt(now)
                .alertSent(false)
                .build();
        hosSegmentRepository.save(newSegment);

        return buildStatus(trip);
    }

    private HosStatusResponse buildStatus(Trip trip) {
        LocalDateTime now = LocalDateTime.now();
        Optional<HosSegment> openOpt = hosSegmentRepository.findByTripIdAndEndedAtIsNull(trip.getId());

        String status = "NONE";
        String segmentStartedAt = null;
        long continuousDrivingSeconds = 0;
        boolean fatigueWarning = false;

        if (openOpt.isPresent()) {
            HosSegment open = openOpt.get();
            status = open.getStatus().name();
            segmentStartedAt = open.getStartedAt().toString();

            if (open.getStatus() == HosStatus.DRIVING) {
                continuousDrivingSeconds = Duration.between(open.getStartedAt(), now).getSeconds();
                double hours = continuousDrivingSeconds / 3600.0;
                fatigueWarning = hours >= FATIGUE_THRESHOLD_HOURS;

                // Astana keçiləndə BİR DƏFƏ FatigueAlert yaradılır — köhnə
                // client-side versiyada bunu sürücünün brauzeri edirdi (bax
                // DriverController#raiseFatigueAlert), indi server özü aşkar
                // edib eyni cədvələ yazır ki, Control Tower banner-i (bax
                // DispatcherController#fatigueAlerts) dəyişməsin.
                if (fatigueWarning && !Boolean.TRUE.equals(open.getAlertSent())) {
                    open.setAlertSent(true);
                    hosSegmentRepository.save(open);

                    FatigueAlert alert = FatigueAlert.builder()
                            .trip(trip)
                            .driverName(trip.getDriver() != null ? trip.getDriver().getFullName() : null)
                            .vehiclePlate(trip.getVehicle() != null ? trip.getVehicle().getPlateNumber() : null)
                            .continuousDrivingHours(Math.round(hours * 100.0) / 100.0)
                            .resolved(false)
                            .build();
                    fatigueAlertRepository.save(alert);
                }
            }
        }

        long todayDrivingSeconds = computeTodayDrivingSeconds(trip.getId(), now);

        return HosStatusResponse.builder()
                .status(status)
                .segmentStartedAt(segmentStartedAt)
                .continuousDrivingSeconds(continuousDrivingSeconds)
                .todayDrivingSeconds(todayDrivingSeconds)
                .fatigueWarning(fatigueWarning)
                .build();
    }

    // Sadə yanaşma — HOS uyğunluq (compliance) sistemi deyil, sadəcə
    // görünürlük üçündür: gecə yarısını keçən seqmentlər dəqiq bölünmür,
    // seqment bugünə aiddirsə (başlayıb və ya bitib) tam sayılır.
    private long computeTodayDrivingSeconds(Long tripId, LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        List<HosSegment> segments = hosSegmentRepository.findByTripIdOrderByStartedAtAsc(tripId);

        long total = 0;
        for (HosSegment seg : segments) {
            if (seg.getStatus() != HosStatus.DRIVING) continue;
            LocalDateTime start = seg.getStartedAt();
            LocalDateTime end = seg.getEndedAt() != null ? seg.getEndedAt() : now;

            boolean touchesToday = start.toLocalDate().equals(today) || end.toLocalDate().equals(today);
            if (!touchesToday) continue;

            LocalDateTime segStart = start.toLocalDate().equals(today) ? start : today.atStartOfDay();
            LocalDateTime segEnd = end.toLocalDate().equals(today) ? end : today.atTime(23, 59, 59);
            total += Math.max(0, Duration.between(segStart, segEnd).getSeconds());
        }
        return total;
    }

    private Trip requireOwnedTrip(Long tripId, Long driverId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Reys tapılmadı"));
        if (trip.getDriver() == null || !trip.getDriver().getId().equals(driverId)) {
            throw new RuntimeException("Bu reys sizə aid deyil");
        }
        return trip;
    }
}
