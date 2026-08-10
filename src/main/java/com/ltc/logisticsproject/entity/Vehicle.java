package com.ltc.logisticsproject.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "vehicles")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String plateNumber;

    private String brand;
    // NƏZƏRƏ AL: bu entity-də YÜK TUTUMU sahəsi YOXDUR — tır (dartıcı)
    // yalnız "baş hissə"dir, özü heç vaxt yük daşımır, kəlləyə qoşulan
    // qoşquya (bax Trailer.capacity) qoşulub yükü O daşıyır. Əvvəllər burada
    // `capacity` sahəsi var idi (layihənin ilk, Trailer konsepsiyası
    // yaranmazdan əvvəlki sadə modelindən qalma), amma məntiqsiz olduğu üçün
    // silindi — bax Trailer.java-dakı ətraflı izah və CargoQueue.jsx#
    // effectiveCapacityTons (yük çəkisi limiti YALNIZ qoşqudan gəlir).
    private Double fuelConsumption;
    private String vehicleDocumentUrl;

    // Sürücünün özü yüklədiyi maşın şəkilləri — 1 əsas (bax DriverController#
    // uploadVehicleMainPhoto), müştəri tracking səhifəsində götürülmə/təhvil
    // kartında dərhal görür. Ətraflı baxmaq istəsə isə bu siyahıdakı (əsas
    // şəkil + detallar, ən çoxu 4) bütün şəkilləri tam-ekran lightbox-da açır
    // (bax frontend: PhotoLightbox.jsx). @ElementCollection ayrıca
    // "vehicle_detail_photos" cədvəlinə yazır — mövcud "vehicles" cədvəlinə
    // yeni NOT NULL sütun əlavə etmir, ona görə Cargo.requiresCustoms-dakı
    // kimi bir miqrasiya problemi yaranmır.
    private String mainPhotoUrl;

    @ElementCollection
    @CollectionTable(name = "vehicle_detail_photos", joinColumns = @JoinColumn(name = "vehicle_id"))
    @Column(name = "photo_url")
    @OrderColumn(name = "photo_order")
    @Builder.Default
    private List<String> detailPhotoUrls = new ArrayList<>();

    // Bu tır hansı nəqliyyat növü ilə işləyir — çoxunun TRUCK olacağı fərz
    // edilir (prePersist-də default təyin olunur), amma multimodal filoya
    // (dəmiryol vaqonu, gəmi konteyner slotu s.) baxan dispetçerlər üçün
    // fərqli dəyər seçilə bilər.
    @Enumerated(EnumType.STRING)
    private TransportMode transportMode;

    @OneToOne
    @JoinColumn(name = "driver_id", unique = true)
    private Driver driver;

    // Şirkətə məxsusdursa (COMPANY) — dispetçer istənilən aktiv sürücüyə
    // reys-be-reys verə bilər, `driver` sahəsi boş qalır. Sürücünün öz
    // tırıdırsa (DRIVER_OWNED) — `driver` mütləq həmin sürücüyə bağlıdır və
    // dispetçerin tır seçimində YALNIZ o sürücü seçiləndə görünür (bax
    // DispatcherController#allVehicles driverId filtri). Köhnə sətirlərdə
    // (miqrasiyadan əvvəl) bu sütun boş ola bilər, ona görə prePersist-də
    // deyil, oxuyan tərəfdə (DTO-larda) null-u COMPANY kimi rəftar edirik.
    @Enumerated(EnumType.STRING)
    private OwnerType ownerType;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.transportMode == null) this.transportMode = TransportMode.TRUCK;
        if (this.ownerType == null) this.ownerType = OwnerType.COMPANY;
    }
}
