package com.ltc.logisticsproject.repository;

import com.ltc.logisticsproject.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndReadFalse(Long userId);

    // "Öz bildirişi olmayan id ilə oxunmuş kimi qeyd etmə" hücumunun qarşısını
    // almaq üçün — mark-as-read zamanı userId də yoxlanılır (bax
    // NotificationController#markAsRead).
    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    List<Notification> findByUserIdAndReadFalse(Long userId);
}
