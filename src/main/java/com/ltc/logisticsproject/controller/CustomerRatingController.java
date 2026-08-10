package com.ltc.logisticsproject.controller;

import com.ltc.logisticsproject.dto.rating.RatingRequest;
import com.ltc.logisticsproject.dto.rating.RatingResponse;
import com.ltc.logisticsproject.entity.Rating;
import com.ltc.logisticsproject.entity.User;
import com.ltc.logisticsproject.repository.RatingRepository;
import com.ltc.logisticsproject.repository.UserRepository;
import com.ltc.logisticsproject.service.RatingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// Müştərinin çatdırılmış reysdən sonra sürücünü qiymətləndirməsi (bax
// RatingService). Bir Cargo/Trip modelindən ayrı saxlanılıb ki, gələcəkdə
// (məs. sürücünün müştərini qiymətləndirməsi) simmetrik şəkildə genişlənə
// bilsin, hazırda yalnız müştəri->sürücü istiqaməti var.
@RestController
@RequestMapping("/api/customer/trips/{tripId}/rating")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerRatingController {

    final RatingService ratingService;
    final RatingRepository ratingRepository;
    final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> submit(@PathVariable Long tripId, @RequestBody RatingRequest request, Authentication authentication) {
        Long customerId = currentCustomerId(authentication);
        Rating rating = ratingService.submitRating(tripId, customerId, request.getStars(), request.getComment());
        return ResponseEntity.ok(toResponse(rating));
    }

    @GetMapping
    public ResponseEntity<?> getExisting(@PathVariable Long tripId, Authentication authentication) {
        Long customerId = currentCustomerId(authentication);
        // Optional.map(...).orElse(...) burada iki fərqli ResponseEntity<T>
        // tipini (RatingResponse vs. gövdəsiz) qarışdırdığı üçün generic
        // nəticə çıxarımı ziddiyyətli olur (eyni problem əvvəllər
        // CustomerCargoController-də savedCargo lambda-sında olub) — sadə
        // if/else ilə bunun qarşısı alınır.
        var existing = ratingRepository.findByTripIdAndCustomerId(tripId, customerId);
        if (existing.isPresent()) {
            return ResponseEntity.ok(toResponse(existing.get()));
        }
        return ResponseEntity.noContent().build();
    }

    private RatingResponse toResponse(Rating r) {
        return RatingResponse.builder()
                .id(r.getId())
                .tripId(r.getTripId())
                .stars(r.getStars())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt() != null ? r.getCreatedAt().toString() : null)
                .build();
    }

    private Long currentCustomerId(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("İstifadəçi tapılmadı"));
        if (user.getCustomerId() == null) {
            throw new RuntimeException("Bu istifadəçi müştəri deyil");
        }
        return user.getCustomerId();
    }
}
