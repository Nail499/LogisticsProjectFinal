package com.ltc.logisticsproject.service;

import com.ltc.logisticsproject.dto.rating.RatingDetailResponse;
import com.ltc.logisticsproject.entity.*;
import com.ltc.logisticsproject.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RatingService {

    final RatingRepository ratingRepository;
    final TripRepository tripRepository;
    final CargoRepository cargoRepository;
    final UserRepository userRepository;
    final NotificationService notificationService;

    public Rating submitRating(Long tripId, Long customerId, Integer stars, String comment) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Reys tapılmadı"));

        if (trip.getStatus() != TripStatus.DELIVERED) {
            throw new RuntimeException("Yalnız çatdırılmış reyslər qiymətləndirilə bilər");
        }
        if (trip.getDriver() == null) {
            throw new RuntimeException("Bu reysə sürücü təyin edilməyib");
        }
        if (stars == null || stars < 1 || stars > 5) {
            throw new RuntimeException("Qiymət 1-5 arasında olmalıdır");
        }

        // Bu müştərinin doğrudan bu reysdə yükü olduğunu yoxla — başqasının
        // reysini qiymətləndirməsin.
        boolean belongsToCustomer = cargoRepository.findByTripId(tripId).stream()
                .anyMatch(c -> c.getCustomer() != null && c.getCustomer().getId().equals(customerId));
        if (!belongsToCustomer) {
            throw new RuntimeException("Bu reysi qiymətləndirə bilməzsiniz");
        }

        if (ratingRepository.findByTripIdAndCustomerId(tripId, customerId).isPresent()) {
            throw new RuntimeException("Bu reysi artıq qiymətləndirmisiniz");
        }

        Rating rating = Rating.builder()
                .tripId(tripId)
                .customerId(customerId)
                .driverId(trip.getDriver().getId())
                .stars(stars)
                .comment(comment)
                .build();
        rating = ratingRepository.save(rating);

        // Sürücüyə bildiriş — həm zəng ikonu, həm email yox (qısa, minor
        // hadisə — email spam-a çevrilməsin, yalnız in-app).
        userRepository.findByDriverId(trip.getDriver().getId()).ifPresent(driverUser ->
                notificationService.notify(
                        driverUser.getId(), NotificationType.GENERAL,
                        "Yeni qiymətləndirmə aldınız",
                        "Reys #" + tripId + " üçün " + stars + " ulduz qiymət aldınız" + (comment != null && !comment.isBlank() ? ": \"" + comment + "\"" : "."),
                        "/driver/ratings"
                )
        );

        return rating;
    }

    public RatingSummary getDriverSummary(Long driverId) {
        List<Rating> ratings = ratingRepository.findByDriverId(driverId);
        if (ratings.isEmpty()) {
            return new RatingSummary(0.0, 0);
        }
        double avg = ratings.stream().mapToInt(Rating::getStars).average().orElse(0);
        return new RatingSummary(Math.round(avg * 10.0) / 10.0, ratings.size());
    }

    // Admin/dispetçer "Reytinqlər" səhifəsi — bütün sürücülər üzrə hər bir
    // qiymətləndirmənin hansı reysdən, hansı müştəridən gəldiyini görmək
    // üçün (bax istifadəçinin "hansı reysdən nə reytinq/şərh alıb, ətraflı
    // öyrənə bilsin" tələbi).
    public List<RatingDetailResponse> getAllRatingsDetailed() {
        return ratingRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toDetail).toList();
    }

    // Sürücünün öz "Reytinqlərim" səhifəsi + admin-in bir sürücünün bütün
    // qiymətləndirmələrinə ətraflı baxması (AdminDrivers "Ətraflı bax") üçün
    // eyni məlumat.
    public List<RatingDetailResponse> getDriverRatingsDetailed(Long driverId) {
        return ratingRepository.findByDriverIdOrderByCreatedAtDesc(driverId).stream().map(this::toDetail).toList();
    }

    private RatingDetailResponse toDetail(Rating r) {
        Trip trip = tripRepository.findById(r.getTripId()).orElse(null);
        String driverName = trip != null && trip.getDriver() != null ? trip.getDriver().getFullName() : null;
        String vehiclePlate = trip != null && trip.getVehicle() != null ? trip.getVehicle().getPlateNumber() : null;
        String routeInfo = trip != null ? trip.getRouteInfo() : null;
        String deliveredAt = trip != null && trip.getDeliveredAt() != null ? trip.getDeliveredAt().toString() : null;

        // Bu reysdə birləşdirilmiş yüklərdən məhz bu qiyməti verən müştəriyə
        // aid olanları tap (bax Rating — açar tripId+customerId cütlüyüdür,
        // eyni reysdə fərqli müştərilər ayrı qiymət verə bilər).
        List<Cargo> myCargos = cargoRepository.findByTripId(r.getTripId()).stream()
                .filter(c -> c.getCustomer() != null && c.getCustomer().getId().equals(r.getCustomerId()))
                .toList();
        String customerName = myCargos.stream().findFirst()
                .map(c -> c.getCustomer().getFullName())
                .orElse(null);
        String trackingNumbers = myCargos.stream()
                .map(Cargo::getTrackingNumber)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));

        return RatingDetailResponse.builder()
                .id(r.getId())
                .tripId(r.getTripId())
                .driverId(r.getDriverId())
                .driverName(driverName)
                .vehiclePlate(vehiclePlate)
                .customerName(customerName)
                .trackingNumber(trackingNumbers.isBlank() ? null : trackingNumbers)
                .routeInfo(routeInfo)
                .stars(r.getStars())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt() != null ? r.getCreatedAt().toString() : null)
                .deliveredAt(deliveredAt)
                .build();
    }

    public record RatingSummary(double average, int count) {
    }
}
