package com.ltc.logisticsproject.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

// Saytın yuxarısındakı zəng ikonu ("bell") üçün bildiriş qeydi. Rollardan
// asılı olmayaraq eyni cədvəl istifadə olunur — userId User.id-yə işarə edir
// (admin/dispetçer/sürücü/müştəri fərqi yoxdur), NotificationController isə
// hər zaman "cari istifadəçinin öz bildirişlərini" filtrləyir. Email
// göndərilib-göndərilməməsindən asılı olmayaraq HƏR bildiriş burada
// saxlanılır ki, zəng ikonundakı siyahı email-dən müstəqil işləsin (bax
// NotificationService — bəzi tip-lər üçün email də göndərilir, bəziləri
// yalnız in-app qalır, məs. IN_TRANSIT).
@Entity
@Table(name = "notifications")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    NotificationType type;

    @Column(nullable = false)
    String title;

    @Column(length = 1000)
    String message;

    // Kliklənəndə frontend-in hara yönləndirəcəyi (məs. "/customer/orders"
    // və ya tracking nömrəsi ilə "/tracking?number=..."). Boş ola bilər —
    // hər bildiriş naviqasiya tələb etmir.
    String link;

    @Column(nullable = false)
    Boolean read;

    @Column(nullable = false)
    LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.read == null) this.read = false;
    }
}
