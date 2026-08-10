package com.ltc.logisticsproject.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Map;

// Reys öncəsi/sonrası yoxlama siyahısı (DVIR) — sürücü sabit bir yoxlama
// (əyləc, təkərlər, işıqlar və s.) siyahısını doldurur, hər maddə üçün
// OK/DEFECT/NA seçir. Defekt aşkar olunarsa dispetçer/admin dərhal xəbərdar
// olur (bax NotificationService#notifyDvirDefect). TripIncident/FatigueAlert
// ilə eyni naxış (trip back-reference, resolved bayrağı) — Control Tower-da
// oxşar banner göstərmək üçün. schema avtomatik yaradılır (ddl-auto=update).
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "dvir_inspections")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DvirInspection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "trip_id", nullable = false)
    Trip trip;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    DvirType type;

    String driverName;
    String vehiclePlate;

    // Açar = yoxlama maddəsinin kodu (BRAKES, TIRES, ...), dəyər = OK/DEFECT/NA
    // — bax frontend DVIR_ITEMS siyahısı (DriverCurrentTrip.jsx).
    @ElementCollection
    @CollectionTable(name = "dvir_inspection_items", joinColumns = @JoinColumn(name = "inspection_id"))
    @MapKeyColumn(name = "item_key")
    @Column(name = "item_status")
    Map<String, String> items;

    @Column(nullable = false)
    @Builder.Default
    Boolean hasDefects = false;

    String notes;

    // Yalnız hasDefects=true olduqda mənalıdır — dispetçer defekti aradan
    // qaldırılmış kimi qeyd edə bilsin deyə (bax
    // DispatcherController#resolveDvir).
    @Builder.Default
    Boolean resolved = false;

    LocalDateTime createdAt;
    LocalDateTime resolvedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.hasDefects == null) this.hasDefects = false;
        if (this.resolved == null) this.resolved = false;
    }
}
