package com.ltc.logisticsproject.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

// Sürücünün yolda bildirdiyi fövqəladə hal (qəza, sınma, yol bağlanması və
// s.) — bax DriverController#reportIncident. FatigueAlert ilə eyni sadə
// naxış: real backend qeydi ki, Dispatcher Control Tower-da dərhal
// görünsün, sadəcə sürücünün lokal ekranında qalmasın.
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "trip_incidents")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TripIncident {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "trip_id", nullable = false)
    Trip trip;

    String driverName;
    String vehiclePlate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    IncidentType type;

    @Column(columnDefinition = "TEXT")
    String description;

    // İstəyə bağlı — hadisə yerindən şəkil (bax FileStorageService, eyni
    // yükləmə naxışı proofOfDeliveryUrl/mainPhotoUrl kimi).
    String photoUrl;

    @Builder.Default
    Boolean resolved = false;

    LocalDateTime createdAt;
    LocalDateTime resolvedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.resolved == null) this.resolved = false;
    }
}
