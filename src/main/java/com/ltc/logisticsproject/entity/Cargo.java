package com.ltc.logisticsproject.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cargos")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Cargo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false,unique = true)
    String trackingNumber;

    String description;
    Double weight;
    Double volume;

    @ManyToOne
    @JoinColumn(name = "origin_warehouse_id")
     WareHouse originWarehouse;

    @ManyToOne
    @JoinColumn(name = "customer_id")
     Customer customer;

    @Enumerated(EnumType.STRING)
     CargoType cargoType;

    @Enumerated(EnumType.STRING)
     UrgencyLevel urgency;

     java.time.LocalDate requestedPickupDate;

     String destinationAddress;
     Double destinationLatitude;
     Double destinationLongitude;

     String customerName;
     String customerPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
     CargoStatus status;

    @ManyToOne
    @JoinColumn(name = "trip_id")
     Trip trip;

     LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = CargoStatus.PENDING;
        if (this.trackingNumber == null) {
            this.trackingNumber = "TRK" + System.currentTimeMillis();
        }
    }
}
