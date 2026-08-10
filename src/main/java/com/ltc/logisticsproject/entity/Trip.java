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

    // İstəyə bağlı — dartıcı (vehicle) ilə ayrıca seçilən qoşqu. Qoşqusuz
    // dartıcılar üçün (yalnız gövdə) və ya qoşqu tələb olunmayan reyslər
    // üçün null qala bilər (bax DispatcherService#createTrip,
    // TripRequest.trailerId).
    @ManyToOne
    @JoinColumn(name = "trailer_id")
    Trailer trailer;

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

    // Stage 8 — mobile driver app: photo proof of delivery uploaded from the
    // Android app when a trip is marked DELIVERED.
     String proofOfDeliveryUrl;

    @JsonIgnore
    @OneToMany(mappedBy = "trip")
     List<Cargo> cargos;


    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = TripStatus.PLANNED;
    }

}
