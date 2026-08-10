package com.ltc.logisticsproject.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

// Stripe test rejimi ilə real ödəniş axını — hər Cargo üçün ən çox bir
// aktiv (PENDING/SUCCEEDED) ödəniş sətri saxlanılır. "plain Long id"
// konvensiyası (bax Notification.userId, Rating.driverId) bu layihədə
// artıq qərarlaşdırılmış olduğu üçün Cargo/Customer ilə @ManyToOne əvəzinə
// cargoId/customerId sahələri istifadə olunur.
@Entity
@Table(name = "payments")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    Long cargoId;

    // Dispetçerin özü yaratdığı "zəngli sifariş"lərdə (bax
    // DispatcherController#createCargo, Cargo.customer sahəsi null qalır)
    // heç bir real Customer hesabı yoxdur — ona görə burada artıq NOT NULL
    // deyil. Bu halda ödəniş yalnız customerId=null ilə saxlanılır (bax
    // PaymentService#recordOfflinePayment).
    Long customerId;

    @Column(nullable = false)
    Double amount;

    @Column(nullable = false)
    String currency;

    @Column(nullable = false, unique = true)
    String stripePaymentIntentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    PaymentStatus status;

    // "STRIPE" (defolt, mövcud onlayn kart axını) və ya "OFFLINE_DISPATCHER"
    // (dispetçerin nağd/bank köçürməsi kimi əl ilə qeydə aldığı ödəniş —
    // bax PaymentService#recordOfflinePayment). Köhnə sətirlərdə (bu sahə
    // əlavə olunmazdan əvvəl yaradılmış) null qala bilər, frontend bunu
    // "STRIPE" kimi göstərir.
    String method;

    // OFFLINE_DISPATCHER üçün dispetçerin qeyd etdiyi qısa qeyd (məs. "Nağd
    // qəbul edildi", "Bank köçürməsi, qəbz #123") — STRIPE ödənişlərdə null.
    String offlineNote;

    @Column(nullable = false)
    LocalDateTime createdAt;

    LocalDateTime paidAt;

    // DB-də saxlanılmır — yalnız PaymentIntent yaradılan anda Stripe-dan
    // gələn client secret-i controller-ə ötürmək üçün keçici sahə (frontend
    // Stripe Elements kart formasını bununla təsdiqləyir). Bir daha
    // sorğulananda (məs. səhifə yenilənəndə) bərpa olunmur — bu qəsdəndir,
    // çünki client secret yalnız bir dəfəlik istifadə üçün nəzərdə tutulub.
    @Transient
    String clientSecret;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = PaymentStatus.PENDING;
    }
}
