package com.ltc.logisticsproject.repository;

import com.ltc.logisticsproject.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    Optional<Rating> findByTripIdAndCustomerId(Long tripId, Long customerId);
    List<Rating> findByTripId(Long tripId);
    List<Rating> findByDriverId(Long driverId);
    // Admin/dispetçer "bütün qiymətləndirmələr" siyahısı + sürücünün öz
    // "reytinqlərim" səhifəsi üçün — həmişə ən yenisi əvvəldə.
    List<Rating> findAllByOrderByCreatedAtDesc();
    List<Rating> findByDriverIdOrderByCreatedAtDesc(Long driverId);
}
