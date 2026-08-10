package com.ltc.logisticsproject.dto;

import com.ltc.logisticsproject.entity.DriverStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

// Stage 10 — Admin "Sürücülər" list: Driver + its linked User's username in
// one row, so the admin can see who they're about to reset a password for
// without a second lookup.
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DriverAdminView {
    Long driverId;
    String fullName;
    String phone;
    String licenseNumber;
    DriverStatus status;
    String username; // null if somehow no User is linked yet

    // Sürücü performans reytinqi — müştəri qiymətləndirmələrinin ortalaması
    // (bax RatingService) + tamamlanmış reys sayı. Admin "Sürücülər"
    // siyahısında sıralama/filtr üçün istifadə edilə bilər.
    Double averageRating;
    Integer ratingCount;
    Long deliveredTripsCount;
}
