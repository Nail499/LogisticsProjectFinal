package com.ltc.logisticsproject.entity;

import ch.qos.logback.core.status.Status;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

     LocalDateTime startedAt;
     LocalDateTime deliveredAt;
     LocalDateTime createdAt;


     Double estimatedDistanceKm;
     Double estimatedCost;

    @Column(columnDefinition = "TEXT")
     String routeInfo;

    @JsonIgnore
    @OneToMany(mappedBy = "trip")
     List<Cargo> cargos;


    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = TripStatus.PLANNED;
    }

}
