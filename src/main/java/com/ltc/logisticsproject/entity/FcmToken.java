package com.ltc.logisticsproject.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

// Mobil (Android) Firebase Cloud Messaging qeydiyyat tokeni. PushSubscription
// (brauzer Web Push, VAPID p256dh/auth açarları ilə) ilə QARIŞDIRILMAMALIDIR —
// FCM tamam fərqli, daha sadə bir sxemdir: hər cihaz üçün tək bir opak token
// kifayətdir. Eyni istifadəçi bir neçə cihazdan qeydiyyatdan keçə bilər, ona
// görə userId unikal deyil, "token" unikaldır (hər cihaz öz tokenini alır və
// tətbiq silinib-yenidən qurulanda token dəyişə bilər).
//
// QEYD: bu, YALNIZ token-i saxlayır. Faktiki FCM göndərişi (Firebase Admin
// SDK, xidmət hesabı JSON-u tələb edir) hələ qoşulmayıb — bax
// PushNotificationService-dəki şərh. Backend administratoru öz Firebase
// layihəsinin xidmət hesabı açarını əlavə etməyincə, bu cədvələ token-lər
// yığılır, amma real bildiriş göndərilmir.
@Entity
@Table(name = "fcm_tokens")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    Long userId;

    @Column(nullable = false, unique = true, length = 500)
    String token;

    @Column(nullable = false)
    LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
