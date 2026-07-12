package com.ltc.logisticsproject.entity;

import ch.qos.logback.core.status.Status;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@Table(name = "trips")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "driver_id")
    Driver driver;

    @ManyToOne
    @JoinColumn(name = "vehicle_id")
    Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    TripStatus status;


    private Double estimatedDistanceKm;
    private Double estimatedCost;

    @OneToMany(mappedBy = "trip")
    List<Cargo> cargos;

    LocalDateTime createdAt;
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = TripStatus.PLANNED;
    }

}
