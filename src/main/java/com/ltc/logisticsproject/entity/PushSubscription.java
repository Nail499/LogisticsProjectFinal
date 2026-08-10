package com.ltc.logisticsproject.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

// Brauzerin Push API abunəliyi (bax PushNotificationService) — bir
// istifadəçinin bir neçə cihazdan/brauzerdən abunə olması mümkündür,
// ona görə userId unikal deyil, "endpoint" unikaldır (hər brauzer
// abunəliyi öz unikal endpoint URL-ini alır).
@Entity
@Table(name = "push_subscriptions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    Long userId;

    @Column(nullable = false, unique = true, length = 500)
    String endpoint;

    @Column(nullable = false, length = 255)
    String p256dh;

    @Column(nullable = false, length = 255)
    String auth;

    @Column(nullable = false)
    LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
