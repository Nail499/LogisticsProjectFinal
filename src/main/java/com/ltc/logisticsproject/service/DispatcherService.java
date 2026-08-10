package com.ltc.logisticsproject.service;

import com.ltc.logisticsproject.dto.TripRequest;
import com.ltc.logisticsproject.entity.*;
import com.ltc.logisticsproject.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DispatcherService {

    final TripRepository tripRepository;
    final DriverRepository driverRepository;
    final VehicleRepository vehicleRepository;
    final TrailerRepository trailerRepository;
    final CargoRepository cargoRepository;
    // Reys qəbul/imtina: yeni reys yaradılanda sürücüyə "qəbul et/imtina et"
    // bildirişi göndərmək üçün (bax notifyTripAssigned çağırışı aşağıda).
    final UserRepository userRepository;
    final NotificationService notificationService;

    @Transactional
    public Trip createTrip(TripRequest request) {
        Driver driver = driverRepository.findById(request.getDriverId())
                .orElseThrow(() -> new RuntimeException("Sürücü tapılmadı"));

        if (driver.getStatus() != DriverStatus.ACTIVE) {
            throw new RuntimeException("Sürücü aktiv deyil");
        }

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new RuntimeException("Tır tapılmadı"));

        // Sürücüyə məxsus (DRIVER_OWNED) tır yalnız öz sahibi ilə reysə çıxa
        // bilər — frontend seçim siyahısını artıq filtrləyir (bax
        // DispatcherController#allVehicles driverId parametri), amma backend
        // də bunu təsdiqləməlidir (məs. köhnə açılmış tab-dan sərt sorğu).
        if (vehicle.getOwnerType() == OwnerType.DRIVER_OWNED
                && (vehicle.getDriver() == null || !vehicle.getDriver().getId().equals(driver.getId()))) {
            throw new RuntimeException("Bu tır başqa sürücüyə məxsusdur");
        }

        Trailer trailer = null;
        if (request.getTrailerId() != null) {
            trailer = trailerRepository.findById(request.getTrailerId())
                    .orElseThrow(() -> new RuntimeException("Qoşqu tapılmadı"));
            if (trailer.getOwnerType() == OwnerType.DRIVER_OWNED
                    && (trailer.getDriver() == null || !trailer.getDriver().getId().equals(driver.getId()))) {
                throw new RuntimeException("Bu qoşqu başqa sürücüyə məxsusdur");
            }
        }

        List<Cargo> cargos = cargoRepository.findAllById(request.getCargoIds());

        if (cargos.isEmpty()) {
            throw new RuntimeException("Ən azı bir yük seçilməlidir");
        }

        for (Cargo cargo : cargos) {
            if (cargo.getStatus() != CargoStatus.PENDING) {
                throw new RuntimeException("Cargo #" + cargo.getId() + " artıq təhkim olunub");
            }
        }

        Trip trip = Trip.builder()
                .driver(driver)
                .vehicle(vehicle)
                .trailer(trailer)
                // Reys birbaşa PLANNED-a düşmür. Qiyməti olan yüklər üçün əvvəlcə
                // müştərinin ödəniş etməsi gözlənilir (AWAITING_PAYMENT — bax
                // PaymentService#confirmPayment, ödəniş tamamlananda avtomatik
                // PENDING_ACCEPTANCE-a keçib sürücüyə göndərilir). Qiyməti olmayan
                // (estimatedCost boş buraxılmış) yüklər ödəniş gözləmədən birbaşa
                // sürücüyə göndərilir.
                .status(TripStatus.AWAITING_PAYMENT)
                .estimatedDistanceKm(request.getEstimatedDistanceKm())
                .estimatedCost(request.getEstimatedCost())
                .routeInfo(request.getRouteInfo())
                .build();
        trip = tripRepository.save(trip);

        // Dispetçerin "Reys yarat" formasında yazdığı "Təxmini xərc" — istifadəçi
        // istəyi ilə (bax PaymentService#createPaymentIntent — cargo.price
        // varsa, avtomatik PricingService hesablamasından ÜSTÜN tutulur) bu
        // ədəd birbaşa reysdəki HƏR yükün müştəriyə göstəriləcək qiyməti kimi
        // təyin olunur, əvvəlki (avtomatik hesablanmış və ya boş) qiyməti
        // əvəz edir. Xana boş buraxılıbsa (null), yükün qiyməti toxunulmaz
        // qalır — o zaman ödəniş zamanı hələ də PricingService-in avtomatik
        // hesabladığı qiymət istifadə olunur.
        for (Cargo cargo : cargos) {
            cargo.setTrip(trip);
            cargo.setStatus(CargoStatus.ASSIGNED);
            if (request.getEstimatedCost() != null) {
                cargo.setPrice(request.getEstimatedCost());
            }
            cargoRepository.save(cargo);
        }

        // Ödəniş gözləmək yalnız HƏLƏ ödənilməmiş, qiyməti olan yüklər üçün
        // lazımdır. Bura ƏSAS SƏBƏB: sürücü imtina edib/dispetçer ləğv edib
        // yük yenidən növbəyə düşəndən sonra BAŞQA sürücüyə təhkim edilirsə,
        // müştəri onsuz da artıq ödəyib (cargo.paid=true qalır, bax
        // DriverTripService#rejectTrip / DispatcherService#cancelTrip) —
        // belə yükü yenidən AWAITING_PAYMENT-ə salsaq, heç bir yeni ödəniş
        // hadisəsi baş verməyəcəyi üçün (müştəri "artıq ödənilib" görür,
        // Ödə düyməsi göstərilmir) reys əbədi asılı qalardı. Ona görə "hələ
        // ödəniş gözləyən" yük = qiyməti VAR və HƏLƏ ödənilməyib.
        boolean anyCargoNeedsPayment = cargos.stream()
                .anyMatch(c -> c.getPrice() != null && !Boolean.TRUE.equals(c.getPaid()));
        if (!anyCargoNeedsPayment) {
            trip.setStatus(TripStatus.PENDING_ACCEPTANCE);
            trip = tripRepository.save(trip);

            Trip savedTrip = trip;
            userRepository.findByDriverId(driver.getId()).ifPresent(user ->
                    notificationService.notifyTripAssigned(
                            savedTrip, user.getId(), driver.getEmail(),
                            cargos.get(0).getPickupAddress(), cargos.get(0).getDestinationAddress()
                    )
            );
        }

        return trip;
    }

    // Sürücü uzun müddət qəbul/imtina etmirsə (və ya hələ ödəniş gözlənilirsə),
    // dispetçer reysi özü ləğv edib yükü yenidən növbəyə qaytara bilsin —
    // sürücünün cavabını gözləmək məcburi deyil. Yalnız reys HƏLƏ
    // aktivləşməmiş mərhələdədirsə (AWAITING_PAYMENT və ya PENDING_ACCEPTANCE)
    // mümkündür; sürücü artıq qəbul edib yola çıxıbsa, bu yolla ləğv edilə bilməz.
    @Transactional
    public Trip cancelTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Reys tapılmadı"));

        TripStatus current = trip.getStatus();
        if (current != TripStatus.AWAITING_PAYMENT && current != TripStatus.PENDING_ACCEPTANCE) {
            throw new RuntimeException("Yalnız hələ başlamamış (ödəniş/qəbul gözləyən) reyslər ləğv edilə bilər");
        }

        trip.setStatus(TripStatus.CANCELLED);
        Trip saved = tripRepository.save(trip);

        List<Cargo> cargos = cargoRepository.findByTripId(tripId);
        for (Cargo cargo : cargos) {
            cargo.setTrip(null);
            cargo.setStatus(CargoStatus.PENDING);
            cargoRepository.save(cargo);
        }

        // Sürücü yalnız trip PENDING_ACCEPTANCE-a çatmışdısa xəbərdar
        // edilmişdi (AWAITING_PAYMENT mərhələsində sürücü hələ heç nə
        // görməyib, ona bildiriş göndərməyə ehtiyac yoxdur).
        if (current == TripStatus.PENDING_ACCEPTANCE && saved.getDriver() != null) {
            Driver driver = saved.getDriver();
            userRepository.findByDriverId(driver.getId()).ifPresent(user ->
                    notificationService.notifyTripCancelledByDispatcher(saved, user.getId(), driver.getEmail())
            );
        }

        return saved;
    }
}