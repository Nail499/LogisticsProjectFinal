package com.ltc.logisticsproject.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

// Qoşqu — dartıcıdan (Vehicle, "tır") tamamilə ayrı idarə olunan avadanlıq.
// Yükü əslində QOŞQU daşıyır — tır (dartıcı) yalnız "baş hissə"dir və özü
// yük götürmür (bax capacity sahəsi: bu, qoşqunun öz tutumudur, Vehicle.
// capacity-dən asılı deyil — reys yaradanda çəki yoxlaması qoşqu bağlanıbsa
// onun tutumuna görə aparılmalıdır, bax CargoQueue.jsx#effectiveCapacityTons).
// İstifadəçinin real filosunda bəzi dartıcılar qoşqusuzdur ("yalnız gövdə"),
// bəziləri gündən-günə fərqli qoşqu ilə işləyir — ona görə Trip-ə Vehicle-lə
// yanaşı İSTƏYƏ BAĞLI (nullable) ayrıca trailer seçimi əlavə olunur (bax
// Trip.trailer), Vehicle-in daxilində sabit əlaqə kimi deyil.
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "trailers")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Trailer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String plateNumber;

    private Double capacity;
    private String trailerDocumentUrl;

    // Vehicle.ownerType/driver ilə eyni məntiq: COMPANY — istənilən sürücüyə
    // reys-be-reys verilə bilər; DRIVER_OWNED — yalnız `driver`-ə bağlıdır.
    @Enumerated(EnumType.STRING)
    private OwnerType ownerType;

    @OneToOne
    @JoinColumn(name = "driver_id", unique = true)
    private Driver driver;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.ownerType == null) this.ownerType = OwnerType.COMPANY;
    }
}
