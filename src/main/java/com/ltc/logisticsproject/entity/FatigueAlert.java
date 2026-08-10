package com.ltc.logisticsproject.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

// Stage 5 (Driver PWA "Rest Mode"): a lightweight, real backend record so a
// driver's simulated 4.5h continuous-driving fatigue warning actually shows
// up on the Dispatcher Control Tower — not just a local toast the driver
// sees and no one else does. schema is auto-created (ddl-auto=update).
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "fatigue_alerts")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FatigueAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "trip_id", nullable = false)
    Trip trip;

    String driverName;
    String vehiclePlate;

    @Column(nullable = false)
    Double continuousDrivingHours;

    @Builder.Default
    Boolean resolved = false;

    LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.resolved == null) this.resolved = false;
    }
}
