package com.ltc.logisticsproject.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

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
    private Double capacity;
    private Double fuelConsumption;
    private String vehicleDocumentUrl;

    @OneToOne
    @JoinColumn(name = "driver_id", unique = true)
    private Driver driver;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
